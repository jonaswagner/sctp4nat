package net.sctp4nat.core;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.origin.SctpSocket;

public class SctpPorts {
	
	private static final Logger LOG = LoggerFactory.getLogger(SctpPorts.class);

	public static final int MAX_PORT = 65535;
	public static final int MIN_DYN_PORT = 49152;

	
	/**
	 * This class is a singleton
	 */
	private static final SctpPorts instance = new SctpPorts(); // threadsafe

	/**
	 * This {@link ConcurrentHashMap} keeps track of all the used ports for sctp.
	 * Since the implementation is based on native C code, we need to make sure that
	 * a port isn't used twice.
	 */
	private static final ConcurrentHashMap<SctpChannel, Integer> portMap = new ConcurrentHashMap<>();

	/**
	 * This is a simple {@link Random} object. {@link Date}.getTime() should provide
	 * more randomness.
	 */
	private static final Random RND = new Random(new Date().getTime());

	/**
	 * Offical SCTP tunneling port assigned by IANA. see
	 * <a href="https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml?search=9899">here</a>
	 * for details.
	 */
	public static final int SCTP_TUNNELING_PORT = 9899;

	private SctpPorts() {
	}

	/**
	 * @return the instance of this class
	 */
	public static SctpPorts getInstance() {
		return instance;
	}

	public synchronized int generateDynPort() {
		int attempt = RND.nextInt(MAX_PORT - MIN_DYN_PORT) + MIN_DYN_PORT;

		while (isUsedPort(attempt)) {
			attempt = RND.nextInt(MAX_PORT - MIN_DYN_PORT) + MIN_DYN_PORT;
		}
		return attempt;
	}

	/**
	 * Adds a port to the portMap.
	 * 
	 * @param so
	 * 		the corresponding {@link SctpChannel}
	 * @param port
	 * 		the port, which is used for so
	 */
	public synchronized void putPort(final SctpChannel so, final int port) {
		portMap.put(so, port);
		LOG.info("SctpPorts added port " + port + " to the portMap!");
	}

	/**
	 * removes a port from the portMap.
	 * 
	 * @param so
	 * 		The corresponding {@link SctpChannel}
	 */
	public synchronized void removePort(final SctpChannel so) {
		if (so == null) {
			LOG.error("SctpPorts cannot remove a port assigned to null (so was null)!");
			return;
		}
		
		if (!portMap.contains(so)) {
			LOG.warn("SctpPorts cannot remove " + so.toString() + ", because it does not contain such an instance in its portMap!");
			return;
		}
		
		
		int port = portMap.remove(so);
		LOG.info("SctpPorts removed port " + port + " from the portMap!");
	}

	/**
	 * @param port
	 * @return returns true if the port is not used yet by any other {@link SctpSocket}.
	 */
	public synchronized boolean isUsedPort(final int port) {
		return portMap.contains(port);
	}

	/**
	 * Clears all entries from the portMap
	 */
	public static void shutdown() {
		portMap.clear();
		LOG.debug("portMap cleared");
	}
}
