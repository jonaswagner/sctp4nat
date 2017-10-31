package net.sctp4j.connection.sample;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4j.core.SctpInitException;
import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpDataCallback;

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
					int flags, SctpAdapter so) {
				LOG.debug("SERVER GOT SCTP DATA: " + new String(data, StandardCharsets.UTF_8));
				so.send(data, 0, data.length, false, sid, (int) ppid);
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
