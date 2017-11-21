package net.sctp4nat.sample.simple;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;

import net.sctp4nat.connection.SctpConnection;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpDataCallback;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.core.UdpClientLink;
import net.sctp4nat.origin.Sctp;

public class SimpleClient {

	public static void main(String[] args) throws Exception {
		Sctp.init();

		InetAddress localHost = Inet6Address.getByName("::1");
		InetSocketAddress local = new InetSocketAddress(localHost, SctpPorts.getInstance().generateDynPort());
		InetSocketAddress remote = new InetSocketAddress(localHost, SctpPorts.SCTP_TUNNELING_PORT);
		
		SctpDataCallback cb = new SctpDataCallback() {
			
			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpChannelFacade so) {
				System.out.println("I WAS HERE");
				System.out.println("got data: " + new String(data, StandardCharsets.UTF_8));
				System.out.println("Now closing channel");
				so.close();
			}
		};

		SctpConnection channel = SctpConnection.builder().cb(cb).local(local).remote(remote).build();
		Promise<SctpChannelFacade, Exception, Object> p = channel.connect(null);
		p.done(new DoneCallback<SctpChannelFacade>() {
			
			@Override
			public void onDone(SctpChannelFacade result) {
				result.send("Hello World!".getBytes(), false, 0, 0);
			}
		});
		
	}
}
