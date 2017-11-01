package net.sctp4j.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdeferred.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4j.origin.SctpSocket;

public class SctpMapper {

	private static final Logger LOG = LoggerFactory.getLogger(SctpMapper.class);

	private static final ConcurrentHashMap<InetSocketAddress, SctpSocketAdapter> socketMap = new ConcurrentHashMap<>();

	private static boolean isShutdown = false;

	public synchronized void register(final InetSocketAddress remote, final SctpSocketAdapter so) {
		if (isShutdown) {
			return;
		}

		if (remote != null && so != null) {
			socketMap.put(remote, so);
		} else {
			LOG.error("Invalid input, socket could not be registered! Either remote or so was null!");
		}
	}

	/**
	 * This method removes a socket from the {@link Mapper} and shuts down the
	 * usrsctp counterpart. Make sure this {@link SctpSocketAdapter} instance is not
	 * used anywhere else!
	 */
	public synchronized void unregister(SctpSocketAdapter so) {
		if (isShutdown) {
			return;
		}

		if (so == null) {
			LOG.error("Invalid input, null can't be removed!");
			return;
		} else if (!socketMap.contains(so)) {
			LOG.error("Invalid input, a socket, which is not registered, cannot be removed!");
			return;
		} else {
			socketMap.remove(so);
		}
	}

	/**
	 * This method removes a socket from the {@link Mapper} and shuts down the
	 * usrsctp counterpart. Make sure this {@link SctpSocketAdapter} instance is not used
	 * anywhere else!
	 */
	public synchronized void unregister(InetSocketAddress remote) {
		if (isShutdown) {
			return;
		}

		if (remote == null) {
			LOG.error("Invalid input, null can't be removed!");
			return;
		} else if (!socketMap.containsKey(remote)) {
			LOG.error("Invalid input, a socket, which is not registered, cannot be removed!");
			return;
		} else {
			socketMap.remove(remote);
		}
	}

	/**
	 * This method locates a {@link SctpSocketAdapter} object given the remote {@link InetAddress} and port.
	 * @param remoteAddress
	 * @param remotePort
	 * @return {@link SctpSocketAdapter}
	 */
	public synchronized static SctpSocketAdapter locate(final String remoteAddress, final int remotePort) {
		if (isShutdown) {
			return null;
		}

		for (Map.Entry<InetSocketAddress, SctpSocketAdapter> element : socketMap.entrySet()) {
			int port = element.getKey().getPort();
			String address = element.getKey().getAddress().getHostAddress();

			if (port == remotePort && address.equals(remoteAddress)) {
				return socketMap.get(element.getKey());
			}
		}

		LOG.info("No socketMap entry found for IP:" + remoteAddress + " and port: " + remotePort);
		return null;
	}

	/**
	 * This method locates a {@link SctpSocketAdapter} object given a {@link SctpSocket}.
	 * 
	 * @param remoteAddress
	 * @param remotePort
	 * @return {@link SctpSocketAdapter}
	 */
	public synchronized static SctpSocketAdapter locate(final SctpSocket sctpSocket) {
		if (isShutdown) {
			return null;
		}

		if (socketMap.isEmpty()) {
			return null;
		}

		SctpSocketAdapter facade = socketMap.values().stream().filter(so -> so.containsSctpSocket(sctpSocket)).findFirst()
				.get();

		if (facade == null) {
			LOG.error("Could not retrieve SctpSocket from SctpMapper!");
			return null;
		} else {
			return facade;
		}
	}

	/**
	 * This method shuts down all remaining connections and closes them.
	 */
	public void shutdown() {
		isShutdown = true;

		for (Map.Entry<InetSocketAddress, SctpSocketAdapter> element : socketMap.entrySet()) {
			SctpSocketAdapter so = element.getValue();
			so.shutdownInit();		
			Promise<Object, Exception, Object> p = so.close();
			try {
				p.waitSafely(2000l);
			} catch (InterruptedException e) {
				LOG.error(
						"Could not shutdown connection to " + so.getRemote().getAddress().getHostAddress()
								+ ":" + so.getRemote().getPort(), e);
			}
		}
		LOG.debug("all sctp connections closed");

		socketMap.clear();
		LOG.debug("socketMap cleared");

	}
}
