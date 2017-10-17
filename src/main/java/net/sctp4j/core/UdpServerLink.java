package net.sctp4j.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

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
						if (so == null) {
							so = setupSocket(localAddress, localPort, p.getAddress(), p.getPort(), cb);
							mapper.register(remote, so);
							so.onConnIn(p.getData(), p.getOffset(), p.getLength());
						} else {
							so.onConnIn(p.getData(), p.getOffset(), p.getLength());
						}
					} catch (IOException e) {
						if (!isShutdown) {
							LOG.error("Error while receiving packet in UDPClientLink.class!", e);
						} else {
							LOG.debug("receive aborted because of shutdown!");
						}
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

	/**
	 * Since there is no socket yet, we need to create one first.
	 * 
	 * @param localAddress {@link InetAddress}
	 * @param localPort {@link Integer}
	 * @param remoteAddress {@link InetAddress}
	 * @param remotePort {@link Integer}
	 * @param cb {@link SctpDataCallback}
	 * @return so {@link SctpAdapter}
	 */
	private SctpAdapter setupSocket(final InetAddress localAddress, final int localPort,
			final InetAddress remoteAddress, final int remotePort, final SctpDataCallback cb) {
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
