package net.sctp4nat.sample.extended;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import net.sctp4nat.connection.SctpDefaultConfig;
import net.sctp4nat.connection.SctpUtils;
import net.sctp4nat.connection.UdpClientLink;
import net.sctp4nat.connection.UdpServerLink;
import net.sctp4nat.core.NetworkLink;
import net.sctp4nat.core.SctpDataCallback;
import net.sctp4nat.core.SctpInitException;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelBuilder;
import net.sctp4nat.core.SctpChannelFacade;

public class UpgradeableUdpSocketSample extends DatagramSocket {

	private String UPGRADE = "upgrade"; // TODO jwa change this to something better
	private String UPGRADE_COMPLETE = "upgradeComplete"; // TODO jwa change this to something better
	private static final Logger LOG = LoggerFactory.getLogger(UpgradeableUdpSocketSample.class);

	private final Deferred<SctpChannelFacade, Exception, NetworkLink> d = new DeferredObject<>();
	private SctpChannel so;
	private InetSocketAddress remote;
	
	@Getter
	private boolean isUgrading = false;
	
	@Getter
	@Setter
	private SctpDataCallback cb;

	public UpgradeableUdpSocketSample(SocketAddress bindaddr) throws SocketException {
		super(bindaddr);
	}

	public UpgradeableUdpSocketSample(int port, InetAddress laddr) throws SocketException {
		super(port, laddr);
	}

	public UpgradeableUdpSocketSample(int port) throws SocketException {
		super(port);
	}

	protected UpgradeableUdpSocketSample(DatagramSocketImpl impl) {
		super(impl);
	}

	public UpgradeableUdpSocketSample() throws SocketException {
		super();
	}

	@Override
	public synchronized void receive(final DatagramPacket packet) throws IOException {
		super.receive(packet);
		if (decodeString(packet).equals(UPGRADE) && !isUgrading) {
			synchronized (this) {
			isUgrading = true;
			LOG.debug("SctpUpgrade request received! starting upgrade procedure with remote endpoint: "
					+ packet.getAddress().getHostName() + ":" + packet.getPort());
				replyUpgrade(packet);
			}
		} else if (decodeString(packet).equals(UPGRADE_COMPLETE) && isUgrading) {
			synchronized (this) {
				LOG.debug("upgrade procedure finished received, now starting to connect via sctp...");
				setUpSctp();
			}
		}
	}

	private String decodeString(final DatagramPacket packet) {
		String current = new String(packet.getData(), StandardCharsets.UTF_8);
		current = current.trim();
		return current;
	}
	
	private void replyUpgrade(final DatagramPacket packet) throws IOException {
		Promise<NetworkLink, Exception, Object> p = initUpgrade(this.getLocalSocketAddress());

		p.done(new DoneCallback<NetworkLink>() {

			@Override
			public void onDone(NetworkLink result) {
				DatagramPacket reply = new DatagramPacket(UPGRADE_COMPLETE.getBytes(), UPGRADE_COMPLETE.length());
				reply.setAddress(packet.getAddress());
				reply.setPort(packet.getPort());
				try {
					LOG.debug("Upgrade procedure finished.");
					UpgradeableUdpSocketSample.this.send(reply);
				} catch (IOException e) {
					LOG.error("Could not setup sctp upgrade on udp connection!");
					LOG.error(e.getMessage(), e);
					isUgrading = false;
				}
			}
		});

		p.fail(new FailCallback<Exception>() {

			@Override
			public void onFail(Exception result) {
				LOG.error("Could not setup sctp upgrade on udp connection!");
				LOG.error(result.getMessage(), result);
				isUgrading = false;
			}
		});
	}

	/**
	 * This method answers the transition attempt from the server side and
	 * transitions the connection from plain udp to sctp via udp.
	 * 
	 * @param local
	 * @return	promise
	 */
	private Promise<NetworkLink, Exception, Object> initUpgrade(SocketAddress local) {
		Deferred<NetworkLink, Exception, Object> d = new DeferredObject<>();

		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

			@Override
			public void run() {
				try {
					InetSocketAddress localAddress = (InetSocketAddress) local;
					UdpServerLink link = new UdpServerLink(SctpUtils.getMapper(), localAddress, cb,
							UpgradeableUdpSocketSample.this);
					d.resolve(link);
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
					d.reject(e);
				}
			}
		});

		return d.promise();
	}

	public Promise<SctpChannelFacade, Exception, NetworkLink> upgrade(final SctpDefaultConfig config,
			final InetSocketAddress local, final InetSocketAddress remote) {
		this.isUgrading = true;
		
		LOG.debug("Upgrade procedure started! Trying to establish sctp connection to "
				+ remote.getAddress().getHostName() + ":" + remote.getPort());
		
		this.remote = remote;
		
		// TODO check if defereed is not used 
		// TODO jwa implement config

		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {
			
			@Override
			public void run() {

				try {
					so = new SctpChannelBuilder()
							.localSctpPort(local.getPort())
							.remoteAddress(remote.getAddress())
							.remotePort(remote.getPort())
							.mapper(SctpUtils.getMapper())
							.sctpDataCallBack(cb).build();
				} catch (SctpInitException e1) {
					LOG.error("Could not create SctpSocketAdapter!");
					d.reject(e1);
				}

				UdpClientLink link = new UdpClientLink(local, remote, so, UpgradeableUdpSocketSample.this);
				d.notify(link);

				try {
					DatagramPacket request = new DatagramPacket(UPGRADE.getBytes(), UPGRADE.length());
					request.setAddress(remote.getAddress());
					request.setPort(remote.getPort());
					send(request);
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
					isUgrading = false;
					d.reject(e);
				}
			}
		});

		return d.promise();
	}
	
	private void setUpSctp() {
		Promise<SctpChannelFacade, Exception, Object> p = so.connect(remote);

		p.done(new DoneCallback<SctpChannelFacade>() {
			@Override
			public void onDone(SctpChannelFacade result) {
				isUgrading = false;
				LOG.debug("Sctp connection success!");
				LOG.debug("Upgrade procedure successfully finished. This udp socket is now connected via sctp!");
				d.resolve(result);
			}
		});

		p.fail(new FailCallback<Exception>() {
			@Override
			public void onFail(Exception result) {
				isUgrading = false;
				d.reject(result);
			}
		});
	}

}