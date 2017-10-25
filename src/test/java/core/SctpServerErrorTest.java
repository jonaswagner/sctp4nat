package core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4j.connection.SctpChannel;
import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.SctpChannelFacade;
import net.sctp4j.core.SctpInitException;
import net.sctp4j.core.SctpPorts;
import net.sctp4j.core.UdpClientLink;
import net.sctp4j.origin.Sctp;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpInitException;
import net.sctp4j.core.SctpPorts;

public class SctpServerErrorTest {
	
	private static final String TEST_STR = "Hello World!";
	private final CountDownLatch clientLatch = new CountDownLatch(1);

	private static final Logger LOG = LoggerFactory.getLogger(SctpTest.class);

	Thread server;
	Thread client;
	InetAddress localhost;

	@Before
	public void setUp() throws UnknownHostException {
		localhost = InetAddress.getByName("127.0.0.1");
	}
	
	@Test
	public void clientError() {
		final InetSocketAddress serverAddr = new InetSocketAddress(localhost, SctpPorts.SCTP_TUNNELING_PORT);
		final InetSocketAddress clientAddr = new InetSocketAddress(localhost, 2000);
		
		
		/**
		 * CONTINUE HERE WITH THE SERVER THREAD, which should not ANSWER because it has errors!!!!!!
		 */
		
		
		client = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				SctpDataCallback cb = new SctpDataCallback() {
					
					@Override
					public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
							SctpAdapter so) {
						// TODO Auto-generated method stub
					}
				};
				
				try {
					SctpUtils.init(clientAddr.getAddress(), clientAddr.getPort(), cb);
				} catch (SocketException e) {
					fail(e.getMessage());
				} catch (SctpInitException e) {
					fail(e.getMessage());
				}
				
				SctpChannel channel = SctpChannel.builder().local(clientAddr).remote(serverAddr).cb(cb).build();
				Promise<SctpChannelFacade, Exception, UdpClientLink> p = channel.connect();
				
				p.done(new DoneCallback<SctpChannelFacade>() {

					@Override
					public void onDone(SctpChannelFacade result) {
						fail("We should not get here!!!");
					}
				});
				
				p.fail(new FailCallback<Exception>() {

					@Override
					public void onFail(Exception result) {
						LOG.error(result.getMessage());
						assertTrue(result instanceof TimeoutException);
						clientLatch.countDown();
					}
				});
			}
		});
		
		client.start();
	}
}
