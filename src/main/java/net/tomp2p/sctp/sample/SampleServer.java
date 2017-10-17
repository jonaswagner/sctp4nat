package net.tomp2p.sctp.sample;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import net.tomp2p.sctp.core.Sctp;
import net.tomp2p.sctp.core.SctpAdapter;
import net.tomp2p.sctp.core.SctpDataCallback;
import net.tomp2p.sctp.core.SctpMapper;
import net.tomp2p.sctp.core.UdpServerLink;

public class SampleServer {

	public static void main(String[] args) throws UnknownHostException, SocketException {

		Sctp.init();
		
		/*
		 * Usage: echo_server [local_encaps_port] [remote_encaps_port]
		 * 
		 * Example
		 * Server: $ ./echo_server 11111 22222
		 * Client: $ ./client 127.0.0.1 7 0 22222 11111
		 */
		
		InetSocketAddress local = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 9899);
		
		SctpMapper mapper = new SctpMapper();
		
		SctpDataCallback cb = new SctpDataCallback() {
			
			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpAdapter so) {
				System.out.println("I WAS HERE");
				System.out.println("got data: " + new String(data, StandardCharsets.UTF_8));
				so.send(data, false, sid, (int) ppid);
			}
		};
		
		UdpServerLink link = new UdpServerLink(mapper, local.getAddress(), cb);
		
		System.out.println("SETUP COMPLETE");
	}
}