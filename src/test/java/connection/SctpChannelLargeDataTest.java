package connection;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.junit.After;
import org.junit.Test;

import net.sctp4nat.connection.SctpConnection;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;

/**
 * File payload test
 * 
 * 12 Byte SCTP Common Header 
 * + 4 Byte SCTP Chunk Header 
 * + 8 Byte UDP Header 
 * + 1 Byte SCTP Flag on UDP Packets 
 * ------------------------------------- 
 * = 25
 * Bytes Overhead
 * 
 * 
 * 981 + 25 = 1006 1024 - 1006 = z --> where are those 18 Bytes?
 * 
 * @author root
 *
 */
public class SctpChannelLargeDataTest {

	SctpChannelFacade serverSo;
	SctpChannelFacade clientSo;
	Thread server;
	Thread client;

	@Test
	public void sctpChannelTest() throws InterruptedException {

		CountDownLatch serverCd = new CountDownLatch(1);
		CountDownLatch clientCd = new CountDownLatch(1);
		CountDownLatch shutdownCd = new CountDownLatch(1);

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
						System.err.println("len: " + data.length + "/ " + sid + " / " + ssn + " :tsn " + tsn + " F:"
								+ flags + " cxt:" + context);
						if (flags == 128) {
							SctpChannelLargeDataTest.this.serverSo = so;
							shutdownCd.countDown();
						}
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
						try {
							if (!clientCd.await(10, TimeUnit.SECONDS)) {
								fail("Clientsetup failed!");
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
							fail(e.getMessage());
						}

						result.send(new byte[512 * 1024], true, 2, 0); //just works
//						result.send(new byte[(1024 * 1024)- 16156], true, 5, 0); //this works --> if the transmissions are sent too fast, the system will 
						// result.send(new byte[982 * 1024], true, 1, 0); this does not work!, there are
						// only 10 streams available per association

						SctpChannelLargeDataTest.this.clientSo = result;
					}
				});
				p.fail(new FailCallback<Exception>() {
					
					@Override
					public void onFail(Exception result) {
						fail(result.getMessage());
					}
				});

				clientCd.countDown();
			}
		});

		server.run();
		if (!serverCd.await(3, TimeUnit.SECONDS)) {
			fail("Serversetup failed!");
		}
		
		client.run();

		if (!shutdownCd.await(20, TimeUnit.SECONDS)) {
			fail("Timeout, --> Files were not transmitted");
		}

		Thread.sleep(1000); //wait for SCTP_SENDER_DRY_EVENT

	}

	@After
	public void tearDown() throws InterruptedException, IOException {
		CountDownLatch close = new CountDownLatch(1);
		
		Promise<Object, Exception, Object> closePromise1 = serverSo.close();
		closePromise1.done(new DoneCallback<Object>() {

			@Override
			public void onDone(Object result) {
				Promise<Object, Exception, Object> closePromise2 = clientSo.close();
				closePromise2.done(new DoneCallback<Object>() {

					@Override
					public void onDone(Object result) {
						server.interrupt();
						client.interrupt();
						SctpPorts.shutdown();
							close.countDown();
						}
				});
			}
		});
		if (!close.await(5, TimeUnit.SECONDS)) {
			fail("Shutdown Timeout called!");
		}
		Sctp.getInstance().finish();
	}
	
}