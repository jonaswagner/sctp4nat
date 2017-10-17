package net.tomp2p.sctp.core;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import net.tomp2p.connection.Ports;

public class SctpPorts {

	/**
	 * This class is a singleton
	 */
	private static final SctpPorts instance = new SctpPorts(); // threadsafe

	/**
	 * This {@link ConcurrentHashMap} keeps track of all the used ports for sctp.
	 * Since the implementation is based on native C code, we need to make sure that
	 * a port isn't used twice.
	 */
	private static final ConcurrentHashMap<SctpAdapter, Integer> portMap = new ConcurrentHashMap<>();

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
		int attempt = RND.nextInt(Ports.MAX_PORT - Ports.MIN_DYN_PORT) + Ports.MIN_DYN_PORT;

		while (isFreePort(attempt)) {
			attempt = RND.nextInt(Ports.MAX_PORT - Ports.MIN_DYN_PORT) + Ports.MIN_DYN_PORT;
		}
		return attempt;
	}

	public synchronized void putPort(final SctpAdapter so, final int port) {
		portMap.put(so, port);
	}

	public synchronized void removePort(final SctpAdapter so) {
		portMap.remove(so);
	}

	public synchronized boolean isFreePort(final int port) {
		return portMap.contains(port);
	}
}
