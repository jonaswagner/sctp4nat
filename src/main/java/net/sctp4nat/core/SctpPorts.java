/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sctp4nat.core;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.origin.SctpSocket;

/**
 * This class cares about the SCTP port assignement. It provides methods to
 * check if usrsctp already has assigned a certain port or not.
 * 
 * @author Jonas Wagner
 *
 */
public class SctpPorts {

	private static final Logger LOG = LoggerFactory.getLogger(SctpPorts.class);

	/**
	 * This class is a singleton
	 */
	private static final SctpPorts instance = new SctpPorts(); // threadsafe

	/**
	 * port = 2⁰ - 1 (because 0 is also a port)
	 */
	public static final int MIN_PORT = 0;
	/**
	 * port = 2¹⁶ - 1 (because 0 is also a port)
	 */
	public static final int MAX_PORT = 65535;
	/**
	 * The minimal dyniamic port.
	 */
	public static final int MIN_DYN_PORT = 49152;
	/**
	 * -1 indicates, that a port variable is not initialized yet.
	 */
	public static final int PORT_NOT_INITIALIZED = -1;

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
	 * Offical SCTP tunneling port assigned by IANA. see <a href=
	 * "https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml?search=9899">here</a>
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

	/**
	 * This method retrieves a unused SCTP port out of the dynamic port range
	 * (49152-65535).
	 * 
	 * @return a random unused port from the dynamic range.
	 */
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
	 *            the corresponding {@link SctpChannel}
	 * @param port
	 *            the port, which is used for so
	 */
	public synchronized void putPort(final SctpChannel so, final int port) {
		if (so == null) {
			LOG.error("SctpPorts cannot remove a port assigned to null (so was null)!");
			return;
		}
		portMap.put(so, port);
		LOG.info("SctpPorts added port " + port + " to the portMap!");
	}

	/**
	 * removes a port from the portMap.
	 * 
	 * @param so
	 *            The corresponding {@link SctpChannel}
	 */
	public synchronized void removePort(final SctpChannel so) {
		if (so == null) {
			LOG.error("SctpPorts cannot remove a port assigned to null (so was null)!");
			return;
		}

		if (!portMap.containsKey(so)) {
			LOG.warn("SctpPorts cannot remove " + so.toString()
					+ ", because it does not contain such an instance in its portMap!");
			return;
		}

		int port = portMap.remove(so);
		LOG.info("SctpPorts removed port " + port + " from the portMap!");
	}

	/**
	 * @param port
	 * @return returns true if the port is not used yet by any other
	 *         {@link SctpSocket}.
	 */
	public synchronized boolean isUsedPort(final int port) {
		return portMap.contains(port);
	}
	
	public boolean isInValidRange(final int port) {
		return port >= SctpPorts.MIN_PORT && port <= SctpPorts.MAX_PORT;
	}

	/**
	 * Clears all entries from the portMap. Only call this after usrsctp.finish()
	 * has been executed!
	 */
	public static void shutdown() {
		portMap.clear();
		LOG.debug("portMap cleared");
	}
}
