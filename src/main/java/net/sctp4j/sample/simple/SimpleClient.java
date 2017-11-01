package net.sctp4j.sample.simple;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;

import net.sctp4j.connection.SctpChannel;
import net.sctp4j.core.SctpChannelFacade;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpPorts;
import net.sctp4j.core.UdpClientLink;
import net.sctp4j.origin.Sctp;

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

		SctpChannel channel = SctpChannel.builder().cb(cb).local(local).remote(remote).build();
		Promise<SctpChannelFacade, Exception, UdpClientLink> p = channel.connect();
		p.done(new DoneCallback<SctpChannelFacade>() {
			
			@Override
			public void onDone(SctpChannelFacade result) {
				result.send("Hello World!".getBytes(), false, 0, 0);
			}
		});
		
	}
}
