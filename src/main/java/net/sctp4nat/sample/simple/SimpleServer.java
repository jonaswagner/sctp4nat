package net.sctp4nat.sample.simple;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import net.sctp4nat.connection.SctpDefaultStreamConfig;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.util.SctpUtils;

public class SimpleServer {
	
	public static void main(String[] args) throws Exception {
		InetAddress localHost = Inet6Address.getByName("::1");
		
		SctpDataCallback cb = new SctpDataCallback() {
			
			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpChannelFacade facade) {
				System.out.println("I WAS HERE");
				System.out.println("got data: " + new String(data, StandardCharsets.UTF_8));
				facade.send(data, new SctpDefaultStreamConfig());
			}
		};
		
		SctpUtils.init(localHost, SctpPorts.SCTP_TUNNELING_PORT, cb);
		
		System.out.println("Server ready!");
	}
}
