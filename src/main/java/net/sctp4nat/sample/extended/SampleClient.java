package net.sctp4nat.sample.extended;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.connection.UdpClientLink;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.core.NetworkLink;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelBuilder;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;

public class SampleClient {

	private static final Logger LOG = LoggerFactory.getLogger(SampleClient.class);
	
	public static void main(String[] args) throws IOException, SctpInitException {

		Sctp.getInstance().init();
		
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
					SctpChannelFacade facade) {
				System.out.println("I WAS HERE");
				SctpChannel realSo = (SctpChannel) facade;
				realSo.shutdownInit();
				realSo.close();
			}
		};

		SctpChannel so = new SctpChannelBuilder().
				localSctpPort(localSctpPort).
				remoteAddress(remote.getAddress()).
				remotePort(remote.getPort()).
				sctpDataCallBack(cb).
				mapper(mapper).
				build();
		
		NetworkLink link = new UdpClientLink(local, remote, so);
		so.setLink(link);
		
		Promise<SctpChannelFacade, Exception, Void> p = so.connect(remote);
		
		p.done(new DoneCallback<SctpChannelFacade>() {
			
			@Override
			public void onDone(SctpChannelFacade result) {
				SctpUtils.getThreadPoolExecutor().execute(new Runnable() {
					
					@Override
					public void run() {
						
						try {
							Thread.sleep(2000); //wait for the connection setup
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						result.send("Hello World!".getBytes(), false, 0, 0);
					}
				});
			}
		});
		
		p.fail(new FailCallback<Exception>() {
			
			@Override
			public void onFail(Exception result) {
				LOG.error(result.getMessage());
				so.close();
			}
		});
		
		System.out.println("SETUP COMPLETE");
	}
}
