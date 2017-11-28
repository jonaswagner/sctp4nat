package net.sctp4nat.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.exception.SctpInitException;
import net.sctp4nat.origin.SctpAcceptable;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.origin.SctpNotification;
import net.sctp4nat.origin.SctpSocket.NotificationListener;

/**
 * This class helps instantiating a clean {@link SctpChannel}.
 * 
 * @author root
 *
 */
public class SctpChannelBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(SctpChannelBuilder.class);

	private int localSctpPort = -1;
	private int remotePort = -1;
	private InetAddress remoteAddress = null;
	private SctpDataCallback cb = null;
	private NetworkLink link = null;
	private SctpMapper mapper = null;

	public SctpChannel build() throws SctpInitException {

		if (localSctpPort == -1) {
			localSctpPort = SctpPorts.getInstance().generateDynPort();
		}

		if (cb == null) {
			cb = new SctpDataCallback() {
				@Override
				public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
						SctpChannelFacade facade) {
					// do nothing
				}
			};
		}

		if (mapper == null) {
			LOG.error("No mapper added! You need a mapper to create a new SctpFacade!");
			return null;
		}

		SctpChannel candidateSo = null;
		if (remoteAddress == null || remotePort == -1) {
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
				if (notification.toString().indexOf("SHUTDOWN_COMP") >= 0) {
					so.close();
				} else if (notification.toString().indexOf("ADDR_UNREACHABLE") >= 0) {
					LOG.error("Heartbeat missing! Now shutting down the SCTP connection...");
					so.close();
				} else if (notification.toString().indexOf("COMM_LOST") >= 0) {
					LOG.error("Communication aborted! Now shutting down the udp connection...");
					so.close();
				} else if (notification.toString().indexOf("SCTP_SHUTDOWN_EVENT") > 0) {
					so.close();
				}
			}

		});

		return so;
	}

	public SctpChannelBuilder localSctpPort(int localSctpPort) {
		if (isInRange(localSctpPort)) {
			if (!SctpPorts.getInstance().isUsedPort(localSctpPort)) {
				this.localSctpPort = localSctpPort;
			} else {
				LOG.error("Port " + localSctpPort + " is already assigned!");
				return null;
			}
		} else {
			LOG.error("Port is out of range (possible range: 0-65535) or is already assigned!");
			return null;
		}
		return this;
	}

	private boolean isInRange(int localPort) {
		return localPort >= 0 && localPort < SctpPorts.MAX_PORT;
	}

	public SctpChannelBuilder remotePort(int remotePort) {
		if (isInRange(remotePort)) {
			this.remotePort = remotePort;
		} else {
			LOG.error("Port is out of range (possible range: 0-65535)!");
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
