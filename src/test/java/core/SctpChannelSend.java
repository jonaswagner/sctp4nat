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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import net.sctp4nat.connection.SctpDefaultStreamConfig;
import net.sctp4nat.core.NetworkLink;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelBuilder;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpNotification;
import net.sctp4nat.origin.SctpSocket;
import net.sctp4nat.origin.SctpSocket.NotificationListener;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;

import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.NotFoundException;;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SctpChannel.class)
public class SctpChannelSend {

	private static final Logger LOG = LoggerFactory.getLogger(SctpChannelSend.class);
	
	private static CountDownLatch shutdownCountDown = new CountDownLatch(1);
	
//	@Test
	public void testSendFail() throws SctpInitException, InterruptedException, IOException {
		LOG.debug("SendFail start");
		
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
		CountDownLatch countDown = new CountDownLatch(1);
		
		SctpSocket mockSocket = Mockito.mock(SctpSocket.class);
		Whitebox.setInternalState(channel, SctpSocket.class, mockSocket);
		
		SctpDefaultStreamConfig config = new SctpDefaultStreamConfig();
		byte[] data = new byte[2048];
		Mockito.when(mockSocket.sendNative(data, 0, data.length, false, 0, 0)).thenThrow(new IOException("test"));
		Promise<Integer, Exception, Object> promise = channel.send(data, config);
		promise.fail(new FailCallback<Exception>() {
			
			@Override
			public void onFail(Exception result) {
				countDown.countDown();
			}
		});
		
		if (!countDown.await(3, TimeUnit.SECONDS)) {
			fail("send fail");
		}
		
		CountDownLatch countDown2 = new CountDownLatch(1);
		Promise<Integer, Exception, Object> promise3 = channel.send(data, config);
		promise3.fail(new FailCallback<Exception>() {
			
			@Override
			public void onFail(Exception result) {
				countDown2.countDown();
			}
		});
		
		if (!countDown2.await(3, TimeUnit.SECONDS)) {
			fail("send fail");
		}
		
		Promise<Object, Exception, Object> promise4 = channel.close();
		promise4.done(new DoneCallback<Object>() {
			
			@Override
			public void onDone(Object result) {
				shutdownCountDown.countDown();
				try {
					Sctp.getInstance().finish();
				} catch (IOException e) {
					LOG.error(e.getMessage());
				}
			}
		});
		
		if (!shutdownCountDown.await(10, TimeUnit.SECONDS)) {
			fail("could not shutdown usrsctp properly");
		}
		
		LOG.debug("SendFail finished");
	}
}
