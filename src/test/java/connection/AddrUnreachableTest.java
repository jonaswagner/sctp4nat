package connection;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4j.connection.SctpChannel;
import net.sctp4j.connection.SctpDefaultConfig;
import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpChannelFacade;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpInitException;
import net.sctp4j.core.SctpPorts;
import net.sctp4j.core.UdpClientLink;
import net.sctp4j.origin.Sctp;
import net.sctp4j.origin.SctpNotification;
import net.sctp4j.origin.SctpSocket;
import net.sctp4j.origin.SctpSocket.NotificationListener;

public class AddrUnreachableTest {

	private static final Logger LOG = LoggerFactory.getLogger(AddrUnreachableTest.class);
	
	@Test
	public void lostConnectionTest() {

		Thread server;
		Thread client;

		CountDownLatch serverSetup = new CountDownLatch(1);
		CountDownLatch clientMessageExchange = new CountDownLatch(1);
		CountDownLatch addrUnreachable = new CountDownLatch(1);

		InetAddress localhostCandidate = null;
		try {
			localhostCandidate = Inet6Address.getByName("::1");
		} catch (UnknownHostException e1) {
			fail(e1.getMessage());
		}

		final InetAddress localhost = localhostCandidate;

		server = new Thread(new Runnable() {

			@Override
			public void run() {

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
						
						try {
							clientMessageExchange.wait(5000l);
							SctpUtils.getLink().close(); //fake crash without 
							facade.close(); 
						} catch (InterruptedException e) {
							fail(e.getMessage());
						}
					}
				};

				try {
					SctpUtils.init(localhost, SctpPorts.SCTP_TUNNELING_PORT, cb);
				} catch (SocketException | SctpInitException e) {
					fail(e.getMessage());
				}

				System.out.println("Server ready!");
				serverSetup.countDown();
				
			}
		});

		client = new Thread(new Runnable() {

			@Override
			public void run() {
				Sctp.init();

				InetSocketAddress local = new InetSocketAddress(localhost, SctpPorts.getInstance().generateDynPort());
				InetSocketAddress remote = new InetSocketAddress(localhost, SctpPorts.SCTP_TUNNELING_PORT);

				SctpDataCallback cb = new SctpDataCallback() {

					@Override
					public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
							SctpAdapter so) {
						System.out.println("I WAS HERE");
						System.out.println("got data: " + new String(data, StandardCharsets.UTF_8));
						
						clientMessageExchange.countDown();
						
						so.setNotificationListener(new NotificationListener() {
							
							@Override
							public void onSctpNotification(SctpSocket socket, SctpNotification notification) {
								if (notification.toString().indexOf("ADDR_UNREACHABLE") >= 0){
									LOG.error("Heartbeat missing! Now shutting down the SCTP connection...");
									try {
										so.close().wait(SctpUtils.SHUTDOWN_TIMEOUT);
										addrUnreachable.countDown();
									} catch (InterruptedException e) {
										LOG.error(e.getMessage(), e);
									}
								} else {
									LOG.debug(notification.toString());
								}
							}
						});
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
		});

		server.start();

		try {
			serverSetup.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}

		client.start();
		try {
			addrUnreachable.await(270, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
		
		assertTrue(addrUnreachable.getCount()==0);		
	}
	
	@After
	public void tearDown() throws IOException {
		Sctp.finish();
	}
}
