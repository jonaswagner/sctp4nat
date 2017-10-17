package net.tomp2p.sctp.connection.sample;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;

import net.tomp2p.sctp.core.SctpChannelFacade;
import net.tomp2p.sctp.connection.SctpChannel;
import net.tomp2p.sctp.core.Sctp;
import net.tomp2p.sctp.core.SctpAdapter;
import net.tomp2p.sctp.core.SctpDataCallback;
import net.tomp2p.sctp.core.SctpPorts;
import net.tomp2p.sctp.core.UdpClientLink;

public class SimpleClient {

	public static void main(String[] args) throws Exception {
		Sctp.init();

		InetAddress localHost = Inet6Address.getByName("::1");
		InetSocketAddress local = new InetSocketAddress(localHost, SctpPorts.getInstance().generateDynPort());
		InetSocketAddress remote = new InetSocketAddress(localHost, SctpPorts.SCTP_TUNNELING_PORT);
		
		SctpDataCallback cb = new SctpDataCallback() {
			
			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpAdapter so) {
				System.out.println("I WAS HERE");
				System.out.println("got data: " + new String(data, StandardCharsets.UTF_8));
				System.out.println("Now closing channel");
//				so.close();
			}
		};

		SctpChannel channel = SctpChannel.builder().cb(cb).local(local).remote(remote).build();
		Promise<SctpChannelFacade, Exception, UdpClientLink> p = channel.connect();
		p.done(new DoneCallback<SctpChannelFacade>() {
			
			@Override
			public void onDone(SctpChannelFacade result) {
				int success = result.send("Hello World!".getBytes(), false, 0, 0);
				if (success > 0) {
					System.out.println("Message sent");
				} else {
					System.out.println("ERROR WHILE SENDING THE MESSAGE!");
				}
			}
		});
		
	}
}
