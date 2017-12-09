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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.connection.NetworkLink;
import net.sctp4nat.origin.SctpAcceptable;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.origin.SctpNotification;
import net.sctp4nat.origin.SctpSocket.NotificationListener;
import net.sctp4nat.util.SctpInitException;

/**
 * This class helps instantiating a clean {@link SctpChannel}. It is designed
 * according to the Builder pattern.
 * 
 * @author Jonas Wagner
 *
 */
public class SctpChannelBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(SctpChannelBuilder.class);

	private int localSctpPort = SctpPorts.PORT_NOT_INITIALIZED;
	private int remotePort = SctpPorts.PORT_NOT_INITIALIZED;
	private InetAddress remoteAddress = null;
	private SctpDataCallback cb = null;
	private NetworkLink link = null;
	private SctpMapper mapper = null;

	/**
	 * This method triggers the creation of the {@link SctpChannel} object.
	 * 
	 * @return A {@link SctpChannel} instance.
	 * @throws SctpInitException
	 *             Thrown, if usrsctp was not initialized.
	 */
	public SctpChannel build() throws SctpInitException {
		LOG.info("start building an SctpChannel object...");
		
		if (localSctpPort == SctpPorts.PORT_NOT_INITIALIZED) {
			localSctpPort = SctpPorts.getInstance().generateDynPort();
		}

		if (cb == null) {
			cb = new SctpDataCallback() {
				@Override
				public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
						SctpChannelFacade facade) {
					LOG.trace("Sctp message from {}/{} ignored", facade.getRemote().getAddress().getHostAddress(), facade.getRemote().getPort());
					// do nothing
				}
			};
		}

		if (mapper == null) {
			LOG.error("No mapper added! You need a mapper to create a new SctpFacade!");
			return null;
		}

		SctpChannel candidateSo = null;
		if (remoteAddress == null || remotePort == SctpPorts.PORT_NOT_INITIALIZED) {
			candidateSo = (SctpChannel) new SctpChannel(localSctpPort, link, cb, mapper);
		} else {
			InetSocketAddress remote = new InetSocketAddress(remoteAddress, remotePort);
			candidateSo = (SctpChannel) new SctpChannel(localSctpPort, remote, link, cb, mapper);
		}

		final SctpChannel so = candidateSo;
		so.setNotificationListener(new NotificationListener() {

			@Override
			public void onSctpNotification(SctpAcceptable socket, SctpNotification notification) {
				LOG.debug(notification.toString());
				if (notification.toString().indexOf(SctpNotification.SHUTDOWN_COMP_STR) >= 0) {
					so.close();
				} else if (notification.toString().indexOf(SctpNotification.ADDR_UNREACHABLE_STR) >= 0) {
					LOG.error("Heartbeat missing! Now shutting down the SCTP connection...");
					so.close();
				} else if (notification.toString().indexOf(SctpNotification.COMM_LOST_STR) >= 0) {
					LOG.error("Communication aborted! Now shutting down the udp connection...");
					so.close();
				} else if (notification.toString().indexOf(SctpNotification.SHUTDOWN_EVENT_STR) > 0) {
					so.close();
				}
			}

		});

		LOG.info("...finished building the SctpChannel object");
		return so;
	}

	public SctpChannelBuilder localSctpPort(int localSctpPort) {
		if (SctpPorts.getInstance().isInValidRange(localSctpPort)) {
			if (!SctpPorts.getInstance().isUsedPort(localSctpPort)) {
				this.localSctpPort = localSctpPort;
			} else {
				LOG.error("Port " + localSctpPort + " is already assigned!");
				return null;
			}
		} else {
			LOG.error("Port is out of range (possible range: {}-{}) or is already assigned!", SctpPorts.MIN_PORT, SctpPorts.MAX_PORT);
			return null;
		}
		return this;
	}

	public SctpChannelBuilder remotePort(int remotePort) {
		if (SctpPorts.getInstance().isInValidRange(remotePort)) {
			this.remotePort = remotePort;
		} else {
			LOG.error("Port is out of range (possible range: {}-{})!", SctpPorts.MIN_PORT, SctpPorts.MAX_PORT);
		}
		return this;
	}

	public SctpChannelBuilder remoteAddress(InetAddress remoteAddress) {
		if (remoteAddress != null) {
			this.remoteAddress = remoteAddress;
		} else {
			LOG.error("Null can't be added as remoteAddress!");
		}
		return this;
	}

	public SctpChannelBuilder sctpDataCallBack(SctpDataCallback cb) {
		if (cb != null) {
			this.cb = cb;
		} else {
			LOG.error("Null can't be added as dataCallback!");
		}
		return this;
	}

	public SctpChannelBuilder networkLink(NetworkLink link) {
		if (link != null) {
			this.link = link;
		} else {
			LOG.error("Null can't be added as networkLink!");
		}
		return this;
	}

	public SctpChannelBuilder mapper(SctpMapper mapper) {
		if (mapper != null) {
			this.mapper = mapper;
		} else {
			LOG.error("Null can't be added as mapper!");
		}
		return this;
	}
}
