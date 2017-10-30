package net.sctp4j.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.Notification;

import net.sctp4j.origin.SctpNotification;
import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import net.sctp4j.connection.SctpTimeoutThread;
import net.sctp4j.connection.SctpUtils;
import net.sctp4j.origin.Sctp;
import net.sctp4j.origin.SctpSocket;
import net.sctp4j.origin.SctpSocket.NotificationListener;

public class SctpSocketAdapter implements SctpAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(SctpSocketAdapter.class);
	private static final int NUMBER_OF_CONNECT_TASKS = 1;
	private static final long CONNECT_TIMEOUT = 5;

	private final SctpSocket so;
	@Getter
	private final InetSocketAddress local;
	@Getter
	private final SctpDataCallback cb;
	@Getter
	private NetworkLink link;
	@Setter
	private InetSocketAddress remote;
	@Getter
	private SctpMapper mapper;
	@Getter
	private NotificationListener l;

	public SctpSocketAdapter(final InetSocketAddress local, int localSctpPort, final NetworkLink link,
			final SctpDataCallback cb, SctpMapper mapper) throws SctpInitException {
		this(local, localSctpPort, null, link, cb, mapper);
	}

	public SctpSocketAdapter(InetSocketAddress local, int localSctpPort, InetSocketAddress remote, NetworkLink link,
			SctpDataCallback cb, SctpMapper mapper) throws SctpInitException {

		if (!Sctp.isInitialized()) {
			throw new SctpInitException("Sctp is currently not initialized! Try init it with SctpUtils.init(...)");
		}

		this.so = Sctp.createSocket(localSctpPort);
		this.so.setLink(link); // forwards all onConnOut to the corresponding link
		this.link = link;
		this.local = local;
		this.remote = remote;
		this.so.setDataCallbackNative(cb);
		this.cb = cb;
		this.mapper = mapper;
	}

	public void setNotificationListener(SctpSocket.NotificationListener l) {
		this.l = l;
		so.setNotificationListener(l);
	}

	@Override
	public Promise<SctpAdapter, Exception, Object> connect(final InetSocketAddress remote) {
		final Deferred<SctpAdapter, Exception, Object> d = new DeferredObject<>();
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
					l = new NotificationListener() {

						@Override
						public void onSctpNotification(SctpSocket socket, SctpNotification notification) {
							if (notification.toString().indexOf("COMM_UP") >= 0) {
								countDown.countDown();
								d.resolve(SctpSocketAdapter.this);
							} else if (notification.toString().indexOf("SHUTDOWN_COMP") >= 0) {
								LOG.debug("Shutdown request received. Now shutting down the SCTP connection...");
								try {
									SctpSocketAdapter.this.close().wait(SctpUtils.SHUTDOWN_TIMEOUT);
								} catch (InterruptedException e) {
									LOG.error(e.getMessage(), e);
								}
							} else {
								LOG.debug(notification.toString());
							}
						}
					};
					SctpSocketAdapter.this.setNotificationListener(l);
					so.connectNative(remote.getPort());
					mapper.register(remote, SctpSocketAdapter.this);
				} catch (IOException e) {
					LOG.error("Could not connect via SCTP! Cause: " + e.getMessage(), e);
					mapper.unregister(remote);
					d.reject(e);
				}
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

	@Override
	public void listen() {
		try {
			this.so.listenNative();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean containsSctpSocket(SctpSocket so) {
		return this.so.equals(so);
	}

	/**
	 * This method is an indirection for SctpSocket, which needs to be unaccessible
	 * for a third party user.
	 */
	@Override
	public void onConnIn(byte[] data, int offset, int length) {
		try {
			this.so.onConnIn(data, offset, length);
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
	}

	@Override
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

	@Override
	public void setLink(NetworkLink link) {
		so.setLink(link);
		this.link = link;
	}

	@Override
	public InetSocketAddress getRemote() {
		return this.remote;
	}
}
