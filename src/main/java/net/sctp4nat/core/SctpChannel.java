package net.sctp4nat.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.connection.SctpDefaultConfig;
import net.sctp4nat.exception.SctpInitException;
import net.sctp4nat.origin.JNIUtils;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpAcceptable;
import net.sctp4nat.origin.SctpNotification;
import net.sctp4nat.origin.SctpSocket;
import net.sctp4nat.origin.SctpSocket.NotificationListener;
import net.sctp4nat.util.SctpUtils;

/**
 * This class implements features from {@link SctpChannelFacade} and
 * {@link SctpSocket}. It is meant to wrap and shield the original classes
 * ({@link Sctp}, {@link SctpSocket}, {@link SctpNotification},
 * {@link JNIUtils}) provided by sctp4j from changing too much code.
 * 
 * <br>
 * <br>
 * This class provides the user with all five service primitives, which are
 * connect, disconnect, send, reply and close.
 * 
 * @author Jonas Wagner
 *
 */
public class SctpChannel implements SctpChannelFacade {

	private static final Logger LOG = LoggerFactory.getLogger(SctpChannel.class);
	private static final int NUMBER_OF_CONNECT_TASKS = 1;
	private static final long CONNECT_TIMEOUT = 5; // TODO jwa remove or explain this

	/**
	 * The corresponding native {@link SctpSocket}
	 */
	private final SctpSocket so;
	/**
	 * The corresponding {@link NetworkLink}.
	 */
	private NetworkLink link;
	/**
	 * {@link InetSocketAddress} of the remote counterpart.
	 */
	private InetSocketAddress remote;
	/**
	 * The {@link SctpMapper} used by the session
	 */
	private SctpMapper mapper;
	/**
	 * The {@link SctpNotification} callback, which is triggered by the native
	 * counterpart.
	 */
	private NotificationListener l;

	/**
	 * Creates an Instance of {@link SctpChannel}.
	 * 
	 * @param localSctpPort
	 *            the port, which is used by the native counterpart
	 * @param link
	 *            the corresponding {@link NetworkLink}.
	 * @param cb
	 *            the {@link SctpDataCallback}, which will be triggered by the
	 *            native counterpart.
	 * @param mapper
	 *            the {@link SctpMapper} used by the session
	 * @throws SctpInitException
	 */
	public SctpChannel(final int localSctpPort, final NetworkLink link, final SctpDataCallback cb,
			SctpMapper mapper) throws SctpInitException {
		this(localSctpPort, null, link, cb, mapper);
	}

	/**
	 * Creates an Instance of {@link SctpChannel}.
	 * 
	 * @param localSctpPort
	 *            the port, which is used by the native counterpart
	 * @param remote
	 *            {@link InetSocketAddress} of the remote.
	 * @param link
	 *            the corresponding {@link NetworkLink}.
	 * @param cb
	 *            the {@link SctpDataCallback}, which will be triggered by the
	 *            native counterpart.
	 * @param mapper
	 *            the {@link SctpMapper} used by the session
	 * @throws SctpInitException
	 */
	public SctpChannel(final int localSctpPort, InetSocketAddress remote, NetworkLink link, SctpDataCallback cb,
			SctpMapper mapper) throws SctpInitException {

		if (!Sctp.isInitialized()) {
			throw new SctpInitException("Sctp is currently not initialized! Try init with SctpUtils.init(...)");
		}

		this.so = Sctp.createSocket(localSctpPort);
		this.so.setLink(link); // forwards all onConnOut to the corresponding link
		this.so.setDataCallbackNative(cb);
		this.link = link;
		this.remote = remote;
		this.mapper = mapper;
	}

	public void setNotificationListener(SctpSocket.NotificationListener l) {
		this.l = l;
		so.setNotificationListener(l);
	}

	/**
	 * This method connects this {@link SctpChannel} to the remote
	 * counterpart. It uses {@link SctpSocket} to prepare the init messages and its
	 * {@link NetworkLink} to send it. Afterwards the SCTP four way handshake will
	 * be done. If this method is not answered within time it will return a
	 * {@link SctpInitException}.
	 * 
	 * @param remote
	 *            {@link InetSocketAddress} of the remote.
	 * @return p {@link Promise}
	 */
	public Promise<SctpChannelFacade, Exception, Object> connect(final InetSocketAddress remote) {
		final Deferred<SctpChannelFacade, Exception, Object> d = new DeferredObject<>();
		final CountDownLatch countDown = new CountDownLatch(NUMBER_OF_CONNECT_TASKS);

		class SctpConnectThread extends Thread {
			@Override
			public void run() {
				super.run();

				if (!Sctp.isInitialized()) {
					d.reject(new SctpInitException(
							"Sctp is currently not initialized! Try init it with SctpUtils.init(...)"));
					return;
				}

				try {
					addNotificationListener(d, countDown);
					SctpChannel.this.setNotificationListener(l);
					mapper.register(remote, SctpChannel.this);
					so.connectNative(remote.getPort());
				} catch (IOException e) {
					LOG.error("Could not connect via SCTP! Cause: " + e.getMessage(), e);
					mapper.unregister(remote);
					d.reject(e);
				}
			}

			/**
			 * This method catches the notifications send by the native counterpart
			 * 
			 * @param d
			 * @param countDown
			 */
			private void addNotificationListener(final Deferred<SctpChannelFacade, Exception, Object> d,
					final CountDownLatch countDown) {
				l = new NotificationListener() {

					@Override
					public void onSctpNotification(SctpAcceptable socket, SctpNotification notification) {
						LOG.debug(notification.toString());
						if (notification.toString().indexOf("COMM_UP") >= 0) {
							countDown.countDown();
							d.resolve(SctpChannel.this);
						} else if (notification.toString().indexOf("SHUTDOWN_COMP") >= 0) {
							// TODO jwa make a clean shutdown possible closing the socket prevents any
							// SHUTDOWN ACK to be sent...
							LOG.debug("Shutdown request received. Now shutting down the SCTP connection...");
							SctpChannel.this.close();
							d.reject(new Exception(
									"we are forced to shutdown because of shutdown request from server!"));
						} else if (notification.toString().indexOf("ADDR_UNREACHABLE") >= 0) {
							LOG.error("Heartbeat missing! Now shutting down the SCTP connection...");
							SctpChannel.this.close();
							d.reject(new Exception(
									"we are forced to close the connection because the remote is not answering! (remote: "
											+ remote.getAddress().getHostAddress() + ":" + remote.getPort() + ")"));
						} else if (notification.toString().indexOf("COMM_LOST") >= 0) {
							LOG.error("Communication aborted! Now shutting down the udp connection...");
							SctpChannel.this.close();
							d.reject(new Exception(
									"we are forced to close the connection because we lost the connection to remote: "
											+ remote.getAddress().getHostAddress() + ":" + remote.getPort()));
						}
					}

				};
			}
		}

		SctpUtils.getThreadPoolExecutor()
				.execute(new SctpTimeoutThread(d, CONNECT_TIMEOUT, TimeUnit.SECONDS, countDown));
		SctpUtils.getThreadPoolExecutor().execute(new SctpConnectThread());

		return d.promise();
	}

	@Override
	public Promise<Integer, Exception, Object> send(byte[] data, boolean ordered, int sid, int ppid) {
		Deferred<Integer, Exception, Object> d = new DeferredObject<>();

		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

			@Override
			public void run() {
				if (!Sctp.isInitialized()) {
					d.reject(new SctpInitException(
							"Sctp is currently not initialized! Try init it with SctpUtils.init(...)"));
				}

				try {
					d.resolve(new Integer(so.sendNative(data, 0, data.length, ordered, sid, ppid)));
				} catch (IOException e) {
					LOG.error("Could not send! Cause: " + e.getMessage(), e);
					d.reject(e);
				}
			}
		});

		return d.promise();
	}

	@Override
	public Promise<Integer, Exception, Object> send(byte[] data, int offset, int len, boolean ordered, int sid,
			int ppid) {
		Deferred<Integer, Exception, Object> d = new DeferredObject<>();

		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

			@Override
			public void run() {
				if (!Sctp.isInitialized()) {
					d.reject(new SctpInitException(
							"Sctp is currently not initialized! Try init it with SctpUtils.init(...)"));
				}

				try {
					d.resolve(new Integer(so.sendNative(data, offset, len, ordered, sid, ppid)));
				} catch (IOException e) {
					LOG.error("Could not send! Cause: " + e.getMessage(), e);
					d.reject(e);
				}
			}
		});

		return d.promise();
	}
	
	@Override
	public Promise<Integer, Exception, Object> send(byte[] data, int offset, int len, SctpDefaultConfig config) {
		return send(data, offset, len, config.isOrdered(), config.getSid(), config.getPpid());
	}

	@Override
	public Promise<Integer, Exception, Object> send(byte[] data, SctpDefaultConfig config) {
		return send(data, config.isOrdered(), config.getSid(), config.getPpid());
	}

	/**
	 * FIXME jwa this call is non-blocking, therefore it should be calling a
	 * callback or something similar
	 */
	@Override
	public void shutdownInit() {
		try {
			LOG.debug("Send shutdown command to " + remote.getHostString() + ":" + remote.getPort());
			int success = so.shutdownNative(SctpUtils.SHUT_WR); // TODO jwa implement config
			if (success < 0) {
				LOG.error("Could not send SHUTDOWN command to " + remote.getHostString() + ":" + remote.getPort());
			}
		} catch (Exception e) {
			LOG.error("Could not send SHUTDOWN command to " + remote.getHostString() + ":" + remote.getPort());
		}

	}

	@Override
	public Promise<Object, Exception, Object> close() {
		Deferred<Object, Exception, Object> d = new DeferredObject<>();

		final SctpChannel currentInstance = this;
		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

			@Override
			public void run() {
				LOG.debug("Closing connection to " + remote.getHostString() + ":" + remote.getPort());
				mapper.unregister(currentInstance);
				SctpPorts.getInstance().removePort(currentInstance);
				so.closeNative();
				link.close();
				d.resolve(null);
			}
		});

		return d.promise();

	}

	/**
	 * This method makes the SCTP socket listen to incoming connections.
	 */
	public void listen() {
		try {
			this.so.listenNative();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public boolean containsSctpSocket(final SctpSocket so) {
		return this.so.equals(so);
	}

	/**
	 * Forwards the incoming SCTP message to the native counterpart
	 */
	public void onConnIn(byte[] data, int offset, int length) {
		try {
			this.so.onConnIn(data, offset, length);
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
	}

	/**
	 * triggers the state change of the incoming connection to "COMM_UP"
	 * @return
	 */
	public boolean accept() {
		try {
			return so.acceptNative();
		} catch (IOException e) {
			LOG.error(e.getMessage());
			return false; // this signals a accept failure
		}
	}

	@Override
	public void setSctpDataCallback(final SctpDataCallback cb) {
		so.setDataCallbackNative(cb);
	}

	public void setLink(NetworkLink link) {
		so.setLink(link);
		this.link = link;
	}

	@Override
	public InetSocketAddress getRemote() {
		return this.remote;
	}

}
