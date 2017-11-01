package net.sctp4j.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4j.connection.SctpUtils;
import net.sctp4j.origin.SctpNotification;
import net.sctp4j.origin.SctpSocket;
import net.sctp4j.origin.SctpSocket.NotificationListener;

public class SctpSocketBuilder {

	//TODO jwa implement all possible variables and parameters for a given SCTP connection

	private static final Logger LOG = LoggerFactory.getLogger(SctpSocketBuilder.class);

	private int localSctpPort = -1;
	private int localPort = -1;
	private InetAddress localAddress = null;
	private int remotePort = -1;
	private InetAddress remoteAddress = null;
	private SctpDataCallback cb = null;
	private NetworkLink link = null;
	private SctpMapper mapper = null;
	
	public SctpSocketAdapter build() throws SctpInitException {

		if (localSctpPort == -1) {
			localSctpPort = SctpPorts.getInstance().generateDynPort();
		}

		if (cb == null) {
			cb = new SctpDataCallback() {
				@Override
				public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags, SctpChannelFacade so) {
					//do nothing
				}
			};
		}
		
		if (mapper == null) {
			LOG.error("No mapper added! You need a mapper to create a new SctpFacade!");
			return null;
		}
		

		InetSocketAddress local = new InetSocketAddress(localAddress, localPort);
		SctpSocketAdapter candidateSo = null;
		if (remoteAddress == null || remotePort == -1) {
			candidateSo = (SctpSocketAdapter) new SctpSocketAdapter(local, localSctpPort, link, cb, mapper);
		} else {
			InetSocketAddress remote = new InetSocketAddress(remoteAddress, remotePort);
			candidateSo = (SctpSocketAdapter) new SctpSocketAdapter(local, localSctpPort, remote, link, cb, mapper);
		}

		final SctpSocketAdapter so = candidateSo;
		so.setNotificationListener(new NotificationListener() {
			
			@Override
			public void onSctpNotification(SctpSocket socket, SctpNotification notification) {
				LOG.debug(notification.toString());
				if (notification.toString().indexOf("SHUTDOWN_COMP") >= 0) {
					so.close();
				} else if (notification.toString().indexOf("ADDR_UNREACHABLE") >= 0){
					LOG.error("Heartbeat missing! Now shutting down the SCTP connection...");
					so.close();
				}  else if (notification.toString().indexOf("COMM_LOST") >= 0){
					LOG.error("Communication aborted! Now shutting down the udp connection...");
					so.close();
				} 
			}
		});
		
		return so;
	}

	public SctpSocketBuilder localPort(int localPort) {
		if (isInRange(localPort)) {
			this.localPort = localPort;
		} else {
			LOG.error("Port is out of range (possible range: 0-65535)!");
			return null;
		}
		return this;
	}

	public SctpSocketBuilder localSctpPort(int localSctpPort) {
		if (isInRange(localSctpPort)) {
			this.localSctpPort = localSctpPort;
		} else {
			LOG.error("Port is out of range (possible range: 0-65535)!");
			return null;
		}
		return this;
	}

	private boolean isInRange(int localPort) {
		return localPort >= 0 && localPort < SctpPorts.MAX_PORT;
	}

	public SctpSocketBuilder localAddress(InetAddress localAddress) {
		if (localAddress != null) {
			this.localAddress = localAddress;
		} else {
			LOG.error("Null can't be added as localAddress!");
		}
		return this;
	}

	public SctpSocketBuilder remotePort(int remotePort) {
		if (isInRange(remotePort)) {
			this.remotePort = remotePort;
		} else {
			LOG.error("Port is out of range (possible range: 0-65535)!");
		}
		return this;
	}

	public SctpSocketBuilder remoteAddress(InetAddress remoteAddress) {
		if (remoteAddress != null) {
			this.remoteAddress = remoteAddress;
		} else {
			LOG.error("Null can't be added as remoteAddress!");
		}
		return this;
	}

	public SctpSocketBuilder sctpDataCallBack(SctpDataCallback cb) {
		if (cb != null) {
			this.cb = cb;
		} else {
			LOG.error("Null can't be added as dataCallback!");
		}
		return this;
	}

	public SctpSocketBuilder networkLink(NetworkLink link) {
		if (link != null) {
			this.link = link;
		} else {
			LOG.error("Null can't be added as networkLink!");
		}
		return this;
	}
	
	public SctpSocketBuilder mapper(SctpMapper mapper) {
		if (mapper != null) {
			this.mapper = mapper;
		} else {
			LOG.error("Null can't be added as mapper!");
		}
		return this;
	}
}
