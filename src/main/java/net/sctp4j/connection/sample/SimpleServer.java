package net.sctp4j.connection.sample;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import net.sctp4j.connection.SctpDefaultConfig;
import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.SctpChannelFacade;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpPorts;

public class SimpleServer {
	
	public static void main(String[] args) throws Exception {
		InetAddress localHost = Inet6Address.getByName("::1");
		SctpDefaultConfig config = new SctpDefaultConfig();
		
		SctpDataCallback cb = new SctpDataCallback() {
			
			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpChannelFacade so) {
				System.out.println("I WAS HERE");
				System.out.println("got data: " + new String(data, StandardCharsets.UTF_8));
				try {
					Thread.sleep(config.getConnectPeriodMillis());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				so.send(data, false, sid, (int) ppid);
			}
		};
		
		SctpUtils.init(localHost, SctpPorts.SCTP_TUNNELING_PORT, cb);
		
		System.out.println("Server ready!");
	}
}
