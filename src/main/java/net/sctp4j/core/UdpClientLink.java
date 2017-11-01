package net.sctp4j.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.NotFoundException;
import lombok.Getter;
import net.sctp4j.connection.SctpUtils;

/**
 * @author Jonas Wagner
 * 
 *         This class holds the {@link DatagramSocket} the outgoing
 *         SCTP connection attempts.
 *
 */
public class UdpClientLink implements NetworkLink {

	private final static Logger LOG = LoggerFactory.getLogger(UdpClientLink.class);

	/**
	 * <tt>SctpFacade</tt> instance that is used in this connection.
	 */
	@Getter
	private final SctpSocketAdapter so;

	/**
	 * Udp socket used for transport.
	 */
	private final DatagramSocket udpSocket;

	/**
	 * Destination <tt>InetSocketAddress</tt>.
	 */
	@Getter
	private final InetSocketAddress remote;

	/**
	 * Trigger to end the wrapper thread.
	 */
	private boolean isShutdown = false;

	/**
	 * Creates new instance of <tt>UdpClientLink</tt>.
	 */
	public UdpClientLink(final InetSocketAddress local, final InetSocketAddress remote, final SctpSocketAdapter so)
			throws IOException {
		this.so = so;
		this.so.setLink(this);
		this.remote = remote;
		this.udpSocket = new DatagramSocket(local.getPort(), local.getAddress());

		// Listening thread
		receive(remote, so);
	}

	/**
	 * Do not use this constructor without a valid {@link DatagramSocket}! Creates
	 * new instance of <tt>UdpClientLink</tt>.
	 */
	public UdpClientLink(final InetSocketAddress local, final InetSocketAddress remote, final SctpSocketAdapter so,
			final DatagramSocket udpSocket) {
		this.so = so;
		this.so.setLink(this);
		this.remote = remote;
		this.udpSocket = udpSocket;

		// Listening thread
		receive(remote, so);
	}

	/**
	 * Forwards the UDP packets to the native counterpart.
	 * @param remote
	 * 			{@link SctpSocketAdapter}
	 * @param so
	 */
	private void receive(final InetSocketAddress remote, final SctpSocketAdapter so) {
		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {
			public void run() {
				try {
					byte[] buff = new byte[2048];
					DatagramPacket p = new DatagramPacket(buff, 2048);
					while (!isShutdown) {
						udpSocket.receive(p);
						so.onConnIn(p.getData(), p.getOffset(), p.getLength());
					}
				} catch (IOException e) {
					LOG.error(e.getMessage());
				}
				LOG.debug(
						"Link shutdown, closing udp connection to " + remote.getHostString() + ":" + remote.getPort());
			}
		});
	}

	@Override
	public void onConnOut(final SctpChannelFacade so, final byte[] data) throws IOException, NotFoundException {
		DatagramPacket packet = new DatagramPacket(data, data.length, this.remote.getAddress(), this.remote.getPort());
		udpSocket.send(packet);
	}

	@Override
	public void close() {
		this.isShutdown = true;
		this.udpSocket.close();
	}

}
