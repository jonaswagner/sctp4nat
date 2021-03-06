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

import net.sctp4nat.connection.NetworkLink;
import net.sctp4nat.connection.SctpDefaultStreamConfig;
import net.sctp4nat.origin.JNIUtils;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpAcceptable;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.origin.SctpNotification;
import net.sctp4nat.origin.SctpSocket;
import net.sctp4nat.origin.SctpSocket.NotificationListener;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;

/**
 * This class implements features from {@link SctpChannelFacade} and
 * {@link SctpSocket}. It is meant to wrap and shield the original classes
 * ({@link Sctp}, {@link SctpSocket}, {@link SctpNotification},
 * {@link JNIUtils}) provided by sctp4j from changing too much code.
 * 
 * <br>
 * <br>
 * This class provides the user with all six service primitives, which are
 * listen, connect, disconnect, send, reply and close.
 * 
 * @author Jonas Wagner
 *
 */
public class SctpChannel implements SctpChannelFacade {

	private static final Logger LOG = LoggerFactory.getLogger(SctpChannel.class);
	private static final int NUMBER_OF_CONNECT_TASKS = 1;
	private static final long CONNECT_TIMEOUT_SECONDS = 5;

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
	public SctpChannel(final int localSctpPort, final NetworkLink link, final SctpDataCallback cb, SctpMapper mapper)
			throws SctpInitException {
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
		so.setNotificationListener(l);
	}

	/**
	 * This method connects this {@link SctpChannel} to the remote counterpart. It
	 * uses {@link SctpSocket} to prepare the init messages and its
	 * {@link NetworkLink} to send it. Afterwards the SCTP four way handshake will
	 * be done. If this method is not answered within time it will return a
	 * {@link SctpInitException}.
	 * 
	 * @param remote
	 *            {@link InetSocketAddress} of the remote.
	 * @return p {@link Promise}
	 */
	public Promise<SctpChannelFacade, Exception, Void> connect(final InetSocketAddress remote) {
		final Deferred<SctpChannelFacade, Exception, Void> d = new DeferredObject<>();
		final CountDownLatch countDown = new CountDownLatch(NUMBER_OF_CONNECT_TASKS);

		if (!Sctp.isInitialized()) {
			d.reject(new SctpInitException("Sctp is currently not initialized! Try init it with SctpUtils.init(...)"));
			return d;
		}

		try {
			NotificationListener l = addNotificationListener(d, countDown);
			SctpChannel.this.setNotificationListener(l);
			mapper.register(remote, SctpChannel.this);
			LOG.debug("try connect to {}/{}, ", remote.getAddress().getHostAddress(), remote.getPort());
			so.connectNative(remote.getPort());
		} catch (IOException e) {
			LOG.error("Could not connect via SCTP! Cause: " + e.getMessage(), e);
			mapper.unregister(remote);
			d.reject(e);
		}

		SctpUtils.getThreadPoolExecutor()
				.execute(new SctpTimeoutThread(d, CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS, countDown));
		return d.promise();
	}

	/**
	 * This method catches the notifications send by the native counterpart
	 * 
	 * @param d
	 *            {@link Deferred}
	 * @param countDown
	 *            The {@link CountDownLatch} from connect.
	 */
	private NotificationListener addNotificationListener(final Deferred<SctpChannelFacade, Exception, Void> d,
			final CountDownLatch countDown) {
		return new NotificationListener() {

			@Override
			public void onSctpNotification(SctpAcceptable socket, SctpNotification notification) {
				LOG.debug(notification.toString());
				if (notification.toString().indexOf(SctpNotification.COMM_UP_STR) >= 0) {
					countDown.countDown();
					d.resolve(SctpChannel.this);
				} else if (notification.toString().indexOf(SctpNotification.SHUTDOWN_COMP_STR) >= 0) {
					// TODO jwa make a clean shutdown possible closing the socket prevents any
					// SHUTDOWN ACK to be sent...
					LOG.debug("Shutdown request received. Now shutting down the SCTP connection...");
					SctpChannel.this.close();
					d.reject(new Exception("we are forced to shutdown because of shutdown request from server!"));
				} else if (notification.toString().indexOf(SctpNotification.ADDR_UNREACHABLE_STR) >= 0) {
					LOG.error("Heartbeat missing! Now shutting down the SCTP connection...");
					SctpChannel.this.close();
					d.reject(new Exception(
							"we are forced to close the connection because the remote is not answering! (remote: "
									+ remote.getAddress().getHostAddress() + ":" + remote.getPort() + ")"));
				} else if (notification.toString().indexOf(SctpNotification.COMM_LOST_STR) >= 0) {
					LOG.error("Communication aborted! Now shutting down the udp connection...");
					SctpChannel.this.close();
					d.reject(new Exception(
							"we are forced to close the connection because we lost the connection to remote: "
									+ remote.getAddress().getHostAddress() + ":" + remote.getPort()));
				} else if (notification.toString().indexOf(SctpNotification.SHUTDOWN_EVENT_STR) > 0) {
					SctpChannel.this.close();
				}
			}

		};
	}

	@Override
	public Promise<Integer, Exception, Object> send(byte[] data, boolean ordered, int sid, int ppid) {
		Deferred<Integer, Exception, Object> d = new DeferredObject<>();

		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

			@Override
			public void run() {
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
	public Promise<Integer, Exception, Object> send(byte[] data, int offset, int len, SctpDefaultStreamConfig config) {
		return send(data, offset, len, config.isOrdered(), config.getSid(), config.getPpid());
	}

	@Override
	public Promise<Integer, Exception, Object> send(byte[] data, SctpDefaultStreamConfig config) {
		return send(data, config.isOrdered(), config.getSid(), config.getPpid());
	}

	/*
	 * FIXME jwa this call is non-blocking, therefore it should be calling a
	 * callback or something similar
	 */
	@Override
	public void shutdownInit() {
		try {
			LOG.debug("Send shutdown command to " + remote.getHostString() + ":" + remote.getPort());
			int success = so.shutdownNative(SctpUtils.SHUT_WR);
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
				if (link != null) {
					link.close();
				}
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

	/**
	 * This method checks if this instance contains the suggested
	 * {@link SctpSocket}.
	 * 
	 * @param so
	 *            suggested {@link SctpSocket}
	 * @return true if the suggested {@link SctpSocket} is equal to this instance
	 *         {@link SctpSocket}.
	 */
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

	@Override
	public void setSctpDataCallback(final SctpDataCallback cb) {
		so.setDataCallbackNative(cb);
	}

	/**
	 * The method setLink() defines the NetworkLink, which is used to encapsulate
	 * the SCTP association with a UDP header. Additionally, via this NetworkLink,
	 * also the incoming SCTP packets are decoded.
	 * 
	 * @param link
	 *            A {@link NetworkLink}
	 */
	public void setLink(NetworkLink link) {
		so.setLink(link);
		this.link = link;
	}

	@Override
	public InetSocketAddress getRemote() {
		return this.remote;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SctpChannel: Remote(" + remote.getAddress().getHostAddress() + ":" + remote.getPort()
				+ "), SctpSocket(SctpPort:" + so.getPort() + ")");
		return builder.toString();

	}

}
