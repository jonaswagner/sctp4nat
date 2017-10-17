package net.tomp2p.sctp.core;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SctpMapper {

	private static final Logger LOG = LoggerFactory.getLogger(SctpMapper.class);

	private static final ConcurrentHashMap<InetSocketAddress, SctpAdapter> socketMap = new ConcurrentHashMap<>();

	public synchronized void register(final InetSocketAddress remote, final SctpAdapter so) {
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
	public synchronized void unregister(SctpAdapter so) {
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
	 * usrsctp counterpart. Make sure this {@link SctpAdapter} instance is not
	 * used anywhere else!
	 */
	public synchronized void unregister(InetSocketAddress remote) {
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

	public synchronized static SctpAdapter locate(final String remoteAddress, final int remotePort) {
		for (Map.Entry<InetSocketAddress, SctpAdapter> element : socketMap.entrySet()) {
			int port = element.getKey().getPort();
			String address = element.getKey().getAddress().getHostAddress();
			
			if (port == remotePort && address.equals(remoteAddress)) {
				return socketMap.get(element.getKey());
			}
		}
		
		LOG.info("No socketMap entry found for IP:" + remoteAddress + " and port: " + remotePort);
		return null;
	}

	public synchronized static SctpAdapter locate(final SctpSocket sctpSocket) {
		if (socketMap.isEmpty()) {
			return null;
		}
		
		SctpAdapter facade = socketMap.values().stream().filter(so -> so.containsSctpSocket(sctpSocket)).findFirst()
				.get();

		if (facade == null) {
			LOG.error("Could not retrieve SctpSocket from SctpMapper!");
			return null;
		} else {
			return facade;
		}
	}

}
