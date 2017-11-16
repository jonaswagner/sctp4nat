package connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

import net.sctp4nat.connection.SctpChannel;
import net.sctp4nat.connection.SctpUtils;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpDataCallback;
import net.sctp4nat.core.SctpInitException;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.core.UdpClientLink;
import net.sctp4nat.origin.Sctp;

public class SctpChannelLargeDataTest {

	private static final int TIMEOUT = 30;
	private static final Logger LOG = LoggerFactory.getLogger(SctpChannelLargeDataTest.class);

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
							SctpChannelFacade so) {
						System.err.println("len: "+data.length+ "/ " + sid+" / "+ssn+" :tsn "+ tsn+ " F:"+flags+ " cxt:"+context);
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
				} catch (SctpInitException e) {
					fail(e.getMessage());
					e.printStackTrace();
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
		Thread client = new Thread(new Runnable() {

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
							SctpChannelFacade so) {
					}
				};

				SctpChannel channel = SctpChannel.builder().cb(cb).local(local).remote(remote).build();
				Promise<SctpChannelFacade, Exception, Object> p = null;
				try {
					p = channel.connect(null);
				} catch (Exception e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
				p.done(new DoneCallback<SctpChannelFacade>() {

					@Override
					public void onDone(SctpChannelFacade result) {
						result.send(new byte[(1024 * 900)-1], true, 0, 0);
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
	}
}
