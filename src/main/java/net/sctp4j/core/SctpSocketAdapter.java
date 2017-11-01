package net.sctp4j.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import net.sctp4j.connection.SctpTimeoutThread;
import net.sctp4j.connection.SctpUtils;
import net.sctp4j.origin.Sctp;
import net.sctp4j.origin.SctpNotification;
import net.sctp4j.origin.SctpSocket;
import net.sctp4j.origin.SctpSocket.NotificationListener;

public class SctpSocketAdapter implements SctpChannelFacade {

	private static final Logger LOG = LoggerFactory.getLogger(SctpSocketAdapter.class);
	private static final int NUMBER_OF_CONNECT_TASKS = 1;
	private static final long CONNECT_TIMEOUT = 5;

	/**
	 * The corresponding native {@link SctpSocket}
	 */
	private final SctpSocket so;
	private final SctpDataCallback cb;
	private NetworkLink link;
	private InetSocketAddress remote;
	private SctpMapper mapper;
	private NotificationListener l;

	public SctpSocketAdapter(final int localSctpPort, final NetworkLink link,
			final SctpDataCallback cb, SctpMapper mapper) throws SctpInitException {
		this(localSctpPort, null, link, cb, mapper);
	}

	public SctpSocketAdapter(final int localSctpPort, InetSocketAddress remote, NetworkLink link,
			SctpDataCallback cb, SctpMapper mapper) throws SctpInitException {

		if (!Sctp.isInitialized()) {
			throw new SctpInitException("Sctp is currently not initialized! Try init with SctpUtils.init(...)");
		}

		this.so = Sctp.createSocket(localSctpPort);
		this.so.setLink(link); // forwards all onConnOut to the corresponding link
		this.link = link;
		this.remote = remote;
		this.so.setDataCallbackNative(cb);
		this.cb = cb;
		this.mapper = mapper;
	}

	public void setNotificationListener(SctpSocket.NotificationListener l) {
		this.l = l;
		so.setNotificationListener(l);
	}

	public Promise<SctpSocketAdapter, Exception, Object> connect(final InetSocketAddress remote) {
		final Deferred<SctpSocketAdapter, Exception, Object> d = new DeferredObject<>();
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
					SctpSocketAdapter.this.setNotificationListener(l);
					so.connectNative(remote.getPort());
					mapper.register(remote, SctpSocketAdapter.this);
				} catch (IOException e) {
					LOG.error("Could not connect via SCTP! Cause: " + e.getMessage(), e);
					mapper.unregister(remote);
					d.reject(e);
				}
			}

			private void addNotificationListener(final Deferred<SctpSocketAdapter, Exception, Object> d,
					final CountDownLatch countDown) {
				l = new NotificationListener() {

					@Override
					public void onSctpNotification(SctpSocket socket, SctpNotification notification) {
						LOG.debug(notification.toString());
						if (notification.toString().indexOf("COMM_UP") >= 0) {
							countDown.countDown();
							d.resolve(SctpSocketAdapter.this);
						} else if (notification.toString().indexOf("SHUTDOWN_COMP") >= 0) {
							// TODO jwa make a clean shutdown possible closing the socket prevents any
							// SHUTDOWN ACK to be sent...
							LOG.debug("Shutdown request received. Now shutting down the SCTP connection...");
							SctpSocketAdapter.this.close();
							d.reject(new Exception(
									"we are forced to shutdown because of shutdown request from server!"));
						} else if (notification.toString().indexOf("ADDR_UNREACHABLE") >= 0) {
							LOG.error("Heartbeat missing! Now shutting down the SCTP connection...");
							SctpSocketAdapter.this.close();
							d.reject(new Exception(
									"we are forced to close the connection because the remote is not answering! (remote: "
											+ remote.getAddress().getHostAddress() + ":" + remote.getPort() + ")"));
						} else if (notification.toString().indexOf("COMM_LOST") >= 0) {
							LOG.error("Communication aborted! Now shutting down the udp connection...");
							SctpSocketAdapter.this.close();
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

		final SctpSocketAdapter currentInstance = this;
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

	public void listen() {
		try {
			this.so.listenNative();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public boolean containsSctpSocket(SctpSocket so) {
		return this.so.equals(so);
	}

	/**
	 * This method is an indirection for SctpSocket, which needs to be unaccessible
	 * for a third party user.
	 */
	public void onConnIn(byte[] data, int offset, int length) {
		try {
			this.so.onConnIn(data, offset, length);
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
	}

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
