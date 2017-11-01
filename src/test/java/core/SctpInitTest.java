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
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4j.connection.SctpChannel;
import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.SctpChannelFacade;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpInitException;
import net.sctp4j.core.UdpClientLink;
import net.sctp4j.origin.Sctp;

public class SctpInitTest {

	private static final Logger LOG = LoggerFactory.getLogger(SctpInitTest.class);
	
	private InetSocketAddress serverAddr;
	private InetSocketAddress clientAddr;
	private SctpDataCallback cb;
	
	@Test
	public void testAll() throws InterruptedException {
		testLaunchWithoutInit();
		testLaunch();
	}
	
	public void testLaunch() {
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
	}
	
	private void testLaunchWithoutInit() throws InterruptedException {
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
		
		SctpChannel channel = SctpChannel.builder().cb(cb).local(clientAddr).remote(serverAddr).build();
		Promise<SctpChannelFacade, Exception, UdpClientLink> p = channel.connect();
		p.done(new DoneCallback<SctpChannelFacade>() {
			
			@Override
			public void onDone(SctpChannelFacade result) {
				result.send("Dummy".getBytes(), false, 0, 0);
			}
		});
		
		p.fail(new FailCallback<Exception>() {
			
			@Override
			public void onFail(Exception result) {
				LOG.error(result.getMessage());
				errorCountDown.countDown();
			}
		});
		
		errorCountDown.await(10, TimeUnit.SECONDS);
		if (errorCountDown.getCount() > 0) {
			fail("Not all errors reached");
		}
	}
	
	@After
	public void tearDown() throws InterruptedException, IOException {
		Sctp.finish();
	}
	
}