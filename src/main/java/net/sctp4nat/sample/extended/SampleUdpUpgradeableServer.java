package net.sctp4nat.sample.extended;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;

public class SampleUdpUpgradeableServer {

	private static final Logger LOG = LoggerFactory.getLogger(SampleUdpUpgradeableServer.class);
	
	public static void main(String[] args) throws UnknownHostException, SocketException, SctpInitException {
		
		InetSocketAddress serverSoAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 5689);
		
		SctpUtils.init(serverSoAddr.getAddress(), serverSoAddr.getPort(), null);
		
		UpgradeableUdpSocketSample udpSocket = new UpgradeableUdpSocketSample(serverSoAddr.getPort(),
				serverSoAddr.getAddress());
		udpSocket.setCb(new SctpDataCallback() {

			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context,
					int flags, SctpChannelFacade facade) {
				LOG.debug("SERVER GOT SCTP DATA: " + new String(data, StandardCharsets.UTF_8));
				facade.send(data, 0, data.length, false, sid, (int) ppid);
			}
		});
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (!udpSocket.isUgrading()) {
					byte[] buff = new byte[2048];
					DatagramPacket packet = new DatagramPacket(buff, 2048);
					try {
						udpSocket.receive(packet);
						LOG.debug("SERVER GOT UDP DATA: " + new String(packet.getData(), StandardCharsets.UTF_8));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
}
