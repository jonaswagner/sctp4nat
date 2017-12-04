package core;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.easymock.EasyMock;
import org.jdeferred.Deferred;
import org.jdeferred.impl.DeferredObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelBuilder;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpNotification;
import net.sctp4nat.origin.SctpSocket.NotificationListener;

import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SctpChannel.class)
public class SctpChannelTest {

	private static final Logger LOG = LoggerFactory.getLogger(SctpChannelTest.class);
	private static final Random RND = new Random(new Date().getTime());

	@Test
	public void testNotifications() throws Exception {

		Sctp.getInstance().init();
		InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 12345);

		SctpChannel channel = new SctpChannelBuilder().localSctpPort(9899).mapper(new SctpMapper())
				.remoteAddress(remote.getAddress()).remotePort(remote.getPort()).build();
		Deferred<SctpChannelFacade, Exception, Void> d = new DeferredObject<>();
		CountDownLatch countDown = new CountDownLatch(1);

		SctpNotification sctpNotificationMock = Mockito.mock(SctpNotification.class);
		Mockito.when(sctpNotificationMock.toString())
				.thenReturn("Random number:" + RND.nextInt() + ", " + SctpNotification.COMM_UP_STR);

		NotificationListener l = Whitebox.invokeMethod(channel, "addNotificationListener", d, countDown);
		l.onSctpNotification(null, sctpNotificationMock);
		
		assertTrue(countDown.getCount() == 0);
		assertTrue(!d.isPending());
		assertTrue(d.isResolved());

		LOG.debug("Test finished!");
	}
}
