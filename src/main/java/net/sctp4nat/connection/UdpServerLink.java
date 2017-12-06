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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.NotFoundException;
import net.sctp4nat.core.NetworkLink;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelBuilder;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.origin.SctpSocket;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;

/**
 * This class holds the common {@link DatagramSocket} for all incoming SCTP
 * connection attempts. By default it should be using port 9899 (default SCTP
 * via UDP port).
 * 
 * @author Jonas Wagner
 *
 */
public class UdpServerLink implements NetworkLink {

	private static final Logger LOG = LoggerFactory.getLogger(UdpServerLink.class);

	/**
	 * UDP socket used for transport.
	 */
	private final DatagramSocket udpSocket;

	/**
	 * Trigger to end the wrapper thread.
	 */
	private boolean isShutdown = false;

	/**
	 * 
	 * Creates new instance of {@link UdpServerLink}. The default port used will be
	 * 9899.
	 *
	 * @param mapper
	 *            The {@link SctpMapper} instance
	 * @param local
	 *            The local {@link InetAddress}
	 * @param cb
	 *            The {@link SctpDataCallback} used to reply.
	 * @throws SocketException
	 *             Thrown, if the {@link DatagramSocket} could not be created.
	 */
	public UdpServerLink(final SctpMapper mapper, final InetAddress local, final SctpDataCallback cb)
			throws SocketException {
		this(mapper, local, SctpPorts.SCTP_TUNNELING_PORT, cb);
	}

	/**
	 * 
	 * Creates new instance of {@link UdpServerLink}.
	 * 
	 * @param mapper
	 *            The {@link SctpMapper} instance
	 * @param local
	 *            The local {@link InetAddress} used for the {@link DatagramSocket}.
	 * @param localPort
	 *            The port used for the {@link DatagramSocket}.
	 * @param cb
	 *            The {@link SctpDataCallback} used to reply.
	 * @throws SocketException
	 *             Thrown, if the {@link DatagramSocket} could not be created.
	 */
	public UdpServerLink(final SctpMapper mapper, final InetAddress localAddress, final int localPort,
			final SctpDataCallback cb) throws SocketException {
		this.udpSocket = new DatagramSocket(localPort, localAddress);
		SctpUtils.setLink(this); // set this as main Link
		receive(mapper, localAddress, localPort, cb);
	}

	/**
	 * Creates new instance of {@link UdpServerLink}. 
	 * 
	 * @param mapper
	 *            The {@link SctpMapper} instance
	 * @param cb
	 *            The {@link SctpDataCallback} used to reply.
	 * @param udpSocket
	 *            An already existing {@link DatagramSocket} instance.
	 */
	public UdpServerLink(SctpMapper mapper, InetSocketAddress local, SctpDataCallback cb, DatagramSocket udpSocket) {
		this.udpSocket = udpSocket;
		receive(mapper, local.getAddress(), local.getPort(), cb);
	}

	/**
	 * If a new packet arrives over
	 * the contained {@link DatagramSocket}, the method first checks, if the remote
	 * endpoint is already known. If if is known, the packet is decoded and
	 * forwarded to the native counterpart (usrsctp) via onConnIn(). If not, this
	 * means, that an new SCTP endpoint wants to connect to this SCTP endpoint. Once
	 * the SctpChannel is returned by setupSocket(), the
	 * {@link SctpChannel} gets registered on {@link SctpMapper}. After the
	 * registration, onConnIn() is called to forward the INIT message from
	 * the remote endpoint to the newly created SctpChannel.
	 * 
	 * @param mapper
	 *            The {@link SctpMapper} instance
	 * @param localAddress
	 *            The local {@link InetAddress} used for the {@link DatagramSocket}.
	 * @param localPort
	 *            The port used for the {@link DatagramSocket}.
	 * @param cb
	 *            The {@link SctpDataCallback} used to reply.
	 */
	private void receive(final SctpMapper mapper, final InetAddress localAddress, final int localPort,
			final SctpDataCallback cb) {
		SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

			@Override
			public void run() {
				SctpChannel so = null;

				while (!isShutdown) {
					byte[] buff = new byte[UDP_DEFAULT_BUFFER_SIZE];
					DatagramPacket p = new DatagramPacket(buff, UDP_DEFAULT_BUFFER_SIZE);

					try {
						udpSocket.receive(p);

						InetSocketAddress remote = new InetSocketAddress(p.getAddress(), p.getPort());
						so = SctpMapper.locate(p.getAddress().getHostAddress(), p.getPort());
						if (so == null) {
							LOG.info("New INIT arrived. Now starting the setupSocket() process...");
							so = setupSocket(localAddress, localPort, p.getAddress(), p.getPort(), cb, mapper);
							mapper.register(remote, so);
							LOG.info("onConnIn() Called with IP and port /" + p.getAddress().getHostAddress() + ":"
									+ p.getPort());
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
					} catch (SctpInitException e) {
						LOG.error("Sctp is currently not initialized! Try init it with SctpUtils.init(...)", e);
					}
				}
				LOG.debug("Link shutdown, stop listening, closing udp connection");
			}
		});
	}

	@Override
	public void onConnOut(SctpChannelFacade facade, byte[] data, final int tos) throws IOException, NotFoundException {
		DatagramPacket packet = new DatagramPacket(data, data.length, (SocketAddress) facade.getRemote());
		udpSocket.send(packet);
	}

	/**
	 * Since there is no socket yet, we need to create one first.
	 * 
	 * @param localAddress
	 *            {@link InetAddress}
	 * @param localPort
	 *            {@link Integer}
	 * @param remoteAddress
	 *            {@link InetAddress}
	 * @param remotePort
	 *            {@link Integer}
	 * @param cb
	 *            {@link SctpDataCallback}
	 * @return so {@link SctpChannel}
	 * @throws SctpInitException
	 */
	private SctpChannel setupSocket(final InetAddress localAddress, final int localPort,
			final InetAddress remoteAddress, final int remotePort, final SctpDataCallback cb, final SctpMapper mapper)
			throws SctpInitException {
		SctpChannel so = new SctpChannelBuilder().networkLink(UdpServerLink.this).localSctpPort(localPort)
				.sctpDataCallBack(cb).remoteAddress(remoteAddress).remotePort(remotePort).mapper(mapper).build();
		LOG.info("new SctpChannel object created --> " + so.toString());
		so.listen();
		return so;
	}

	/**
	 * Do not call this method while other corresponding {@link SctpSocket}s are
	 * still open!!! This method closes the {@link DatagramSocket}.
	 */
	@Override
	public void close() {
		this.isShutdown = true;
		udpSocket.close();
	}

	@Override
	public String toString() {
		InetSocketAddress local = (InetSocketAddress) this.udpSocket.getLocalSocketAddress();

		return "UdpClientLink(" + "Local(" + local.getAddress().getHostAddress() + ":" + local.getPort()
				+ "), shutdown is " + isShutdown + ")";

	}
}
