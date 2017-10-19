package connection;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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

import net.sctp4j.connection.SctpDefaultConfig;
import net.sctp4j.connection.SctpUtils;
import net.sctp4j.connection.UpgradeableUdpSocket;
import net.sctp4j.core.NetworkLink;
import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpChannelFacade;
import net.sctp4j.core.SctpDataCallback;

public class UdpUpgradeTest {

	private static final Logger LOG = LoggerFactory.getLogger(UdpUpgradeTest.class);

	Thread server;
	Thread client;
	InetSocketAddress serverSoAddr;
	InetSocketAddress clientSoAddr;

	public static final String TEST_STR = "HELLO WORLD!";

	@Before
	public void setUp() throws Exception {
		serverSoAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 1000);
		clientSoAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 2000);
	}

	@Test
	public void testUpgrade() throws Exception {
		final CountDownLatch clientSetup = new CountDownLatch(1);
		final CountDownLatch serverSetup = new CountDownLatch(1);
		final CountDownLatch udpCom = new CountDownLatch(1);
		final CountDownLatch sctpCom = new CountDownLatch(2);

		server = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					SctpUtils.init(serverSoAddr.getAddress(), serverSoAddr.getPort(), null);
					UpgradeableUdpSocket udpSocket = new UpgradeableUdpSocket(serverSoAddr.getPort(),
							serverSoAddr.getAddress());
					udpSocket.setCb(new SctpDataCallback() {

						@Override
						public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context,
								int flags, SctpAdapter so) {
							LOG.debug("SERVER GOT DATA: " + new String(data, StandardCharsets.UTF_8));
							assertEquals(TEST_STR, new String(data, StandardCharsets.UTF_8));
							so.send(data, 0, data.length, false, sid, (int) ppid);
							sctpCom.countDown();
						}
					});
					new Thread(new Runnable() {

						@Override
						public void run() {
							while (!udpSocket.isUgrading()) {
								byte[] buff = new byte[2048];
								DatagramPacket packet = new DatagramPacket(buff, 2048);
								try {
									udpSocket.receive(packet);
								} catch (IOException e) {
									e.printStackTrace();
									fail(e.getMessage());
								}
								String current = new String(packet.getData(), StandardCharsets.UTF_8);
								current = current.trim();
								if (TEST_STR.equals(current)) {
									LOG.debug("Server ready");
									udpCom.countDown();
								}
							}
						}
					}).start();

				} catch (Exception e) {
					e.printStackTrace();
					fail(e.getMessage());
				}

				serverSetup.countDown();
			}

		});

		client = new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					SctpUtils.init(clientSoAddr.getAddress(), clientSoAddr.getPort(), null);
					UpgradeableUdpSocket udpSocket = new UpgradeableUdpSocket(clientSoAddr.getPort(),
							clientSoAddr.getAddress());
					udpSocket.setCb(new SctpDataCallback() {

						@Override
						public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context,
								int flags, SctpAdapter so) {

							LOG.debug("REPLY SUCCESS");
							assertEquals(TEST_STR, new String(data, StandardCharsets.UTF_8));
							sctpCom.countDown();
						}
					});

					clientSetup.countDown();
					LOG.error("client ready");

					DatagramPacket testPacket = new DatagramPacket(TEST_STR.getBytes(), TEST_STR.length());
					testPacket.setAddress(serverSoAddr.getAddress());
					testPacket.setPort(serverSoAddr.getPort());
					udpSocket.send(testPacket);

					if (!udpCom.await(10, TimeUnit.SECONDS)) {
						fail("timeout");
					} else {
						SctpDefaultConfig config = new SctpDefaultConfig();
						Promise<SctpChannelFacade, Exception, NetworkLink> promise = udpSocket.upgrade(config,
								clientSoAddr, serverSoAddr);

						promise.done(new DoneCallback<SctpChannelFacade>() {

							@Override
							public void onDone(SctpChannelFacade result) {
								result.send(TEST_STR.getBytes(), false, 0, 0);
							}
						});

						promise.fail(new FailCallback<Exception>() {

							@Override
							public void onFail(Exception result) {
								result.printStackTrace();
								fail(result.getMessage());
							}
						});
					}

				} catch (Exception e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
			}

		});

		server.start();

		if (!serverSetup.await(10, TimeUnit.SECONDS)) {
			fail("timeout");
		} else {
			client.start();
		}

		if (!udpCom.await(10, TimeUnit.SECONDS)) {
			fail("timeout");
		} else {

		}

		sctpCom.await(10000, TimeUnit.SECONDS);
		if (sctpCom.getCount() > 0) {
			fail("timeout");
		}
	}
}
