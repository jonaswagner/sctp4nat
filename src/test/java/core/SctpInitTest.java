package core;

import static org.junit.Assert.fail;

import java.io.IOException;
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

import net.sctp4nat.connection.SctpConnection;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;

public class SctpInitTest {

	private static final Logger LOG = LoggerFactory.getLogger(SctpInitTest.class);
	
	private InetSocketAddress serverAddr;
	private InetSocketAddress clientAddr;
	private SctpDataCallback cb;
	
	@Test
	public void testLauncher() throws Exception {
		testLaunchWithoutInit();
		testLaunch();
	}
	
	public void testLaunchWithoutInit() throws Exception {
		CountDownLatch errorCountDown = new CountDownLatch(1);
		
		try {
			serverAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 9899);
			clientAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 2000);
		} catch (UnknownHostException e1) {
			fail(e1.getMessage());
		}
		
		cb = new SctpDataCallback() {
			
			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpChannelFacade so) {
				LOG.debug("Ignored message: " + new String(data, StandardCharsets.UTF_8));
			}
		};
		
		SctpConnection channel = SctpConnection.builder().local(clientAddr).remote(serverAddr).build();
		Promise<SctpChannelFacade, Exception, Void> p = null;
		try{
			p = channel.connect(null);
		} catch (Exception e) {
			if(e instanceof SctpInitException && p == null) {
				LOG.error(e.getMessage());
				errorCountDown.countDown();
			}
		}
		
		errorCountDown.await(10, TimeUnit.SECONDS);
		if (errorCountDown.getCount() > 0) {
			fail("Not all errors reached");
		}
		
		CountDownLatch close = new CountDownLatch(1);
		Promise<Void, Exception, Void> promise = SctpUtils.shutdownAll();
		promise.done(new DoneCallback<Void>() {
			
			@Override
			public void onDone(Void result) {
				close.countDown();
			}
		});
		
		if (!close.await(10, TimeUnit.SECONDS)) {
			fail("Timeout called because close() could not finish");
		}
		Sctp.getInstance().finish();
	}
	
	public void testLaunch() throws InterruptedException, IOException {
		CountDownLatch errorCountDown = new CountDownLatch(2);
		
		try {
			serverAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 9899);
		} catch (UnknownHostException e1) {
			fail(e1.getMessage());
		}
		cb = new SctpDataCallback() {
			
			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpChannelFacade so) {
				LOG.debug("Ignored message: " + new String(data, StandardCharsets.UTF_8));
			}
		};
		
		try {
			SctpUtils.init(null, -1, null);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (SctpInitException e) {
			LOG.error(e.getMessage());
			errorCountDown.countDown();
		}
		
		try {
			SctpUtils.init(serverAddr.getAddress(), serverAddr.getPort(), cb);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (SctpInitException e) {
			LOG.error(e.getMessage());
			errorCountDown.countDown();
		}
		
		if (errorCountDown.getCount() > 0) {
			fail("Not all errors reached");
		}
		
		CountDownLatch close = new CountDownLatch(1);
		Promise<Void, Exception, Void> promise = SctpUtils.shutdownAll();
		promise.done(new DoneCallback<Void>() {
			
			@Override
			public void onDone(Void result) {
				close.countDown();
			}
		});
		
		if (!close.await(10, TimeUnit.SECONDS)) {
			fail("Timeout called because close() could not finish");
		}
		Sctp.getInstance().finish();
	}
	
	@After
	public void tearDown() throws InterruptedException, IOException {
		CountDownLatch close = new CountDownLatch(1);
		Promise<Void, Exception, Void> promise = SctpUtils.shutdownAll();
		promise.done(new DoneCallback<Void>() {
			
			@Override
			public void onDone(Void result) {
				close.countDown();
			}
		});
		
		if (!close.await(10, TimeUnit.SECONDS)) {
			fail("Timeout called because close() could not finish");
		}
		Sctp.getInstance().finish();
	}
	
}