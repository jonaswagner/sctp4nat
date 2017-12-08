package connection;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.connection.SctpConnection;
import net.sctp4nat.connection.SctpDefaultStreamConfig;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpSocket;
import net.sctp4nat.util.SctpUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SctpChannel.class)
public class SctpConnectTest {

	private static final Logger LOG = LoggerFactory.getLogger(SctpConnectTest.class);

	InetSocketAddress serverAddr;
	InetSocketAddress clientAddr;
	static final String LOCALHOST = "127.0.0.1";

	@Before
	public void setup() {
		try {
			Thread.sleep(2000); //wait if some other test shuts down usrsctp
			
			serverAddr = new InetSocketAddress(InetAddress.getByName(LOCALHOST),
					SctpPorts.getInstance().generateDynPort());
			clientAddr = new InetSocketAddress(InetAddress.getByName(LOCALHOST),
					SctpPorts.getInstance().generateDynPort());
		} catch (UnknownHostException | InterruptedException e1) {
			fail(e1.getMessage());
		}
	}

	@Test
	public void connectInitTest2() throws Exception {
		Sctp.getInstance().init();
		CountDownLatch latch = new CountDownLatch(1);

		Promise<SctpChannelFacade, Exception, Void> p = SctpConnection.builder().local(clientAddr).remote(serverAddr)
				.build().connect(null);
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

		CountDownLatch close = new CountDownLatch(1);
		Promise<Void, Exception, Void> promise = SctpUtils.shutdownAll();
		promise.done(new DoneCallback<Void>() {

			@Override
			public void onDone(Void result) {
				close.countDown();
			}
		});

		if (!close.await(10, TimeUnit.SECONDS)) {
			fail("Timeout in close");
		}
	}

	@Test
	public void connectInitTest() throws Exception {
		SctpUtils.init(clientAddr.getAddress(), SctpPorts.getInstance().generateDynPort(), null);
		CountDownLatch latch = new CountDownLatch(1);

		Promise<SctpChannelFacade, Exception, Void> p = SctpConnection.builder().local(clientAddr).remote(serverAddr)
				.build().connect(null);
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

		CountDownLatch close = new CountDownLatch(1);
		Promise<Void, Exception, Void> promise = SctpUtils.shutdownAll();
		promise.done(new DoneCallback<Void>() {

			@Override
			public void onDone(Void result) {
				close.countDown();
			}
		});

		if (!close.await(10, TimeUnit.SECONDS)) {
			fail("Timeout in close");
		}
	}

	@Test
	public void connectFailTest() throws Exception {

		Sctp.getInstance().init();
		CountDownLatch failCountDown = new CountDownLatch(1);

		InetSocketAddress local = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 9899);
		InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 9900);

		SctpSocket mockSocket = Mockito.mock(SctpSocket.class);
		Mockito.doThrow(new IOException("Just a test")).when(mockSocket).connectNative(remote.getPort());

		SctpChannel channel = new SctpChannel(9899, remote, null, new SctpDefaultStreamConfig().getCb(), new SctpMapper());
		Whitebox.setInternalState(channel, SctpSocket.class, mockSocket);

		Promise<SctpChannelFacade, Exception, Void> returnValue = Whitebox.invokeMethod(channel, "connect", remote);
		returnValue.fail(new FailCallback<Exception>() {

			@Override
			public void onFail(Exception result) {
				if (result instanceof IOException) {
					failCountDown.countDown();
				}
			}
		});
		
		returnValue.done(new DoneCallback<SctpChannelFacade>() {

			@Override
			public void onDone(SctpChannelFacade result) {
				fail("This should not work");
			}
		});
		
		if (!failCountDown.await(10, TimeUnit.SECONDS)) {
			fail("Timeoutexception");
		}
		
		CountDownLatch close = new CountDownLatch(1);
		Promise<Object, Exception, Object> promise = channel.close();
		promise.done(new DoneCallback<Object>() {

			@Override
			public void onDone(Object result) {
				try {
					Sctp.getInstance().finish();
				} catch (IOException e) {
//					ignore if fail
				}
				close.countDown();
			}
		});

		if (!close.await(10, TimeUnit.SECONDS)) {
			fail("Timeout in close");
		}
	}
}
