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
package net.sctp4nat.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import net.sctp4nat.connection.SctpDefaultStreamConfig;
import net.sctp4nat.connection.UdpServerLink;
import net.sctp4nat.core.NetworkLink;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.origin.SctpSocket;

/**
 * This class holds several important constants for the whole sctp4nat project.
 * Also, it cares about the correct initialization of usrsctp via the init()
 * method.
 * 
 * @author Jonas Wagner
 *
 */
public class SctpUtils {

	private static final Logger LOG = LoggerFactory.getLogger(SctpUtils.class);

	/**
	 * This constants defines the multiplier, with which the maximal number of
	 * Threads is defined.
	 */
	private static final int THREADPOOL_MULTIPLIER = 100;

	/**
	 * This is the {@link ExecutorService}, which defines the
	 * {@link ThreadPoolExecutor}.
	 */
	@Getter
	private static final ExecutorService threadPoolExecutor = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * THREADPOOL_MULTIPLIER);

	/**
	 * This is the default {@link SctpMapper} instance used in the current session.
	 */
	@Getter
	@Setter
	private static SctpMapper mapper = new SctpMapper();

	/**
	 * This is the default {@link UdpServerLink}, which is used by sctp4nat to be
	 * able to receive sctp association requests. It is instanciated either by the
	 * user or by calling init().
	 */
	@Getter
	@Setter
	private static UdpServerLink link;

	/**
	 * These three constants define the shutdown policy.
	 */
	public static final int SHUT_RD = 1;
	public static final int SHUT_WR = 2;
	public static final int SHUT_RDWR = 3;

	/**
	 * This method initializes the usrsctp library via the {@link Sctp} class. It
	 * also automatically configures {@link SctpMapper} and {@link UdpServerLink}.
	 * Additionally, the default callback behaviour is also specified, since one is
	 * able to define the defaule {@link SctpDataCallback} for the
	 * {@link UdpServerLink}.
	 * 
	 * @param localAddr
	 *            the interface, {@link UdpServerLink} is listening on.
	 * @param localSctpPort
	 *            the assigned SCTP port for usrsctp.
	 * @param cb
	 *            {@link SctpDataCallback}
	 * @throws SocketException
	 *             Thrown, if the {@link SctpSocket} could not be created
	 * @throws SctpInitException
	 *             Thrown, if init() or {@link Sctp}.getInstance().init() is called,
	 *             while usrsctp is already initialized
	 */
	public static synchronized void init(final InetAddress localAddr, final int localSctpPort, SctpDataCallback cb)
			throws SocketException, SctpInitException {

		if (Sctp.isInitialized()) {
			throw new SctpInitException("Sctp is already initialized. You should not initialize it twice!");
		} else {
			Sctp.getInstance().init();
		}

		if (cb == null) {
			cb = new SctpDefaultStreamConfig().getCb();
		}

		if (localAddr == null) {
			throw new SctpInitException("ServerAddress was null! Can't init sctp without a valid InetSocketAddress!");
		} else if (!checkFreePort(localSctpPort) || !checkRange(localSctpPort)) {
			link = new UdpServerLink(mapper, localAddr, cb);
		} else {
			link = new UdpServerLink(mapper, localAddr, localSctpPort, cb);
		}

		if (SctpMapper.isShutdown()) {
			LOG.warn("You are overwriting isShutdown in SctpMapper! This probably causes serious inconsistencies!");
		}
		SctpMapper.setShutdown(false);
	}

	/**
	 * This method checks if the SCTP port is already used.
	 * 
	 * @return true if the port is not already used.
	 */
	private static boolean checkFreePort(final int sctpServerPort) {
		return SctpPorts.getInstance().isUsedPort(sctpServerPort);
	}

	/**
	 * This method checks if a port is in the valid range between 0 and 65535.
	 * 
	 * @param sctpServerPort
	 *            int
	 * @return true if the proposed port is in the valid range.
	 */
	private static boolean checkRange(final int sctpServerPort) {
		return sctpServerPort <= 65535 || sctpServerPort > 0;
	}

	/**
	 * This method shuts down all known instances of {@link NetworkLink},
	 * {@link SctpChannel}, {@link SctpMapper} and {@link SctpPorts}.
	 * 
	 * @return A promise, which is called, once the shutdown process finished.
	 */
	public static Promise<Void, Exception, Void> shutdownAll() {
		return shutdownAll(null, null);
	}

	/**
	 * This method shuts down all known instances of {@link NetworkLink},
	 * {@link SctpChannel}, {@link SctpMapper} and {@link SctpPorts}. Additionally,
	 * a user-defined {@link UdpServerLink} and a user-defined {@link SctpMapper}
	 * can also be shutdown.
	 * 
	 * @param customLink
	 *            user-defined {@link UdpServerLink}
	 * @param customMapper
	 *            user-defined {@link SctpMapper}
	 * @return A promise, which is called, once the shutdown process finished.
	 */
	public static Promise<Void, Exception, Void> shutdownAll(final NetworkLink customLink,
			final SctpMapper customMapper) {
		Deferred<Void, Exception, Void> d = new DeferredObject<>();

		threadPoolExecutor.execute(new Runnable() {

			@Override
			public void run() {
				LOG.debug("sctp4j shutdownAll initialized");

				if (customLink != null) {
					customLink.close();
				}

				if (link != null) {
					link.close();
				}

				if (customMapper != null) {
					try {
						customMapper.shutdown();
					} catch (InterruptedException | TimeoutException e) {
						LOG.error(e.getMessage(), e);
						d.reject(e);
					}
				}

				try {
					mapper.shutdown();
				} catch (InterruptedException | TimeoutException e1) {
					LOG.error(e1.getMessage(), e1);
					d.reject(e1);
				}

				SctpPorts.shutdown();

				try {
					Sctp.getInstance().finish();
				} catch (IOException e) {
					d.reject(e);
				}

				if (d.isPending()) {
					LOG.debug("sctp4j shutdownAll done");
					d.resolve(null);
				} else {
					LOG.error("shutdown all done, but with errors");
				}
				
			}
		});

		return d.promise();
	}
}
