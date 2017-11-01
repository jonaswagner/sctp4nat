package core;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

public class SctpConnectTest {

	private static final Logger LOG = LoggerFactory.getLogger(SctpConnectTest.class);
	
	InetSocketAddress serverAddr;
	InetSocketAddress clientAddr;
	static final String LOCALHOST = "127.0.0.1";
	
	@Before
	public void setup() {
		try {
			serverAddr = new InetSocketAddress(InetAddress.getByName(LOCALHOST), SctpPorts.getInstance().generateDynPort());
			clientAddr = new InetSocketAddress(InetAddress.getByName(LOCALHOST), SctpPorts.getInstance().generateDynPort());
		} catch (UnknownHostException e1) {
			fail(e1.getMessage());
		}
	}
	
	@After
	public void tearDown() throws IOException {
		Sctp.finish();
	}
	
	@Test
	public void connectInitTest() throws InterruptedException {
		
		CountDownLatch latch = new CountDownLatch(1);
		
		Promise<SctpChannelFacade, Exception, UdpClientLink> p = SctpChannel.builder().local(clientAddr).remote(serverAddr).build().connect();
		p.done(new DoneCallback<SctpChannelFacade>() {
			
			@Override
			public void onDone(SctpChannelFacade result) {
				fail("we should not end up here! This test should test the failures");
			}
		});
		
		p.fail(new FailCallback<Exception>() {
			
			@Override
			public void onFail(Exception result) {
				LOG.error(result.getMessage());
				latch.countDown();
			}
		});
		
		latch.await(10, TimeUnit.SECONDS);
		assertTrue(latch.getCount() == 0);
	}
	
	@Test
	public void connectInitTest2() throws InterruptedException, SocketException, SctpInitException {
		SctpUtils.init(clientAddr.getAddress(), SctpPorts.getInstance().generateDynPort(), null);
		CountDownLatch latch = new CountDownLatch(1);
		
		Promise<SctpChannelFacade, Exception, UdpClientLink> p = SctpChannel.builder().local(clientAddr).remote(serverAddr).build().connect();
		p.done(new DoneCallback<SctpChannelFacade>() {
			
			@Override
			public void onDone(SctpChannelFacade result) {
				fail("we should not end up here! This test should test the failures");
			}
		});
		
		p.fail(new FailCallback<Exception>() {
			
			@Override
			public void onFail(Exception result) {
				LOG.error(result.getMessage());
				assertTrue(result instanceof TimeoutException);
				latch.countDown();
			}
		});
		
		latch.await(10, TimeUnit.SECONDS);
		assertTrue(latch.getCount() == 0);
	}
}
