package net.sctp4j.connection;

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
import net.sctp4j.connection.SctpUpgradeable;
import net.sctp4j.core.NetworkLink;
import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpChannelFacade;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpSocketBuilder;
import net.sctp4j.core.UdpClientLink;
import net.sctp4j.core.UdpServerLink;

public class UpgradeableUdpSocket extends DatagramSocket implements SctpUpgradeable {

	private static final String UPGRADE = "upgrade"; // TODO jwa change this to something better
	private static final String UPGRADE_COMPLETE = "upgradeComplete"; // TODO jwa change this to something better
	private static final Logger LOG = LoggerFactory.getLogger(UpgradeableUdpSocket.class);

	@Getter
	private boolean isUgrading = false;

	@Getter
	@Setter
	private SctpDataCallback cb;

	public UpgradeableUdpSocket(SocketAddress bindaddr) throws SocketException {
		super(bindaddr);
	}

	public UpgradeableUdpSocket(int port, InetAddress laddr) throws SocketException {
		super(port, laddr);
	}

	public UpgradeableUdpSocket(int port) throws SocketException {
		super(port);
	}

	protected UpgradeableUdpSocket(DatagramSocketImpl impl) {
		super(impl);
	}

	public UpgradeableUdpSocket() throws SocketException {
		super();
	}

	@Override
	public synchronized void receive(final DatagramPacket packet) throws IOException {
		if (new String(packet.getData(), 0, UPGRADE.length(), StandardCharsets.UTF_8).equals(UPGRADE)) {
			isUgrading = true;
			LOG.debug("SctpUpgrade request received! starting upgrade procedure with remote endpoint: "
					+ packet.getAddress().getHostName() + ":" + packet.getPort());
			replyUpgrade(packet);
		} else {
			super.receive(packet);
		}
	}

	private void replyUpgrade(final DatagramPacket packet) throws IOException {
		Promise<NetworkLink, Exception, Object> p = initUpgrade(this.getChannel().getLocalAddress());

		p.done(new DoneCallback<NetworkLink>() {

			@Override
			public void onDone(NetworkLink result) {
				byte[] buff = new byte[2048];
				DatagramPacket reply = new DatagramPacket(buff, 2048);
				reply.setAddress(packet.getAddress());
				reply.setPort(packet.getPort());
				reply.setData(UPGRADE_COMPLETE.getBytes());
				try {
					LOG.debug("Upgrade procedure finished.");
					UpgradeableUdpSocket.this.send(reply);
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

	@Override
	public Promise<NetworkLink, Exception, Object> initUpgrade(SocketAddress local) {
		Deferred<NetworkLink, Exception, Object> d = new DeferredObject<>();

		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

			@Override
			public void run() {
				try {
					InetSocketAddress localAddress = (InetSocketAddress) local;
					UdpServerLink link = new UdpServerLink(SctpUtils.getMapper(), localAddress, cb,
							UpgradeableUdpSocket.this);
					d.resolve(link);
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
					d.reject(e);
				}
			}
		});

		return d.promise();
	}

	@Override
	public Promise<SctpChannelFacade, Exception, NetworkLink> upgrade(final SctpDefaultConfig config,
			final InetSocketAddress local, final InetSocketAddress remote) {
		isUgrading = true;

		LOG.debug("Upgrade procedure started! Trying to establish sctp connection to "
				+ remote.getAddress().getHostName() + ":" + remote.getPort());

		Deferred<SctpChannelFacade, Exception, NetworkLink> d = new DeferredObject<>();

		// TODO jwa implement config

		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

			@Override
			public void run() {

				SctpAdapter so = new SctpSocketBuilder().localAddress(local.getAddress()).localPort(local.getPort())
						.localSctpPort(local.getPort()).remoteAddress(remote.getAddress()).remotePort(remote.getPort())
						.mapper(SctpUtils.getMapper()).sctpDataCallBack(cb).build();

				UdpClientLink link = new UdpClientLink(local, remote, so, UpgradeableUdpSocket.this);
				d.notify(link);

				try {
					byte[] buff = new byte[2048];
					DatagramPacket request = new DatagramPacket(buff, 2048);
					request.setData(UPGRADE.getBytes());
					request.setAddress(remote.getAddress());
					request.setPort(remote.getPort());
					send(request);
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
					isUgrading = false;
					d.reject(e);
				}

				byte[] buff = new byte[2048];
				DatagramPacket reply = new DatagramPacket(buff, 2048);

				try {
					receive(reply);
					if (new String(reply.getData(), 0, UPGRADE_COMPLETE.length(), StandardCharsets.UTF_8).trim().equals(UPGRADE_COMPLETE)) {
						Promise<SctpAdapter, Exception, Object> p = so.connect(remote);

						p.done(new DoneCallback<SctpAdapter>() {
							@Override
							public void onDone(SctpAdapter result) {
								isUgrading = false;
								LOG.debug("Upgrade procedure successfully finished.");
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

					} else {
						throw new IOException(
								"wrong reply received! reply:" + new String(reply.getData(), StandardCharsets.UTF_8));
					}
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
					isUgrading = false;
					d.reject(e);
				}
			}
		});

		return d.promise();
	}

}