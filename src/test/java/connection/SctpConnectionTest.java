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

import net.sctp4nat.connection.SctpConnection;
import net.sctp4nat.connection.SctpDefaultStreamConfig;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;

public class SctpConnectionTest {

	private static final int TIMEOUT = 30;
	private static final String TEST_STR = "Hello World!";
	private static final Logger LOG = LoggerFactory.getLogger(SctpConnectionTest.class);

	Thread server;
	Thread client;

	@Test
	public void sctpChannelTest() throws InterruptedException {

		SctpMapper.setShutdown(false);

		CountDownLatch serverCd = new CountDownLatch(1);
		CountDownLatch clientCd = new CountDownLatch(1);
		CountDownLatch comCd = new CountDownLatch(2);
		CountDownLatch shutdownCd = new CountDownLatch(2);

		/**
		 * This is the server Thread
		 */
		server = new Thread(new Runnable() {

			@Override
			public void run() {

				SctpDataCallback cb = new SctpDataCallback() {

					@Override
					public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
							SctpChannelFacade so) {
						LOG.debug("SERVER GOT DATA: " + new String(data, StandardCharsets.UTF_8));
						assertEquals(TEST_STR, new String(data, StandardCharsets.UTF_8));
						so.send(data, 0, data.length, false, sid, (int) ppid);
						comCd.countDown();

						Promise<Void, Exception, Void> p = SctpUtils.shutdownAll(null, null);
						p.done(new DoneCallback<Void>() {

							@Override
							public void onDone(Void result) {
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

				serverCd.countDown();
			}
		});

		/**
		 * This is the client Thread
		 */
		client = new Thread(new Runnable() {

			@Override
			public void run() {
				Sctp.getInstance().init();

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

				SctpConnection channel = SctpConnection.builder().local(local).remote(remote).build();
				Promise<SctpChannelFacade, Exception, Void> p = null;
				try {
					p = channel.connect(null);
				} catch (Exception e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
				p.done(new DoneCallback<SctpChannelFacade>() {

					@Override
					public void onDone(SctpChannelFacade result) {
						result.setSctpDataCallback(cb);
						result.send("Hello World!".getBytes(), false, 0, 0);
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

		server.interrupt();
		client.interrupt();
		SctpUtils.shutdownAll();
	}

	@Test
	public void testSctpConnection() throws Exception {

		CountDownLatch latch = new CountDownLatch(3);
		try {
			SctpConnection.builder().build().connect(null);
		} catch (Exception e) {
			latch.countDown();
		}

		InetSocketAddress remote = new InetSocketAddress("localhost", 6597);
		try {
			SctpConnection.builder().remote(remote).build().connect(null);
		} catch (Exception e) {
			latch.countDown();
		}

		try {
			SctpConnection.builder().remote(remote).config(new SctpDefaultStreamConfig()).local(remote)
					.localSctpPort(3000).build().connect(null);
		} catch (Exception e) {
			latch.countDown();
		}

		if (latch.getCount() > 0) {
			fail("test contained errors");
		}
	}
}
