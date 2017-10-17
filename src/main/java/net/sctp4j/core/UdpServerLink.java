package net.sctp4j.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import net.sctp4j.origin.SctpNotification;
import net.sctp4j.origin.SctpSocket;
import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.NotFoundException;
import lombok.Getter;
import net.sctp4j.connection.SctpUtils;

public class UdpServerLink implements NetworkLink {

	private static final Logger LOG = LoggerFactory.getLogger(UdpServerLink.class);
	private final SctpMapper mapper;

	/**
	 * Udp socket used for transport.
	 */
	private final DatagramSocket udpSocket;

	/**
	 * Trigger to end the wrapper thread.
	 */
	private boolean isShutdown = false;

	@Getter
	private int port = -1;

	/**
	 * Creates new instance of <tt>UdpConnection</tt>. The default port used will be
	 * 9899.
	 */
	public UdpServerLink(final SctpMapper mapper, final InetAddress local, final SctpDataCallback cb)
			throws SocketException {
		this(mapper, local, SctpPorts.SCTP_TUNNELING_PORT, cb);
	}

	/**
	 * Creates new instance of <tt>UdpConnection</tt>.
	 */
	public UdpServerLink(final SctpMapper mapper, final InetAddress localAddress, final int localPort,
			final SctpDataCallback cb) throws SocketException {
		this.mapper = mapper;
		this.port = localPort;
		this.udpSocket = new DatagramSocket(localPort, localAddress);
		SctpUtils.setLink(this);
		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

			@Override
			public void run() {
				SctpAdapter so = null;

				while (!isShutdown) {
					byte[] buff = new byte[2048];
					DatagramPacket p = new DatagramPacket(buff, 2048);

					try {
						udpSocket.receive(p);

						InetSocketAddress remote = new InetSocketAddress(p.getAddress(), p.getPort());
						so = SctpMapper.locate(p.getAddress().getHostAddress(), p.getPort());

						/*
						 * If so is null it means that we don't know the other Sctp endpoint yet. Thus,
						 * we need to reply their handshake with INIT ACK.
						 */
						if (so == null) {
							so = replyHandshake(localAddress, localPort, p.getAddress(), p.getPort(), cb);
							mapper.register(remote, so);
//							so.setNotificationListener(new SctpSocket.NotificationListener() {
//								@Override
//								public void onSctpNotification(SctpSocket socket, SctpNotification notification) {
//									if (notification.toString().contains("COMM_UP")) {
//										try {
//											socket.acceptNative();
//										} catch (IOException e) {
//											LOG.error("Could not accept endpoint!", e);
//										}
//									}
//								}
//							});
							so.onConnIn(p.getData(), p.getOffset(), p.getLength());
						} else {
							so.onConnIn(p.getData(), p.getOffset(), p.getLength());
						}
					} catch (IOException e) {
						LOG.error("Error while receiving packet in UDPClientLink.class!", e);
					}
				}
				LOG.debug("Link shutdown, stop listening, closing udp connection");
			}
		});
	}

	@Override
	public void onConnOut(SctpAdapter so, byte[] data) throws IOException, NotFoundException {
		DatagramPacket packet = new DatagramPacket(data, data.length, so.getRemote());
		udpSocket.send(packet);
	}

	private SctpAdapter replyHandshake(final InetAddress localAddress, final int localPort,
			final InetAddress remoteAddress, final int remotePort, final SctpDataCallback cb) {
		// since there is no socket yet, we need to create one first
		SctpAdapter so = new SctpSocketBuilder().networkLink(UdpServerLink.this).localAddress(localAddress)
				.localPort(localPort).localSctpPort(localPort).sctpDataCallBack(cb).remoteAddress(remoteAddress)
				.remotePort(remotePort).mapper(mapper).build();
		so.listen();
		return so;
	}

	@Override
	public void close() {
		this.isShutdown = true;
		udpSocket.close();
	}
}
