package net.sctp4j.sample;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;

import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpMapper;
import net.sctp4j.core.SctpSocketBuilder;
import net.sctp4j.core.UdpClientLink;
import net.sctp4j.origin.Sctp;

public class SampleClient {

	public static void main(String[] args) throws IOException {

		Sctp.init();
		
		/*
		 * Usage: client remote_addr remote_port [local_port] [local_encaps_port] [remote_encaps_port]
		 */
		
		InetSocketAddress local = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 2000);
		InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 9899);
		int localSctpPort = 12345;
		SctpMapper mapper = new SctpMapper();

		SctpDataCallback cb = new SctpDataCallback() {

			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpAdapter so) {
				System.out.println("I WAS HERE");
			}
		};

		SctpAdapter so = new SctpSocketBuilder().
				localAddress(local.getAddress()).
				localPort(local.getPort()).
				localSctpPort(localSctpPort).
				remoteAddress(remote.getAddress()).
				remotePort(remote.getPort()).
				sctpDataCallBack(cb).
				mapper(mapper).
				build();
		
		UdpClientLink link = new UdpClientLink(local, remote, so);
		so.setLink(link);
		
		Promise<SctpAdapter, Exception, Object> p = so.connect(remote);
		
		p.done(new DoneCallback<SctpAdapter>() {
			
			@Override
			public void onDone(SctpAdapter result) {
				SctpUtils.getThreadPoolExecutor().execute(new Runnable() {
					
					@Override
					public void run() {
						
						try {
							Thread.sleep(2000); //wait for the connection setup
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						int success = result.send("Hello World!".getBytes(), false, 0, 0);
						if (success > 0) {
							System.out.println("Message sent");
						} else {
							System.out.println("ERROR WHILE SENDING THE MESSAGE!");
						}
					}
				});
			}
		});
		
		System.out.println("SETUP COMPLETE");
	}
}
