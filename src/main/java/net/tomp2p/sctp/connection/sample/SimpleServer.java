package net.tomp2p.sctp.connection.sample;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import net.tomp2p.sctp.core.SctpChannelFacade;
import net.tomp2p.sctp.connection.SctpDefaultConfig;
import net.tomp2p.sctp.connection.SctpUtils;
import net.tomp2p.sctp.core.SctpAdapter;
import net.tomp2p.sctp.core.SctpDataCallback;
import net.tomp2p.sctp.core.SctpPorts;

public class SimpleServer {
	
	public static void main(String[] args) throws Exception {
		InetAddress localHost = Inet6Address.getByName("::1");
		SctpDefaultConfig config = new SctpDefaultConfig();
		
		SctpDataCallback cb = new SctpDataCallback() {
			
			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpAdapter so) {
				SctpChannelFacade facade = (SctpChannelFacade) so;
				System.out.println("I WAS HERE");
				System.out.println("got data: " + new String(data, StandardCharsets.UTF_8));
				try {
					Thread.sleep(config.getConnectPeriodMillis());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				facade.send(data, false, sid, (int) ppid);
			}
		};
		
		SctpUtils.init(localHost, SctpPorts.SCTP_TUNNELING_PORT, cb);
		
		System.out.println("Server ready!");
	}
}
