package core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.junit.Test;
//import org.junit.runner.RunWith;
import org.mockito.Mockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.NotFoundException;
import net.sctp4nat.connection.NetworkLink;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelBuilder;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpNotification;
import net.sctp4nat.origin.SctpSocket.NotificationListener;
import net.sctp4nat.util.SctpUtils;

public class SctpChannelNotificationTest {

	private static final Logger LOG = LoggerFactory.getLogger(SctpChannelNotificationTest.class);
	private static final Random RND = new Random(new Date().getTime());
	
	private static CountDownLatch shutdownCountDown;

	@Test
	public void testNotifications() throws Exception {
		shutdownCountDown = new CountDownLatch(2);
		
		Sctp.getInstance().init();
		InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 12345);
		NetworkLink link = new NetworkLink() {
			
			@Override
			public void onConnOut(SctpChannelFacade facade, byte[] packet, int tos) throws IOException, NotFoundException {}
			
			@Override
			public void close() {}
		};
		
		SctpChannel channel = new SctpChannelBuilder().localSctpPort(9899).mapper(new SctpMapper())
				.remoteAddress(remote.getAddress()).networkLink(link).remotePort(remote.getPort()).build();
		Deferred<SctpChannelFacade, Exception, Void> d = new DeferredObject<>();
		CountDownLatch countDown = new CountDownLatch(1);

		SctpNotification sctpNotificationMock = Mockito.mock(SctpNotification.class);
		Mockito.when(sctpNotificationMock.toString())
				.thenReturn("Random number:" + RND.nextInt() + ", " + SctpNotification.COMM_UP_STR);
		
		//case connect
		NotificationListener l = Whitebox.invokeMethod(channel, "addNotificationListener", d, countDown);
		l.onSctpNotification(null, sctpNotificationMock);
		
		assertTrue(countDown.getCount() == 0);
		assertTrue(!d.isPending());
		assertTrue(d.isResolved());
		
		//case shutdown
		Mockito.when(sctpNotificationMock.toString())
		.thenReturn("Random number:" + RND.nextInt() + ", " + SctpNotification.SHUTDOWN_COMP_STR);
		d = new DeferredObject<>();
		countDown = new CountDownLatch(1);
		l = Whitebox.invokeMethod(channel, "addNotificationListener", d, countDown);
		l.onSctpNotification(null, sctpNotificationMock);

		assertFalse(countDown.getCount() == 0);
		assertFalse(d.isPending());
		assertTrue(d.isRejected());
		
		Mockito.when(sctpNotificationMock.toString())
		.thenReturn("Random number:" + RND.nextInt() + ", " + SctpNotification.ADDR_UNREACHABLE_STR);
		d = new DeferredObject<>();
		countDown = new CountDownLatch(1);
		l = Whitebox.invokeMethod(channel, "addNotificationListener", d, countDown);
		l.onSctpNotification(null, sctpNotificationMock);
		
		assertFalse(countDown.getCount() == 0);
		assertFalse(d.isPending());
		assertTrue(d.isRejected());
		
		Mockito.when(sctpNotificationMock.toString())
		.thenReturn("Random number:" + RND.nextInt() + ", " + SctpNotification.COMM_LOST_STR);
		d = new DeferredObject<>();
		countDown = new CountDownLatch(1);
		l = Whitebox.invokeMethod(channel, "addNotificationListener", d, countDown);
		l.onSctpNotification(null, sctpNotificationMock);
		
		assertFalse(countDown.getCount() == 0);
		assertFalse(d.isPending());
		assertTrue(d.isRejected());
		
		Mockito.when(sctpNotificationMock.toString())
		.thenReturn("Random number:" + RND.nextInt() + ", " + SctpNotification.SHUTDOWN_EVENT_STR);
		d = new DeferredObject<>();
		countDown = new CountDownLatch(1);
		l = Whitebox.invokeMethod(channel, "addNotificationListener", d, countDown);
		l.onSctpNotification(null, sctpNotificationMock);
		
		assertFalse(countDown.getCount() == 0);
		assertTrue(d.isPending());		
		
		Mockito.when(sctpNotificationMock.toString())
		.thenReturn("Random number:" + RND.nextInt());
		d = new DeferredObject<>();
		countDown = new CountDownLatch(1);
		l = Whitebox.invokeMethod(channel, "addNotificationListener", d, countDown);
		l.onSctpNotification(null, sctpNotificationMock);
		
		assertFalse(countDown.getCount() == 0);
		assertTrue(d.isPending());
		
		LOG.debug("Test finished!");
		
		Promise<Object, Exception, Object> promise = channel.close();
		promise.done(new DoneCallback<Object>() {
			
			@Override
			public void onDone(Object result) {
				shutdownCountDown.countDown();
				Promise<Void, Exception, Void> promise2 = SctpUtils.shutdownAll();
				promise2.done(new DoneCallback<Void>() {

					@Override
					public void onDone(Void result) {
						shutdownCountDown.countDown();
					}
				});
				promise2.fail(new FailCallback<Exception>() {

					@Override
					public void onFail(Exception result) {
						shutdownCountDown.countDown();
					}
				});
			}
		});
		
		if (!shutdownCountDown.await(10, TimeUnit.SECONDS)) {
			fail("could not shutdown usrsctp properly");
		}
	}

}
