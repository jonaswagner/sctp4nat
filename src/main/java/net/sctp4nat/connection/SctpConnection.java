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
package net.sctp4nat.connection;

import java.net.InetSocketAddress;

import org.jdeferred.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Builder;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelBuilder;
import net.sctp4nat.core.SctpChannelFacade;

/**
 * 
 * @author Jonas Wagner
 * 
 *         This class combines the use of the builder pattern with the
 *         possibility to automatically connect to the desired remote endpoint.
 */
@Builder
public class SctpConnection {

	private static final Logger LOG = LoggerFactory.getLogger(SctpConnection.class);

	private SctpDefaultStreamConfig config;
	private InetSocketAddress local;
	private InetSocketAddress remote;
	@Builder.Default private int localSctpPort = SctpPorts.PORT_NOT_INITIALIZED;

	/**
	 * This method calls {@link SctpChannel}.connect() and therefore causes usrsctp
	 * to start the handshake with the remote endpoint.
	 * 
	 * @param link
	 *            The {@link NetworkLink}, on which packets are sent
	 * @return A {@link Promise} object
	 * @throws Exception
	 *             Possible {@link Exception}s are {@link SctpInitException} and
	 *             {@link NullPointerException}.
	 */
	public Promise<SctpChannelFacade, Exception, Void> connect(final NetworkLink link) throws Exception {

		if (remote == null) {
			LOG.error("Remote InetSocketAddress was null. We can't connect to null!");
			throw new NullPointerException("Remote InetSocketAddress was null. We can't connect to null!");
		}

		if (local == null) {
			LOG.error("Local InetSocketAddress was null. We can't connect to null!");
			throw new NullPointerException("Local InetSocketAddress was null. We can't connect to null!");
		}

		if (config == null) {
			config = new SctpDefaultStreamConfig();
		}

		if (localSctpPort == SctpPorts.PORT_NOT_INITIALIZED) {
			localSctpPort = remote.getPort();
		}

		SctpChannel socket = null;
		try {
		socket = new SctpChannelBuilder().remoteAddress(remote.getAddress()).remotePort(remote.getPort())
				.mapper(SctpUtils.getMapper()).localSctpPort(localSctpPort).build();
		} catch (SctpInitException e) {
			LOG.error("Could not create SctpChannel, because Sctp is not initialized! Try SctpUtils.init()");
			throw new SctpInitException(e.getMessage());
		}

		if (socket == null) {
			throw new NullPointerException("Could not create SctpSocketAdapter!");
		}

		NetworkLink link2 = link;
		if (link == null) {
			link2 = new UdpClientLink(local, remote, socket);
		}

		final SctpChannel so = socket;

		if (link2 == null) {
			LOG.error("Could not create NetworkLink");
			SctpPorts.getInstance().removePort(so);
			so.close();
			throw new NullPointerException("NetworkLink was null!");
		}

		so.setLink(link2);

		Promise<SctpChannelFacade, Exception, Void> p = so.connect(remote);

		return p;

	}
}
