package core;

import static org.junit.Assert.*;

import java.io.IOException;
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
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpInitException;
import net.sctp4j.core.SctpMapper;
import net.sctp4j.core.SctpSocketBuilder;
import net.sctp4j.core.UdpClientLink;
import net.sctp4j.core.UdpServerLink;
import net.sctp4j.origin.Sctp;

public class SctpTest {

	private static final String TEST_STR = "Hello World!";

	private static final Logger LOG = LoggerFactory.getLogger(SctpTest.class);

	Thread server;
	Thread client;
	InetAddress localhost;

	@Before
	public void setUp() throws UnknownHostException {
		localhost = InetAddress.getByName("127.0.0.1");
	}

	@Test
	public void sctpTransmissionTest() throws InterruptedException {
		CountDownLatch serverCd = new CountDownLatch(1);
		CountDownLatch clientCd = new CountDownLatch(1);
		CountDownLatch comCd = new CountDownLatch(2);
		CountDownLatch shutdownCd = new CountDownLatch(3);

		server = new Thread(new Runnable() {
			public void run() {
				Sctp.init();

				InetSocketAddress local = new InetSocketAddress(localhost, 9899);

				SctpMapper mapper = new SctpMapper();

				SctpDataCallback cb = new SctpDataCallback() {

					@Override
					public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
							SctpAdapter so) {
						LOG.debug("SERVER GOT DATA: " + new String(data, StandardCharsets.UTF_8));
						assertEquals(TEST_STR, new String(data, StandardCharsets.UTF_8));
						so.send(data, 0, data.length, false, sid, (int) ppid);
						so.send(data, false, sid, (int) ppid);
						comCd.countDown();

						Promise<Object, Exception, Object> p2= so.close();
						p2.done(new DoneCallback<Object>() {

							@Override
							public void onDone(Object result) {
								shutdownCd.countDown();
							}
						});
						
						
						Promise<Object, Exception, Object> p = SctpUtils.shutdownAll(null, null);

						p.done(new DoneCallback<Object>() {

							@Override
							public void onDone(Object result) {
								shutdownCd.countDown();
							}
						});
					}
				};

				UdpServerLink link = null;
				try {
					link = new UdpServerLink(mapper, local.getAddress(), cb);
				} catch (SocketException e) {
					e.printStackTrace();
				}

				LOG.debug("SERVER SETUP COMPLETE");
				serverCd.countDown();
			}
		});

		client = new Thread(new Runnable() {

			public void run() {

				Sctp.init();

				try {
					serverCd.await();
				} catch (InterruptedException e2) {
					e2.printStackTrace();
				}

				InetSocketAddress local = new InetSocketAddress(localhost, 1234);
				InetSocketAddress remote = new InetSocketAddress(localhost, 9899);
				int localSctpPort = 12345;
				SctpMapper mapper = new SctpMapper();

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

				SctpAdapter so = null;
				try {
					so = new SctpSocketBuilder().localAddress(local.getAddress()).localPort(local.getPort())
							.localSctpPort(localSctpPort).remoteAddress(remote.getAddress()).remotePort(remote.getPort())
							.sctpDataCallBack(cb).mapper(mapper).build();
				} catch (SctpInitException e2) {
					e2.printStackTrace();
				}

				UdpClientLink link = null;
				try {
					link = new UdpClientLink(local, remote, so);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				so.setLink(link);

				Promise<SctpAdapter, Exception, Object> p = so.connect(remote);

				p.done(new DoneCallback<SctpAdapter>() {

					@Override
					public void onDone(SctpAdapter result) {
						SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

							@Override
							public void run() {

								clientCd.countDown();

								try {
									Thread.sleep(2000); // wait for the connection setup
								} catch (InterruptedException e) {
									e.printStackTrace();
								}

								result.send(TEST_STR.getBytes(), false, 0, 0);
							}
						});
					}
				});

				p.fail(new FailCallback<Exception>() {

					@Override
					public void onFail(Exception result) {
						LOG.error("CLIENT SETUP FAILED");
						LOG.error("Cause: " + result.getMessage());
						result.printStackTrace();
					}
				});

				LOG.debug("CLIENT SETUP COMPLETE");
			}
		});

		server.run();
		client.run();

		comCd.await(10, TimeUnit.SECONDS);

		if (comCd.getCount() > 0) {
			fail("communication error");
		}

		shutdownCd.await(10, TimeUnit.SECONDS);
		if (shutdownCd.getCount() > 0) {
			fail("shutdown could not complete");
		}
	}
	
}