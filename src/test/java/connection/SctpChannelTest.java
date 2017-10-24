package connection;

import static org.junit.Assert.*;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4j.connection.SctpChannel;
import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpChannelFacade;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpPorts;
import net.sctp4j.core.UdpClientLink;
import net.sctp4j.origin.Sctp;

public class SctpChannelTest {

	private static final int TIMEOUT = 30;
	private static final String TEST_STR = "Hello World!";
	private static final Logger LOG = LoggerFactory.getLogger(SctpChannelTest.class);

	
	@Test
	public void sctpChannelTest() throws InterruptedException {
		
		CountDownLatch serverCd = new CountDownLatch(1);
		CountDownLatch clientCd = new CountDownLatch(1);
		CountDownLatch comCd = new CountDownLatch(2);
		CountDownLatch shutdownCd = new CountDownLatch(2);
		
		/**
		 * This is the server Thread
		 */
		Thread server = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				SctpDataCallback cb = new SctpDataCallback() {
					
					@Override
					public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
							SctpAdapter so) {
						LOG.debug("SERVER GOT DATA: " + new String(data, StandardCharsets.UTF_8));
						assertEquals(TEST_STR, new String(data, StandardCharsets.UTF_8));
						so.send(data, 0, data.length, false, sid, (int) ppid);
						comCd.countDown();
						
						Promise<Object, Exception, Object> p = SctpUtils.shutdownAll(null, null);
						p.done(new DoneCallback<Object>() {

							@Override
							public void onDone(Object result) {
								try {
									comCd.await(TIMEOUT, TimeUnit.SECONDS);
								} catch (InterruptedException e) {
									fail();
								}
								shutdownCd.countDown();
							}
						});
						p.fail(new FailCallback<Exception>() {

							@Override
							public void onFail(Exception result) {
								fail();
							}
						});						
					}
				};
				
				int wrongPort = -300;
				try {
					SctpUtils.init(InetAddress.getByName("127.0.0.1"), wrongPort, cb);
				} catch (SocketException e) {
					e.printStackTrace();
					fail("Could not init server");
				} catch (UnknownHostException e) {
					fail("Could not init server");
					e.printStackTrace();
				}
				
				assertEquals(SctpPorts.SCTP_TUNNELING_PORT, SctpUtils.getLink().getPort());
			
				serverCd.countDown();
			}
		});
		
		
		/**
		 * This is the client Thread
		 */
		Thread client  = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Sctp.init();
				
				InetAddress localHost = null;
				try {
					localHost = Inet6Address.getByName("127.0.0.1");
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				InetSocketAddress local = new InetSocketAddress(localHost, SctpPorts.getInstance().generateDynPort());
				InetSocketAddress remote = new InetSocketAddress(localHost, SctpPorts.SCTP_TUNNELING_PORT);
				
				SctpDataCallback cb = new SctpDataCallback() {
					
					@Override
					public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
							SctpAdapter so) {
						LOG.debug("REPLY SUCCESS");
						assertEquals(TEST_STR, new String(data, StandardCharsets.UTF_8));
						comCd.countDown();
						Promise<Object, Exception, Object> p = so.close();
						p.done(new DoneCallback<Object>() {

							@Override
							public void onDone(Object result) {
								shutdownCd.countDown();
							}
						});
					}
				};

				SctpChannel channel = SctpChannel.builder().cb(cb).local(local).remote(remote).build();
				Promise<SctpChannelFacade, Exception, UdpClientLink> p = channel.connect();
				p.done(new DoneCallback<SctpChannelFacade>() {
					
					@Override
					public void onDone(SctpChannelFacade result) {
						int success = result.send("Hello World!".getBytes(), false, 0, 0);
						if (success > 0) {
							LOG.debug("Message sent");
						} else {
							LOG.error("ERROR WHILE SENDING THE MESSAGE!");
						}
					}
				});
				
				clientCd.countDown();
			}
		});
		
		server.run();
		client.run();

		comCd.await(TIMEOUT, TimeUnit.SECONDS);
		
		if (comCd.getCount() > 0) {
			fail("communication error");
		}
		
		shutdownCd.await(TIMEOUT, TimeUnit.SECONDS);
		if (shutdownCd.getCount() > 0) {
			fail("shutdown could not complete");
		}
	}
}
