package net.sctp4nat.core;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final ConcurrentHashMap<SctpSocketAdapter, Integer> portMap = new ConcurrentHashMap<>();

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

		while (isFreePort(attempt)) {
			attempt = RND.nextInt(MAX_PORT - MIN_DYN_PORT) + MIN_DYN_PORT;
		}
		return attempt;
	}

	public synchronized void putPort(final SctpSocketAdapter so, final int port) {
		portMap.put(so, port);
	}

	public synchronized void removePort(final SctpSocketAdapter so) {
		portMap.remove(so);
	}

	public synchronized boolean isFreePort(final int port) {
		return portMap.contains(port);
	}

	public static void shutdown() {
		portMap.clear();
		LOG.debug("portMap cleared");
	}
}
