package core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.origin.SctpSocket;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SctpChannel.class)
public class SctpMapperTest {

	@SuppressWarnings({ "static-access", "unlikely-arg-type" })
	@Test
	public void testMapper() throws NoSuchFieldException, SecurityException, IllegalArgumentException,
			IllegalAccessException, UnknownHostException, InterruptedException, TimeoutException {

		SctpMapper mapper = new SctpMapper();

		Field field = SctpMapper.class.getDeclaredField("socketMap");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		ConcurrentHashMap<InetSocketAddress, SctpChannel> socketMap = (ConcurrentHashMap<InetSocketAddress, SctpChannel>) field
				.get(mapper);

		mapper.register(null, null);
		mapper.locate(null);
		mapper.locate(null, -1);
		
		SctpChannel mockChannel1 = channelGen();
		InetSocketAddress address1 = isaGen();
		mapper.register(address1, mockChannel1);
		assertTrue(socketMap.containsKey(address1));
		assertTrue(socketMap.containsValue(mockChannel1));

		assertEquals(mockChannel1, mapper.locate(address1.getAddress().getHostAddress(), address1.getPort()));
		SctpSocket mockSocket = Mockito.mock(SctpSocket.class);
		Mockito.when(mockChannel1.containsSctpSocket(mockSocket)).thenReturn(true);
		assertEquals(mockChannel1, mapper.locate(mockSocket));
		Mockito.when(mockChannel1.containsSctpSocket(mockSocket)).thenReturn(false);
		assertNotEquals(mockChannel1, mapper.locate(mockSocket));
	
		SctpMapper.setShutdown(true);
		SctpChannel mockChannel2 = channelGen();
		InetSocketAddress address2 = isaGen();
		mapper.register(address1, mockChannel2);
		assertFalse(socketMap.containsKey(mockChannel2));
		assertFalse(socketMap.containsValue(mockChannel2));
		SctpSocket mockSocket2 = Mockito.mock(SctpSocket.class);
		Mockito.when(mockChannel2.containsSctpSocket(mockSocket)).thenReturn(true);
		assertEquals(null, mapper.locate(mockSocket2));
		assertEquals(null, mapper.locate(address2.getAddress().getHostAddress(), address2.getPort()));

		mapper.unregister(address1);
		assertTrue(socketMap.containsKey(address1));
		assertTrue(socketMap.containsValue(mockChannel1));
		mapper.unregister(mockChannel1);
		assertTrue(socketMap.containsKey(address1));
		assertTrue(socketMap.containsValue(mockChannel1));

		if (SctpMapper.isShutdown()) {
			SctpMapper.setShutdown(false);
		}
		SctpChannel mockChannel3 = channelGen();
		InetSocketAddress address3 = isaGen();
		mapper.register(address3, mockChannel3);
		assertTrue(socketMap.containsKey(address3));
		assertTrue(socketMap.containsValue(mockChannel3));
		assertEquals(2, socketMap.size());
		InetSocketAddress nullAddress = null;
		mapper.unregister(nullAddress);
		mapper.unregister(address2);
		mapper.unregister(address3);
		assertFalse(socketMap.containsKey(address3));
		assertFalse(socketMap.containsValue(mockChannel3));
		assertEquals(1, socketMap.size());
		
		SctpChannel mockChannel4 = channelGen();
		InetSocketAddress address4 = isaGen();
		mapper.register(address4, mockChannel4);
		SctpChannel nullChannel = null;
		mapper.unregister(nullChannel);
		mapper.unregister(mockChannel2);
		mapper.unregister(mockChannel4);
		mapper.unregister(address4);
		assertFalse(socketMap.containsKey(address4));
		assertFalse(socketMap.containsValue(mockChannel4));
		assertEquals(1, socketMap.size());
		
		Deferred<Object, Exception, Object> d = new DeferredObject<>();
		Promise<Object, Exception, Object> p = d.promise();
		
		Mockito.when(mockChannel1.close()).thenReturn(p);
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(200);
					d.resolve(new Object());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}
		}).start();
		mapper.shutdown();
	}

	private InetSocketAddress isaGen() throws UnknownHostException {
		return new InetSocketAddress(InetAddress.getByName("127.0.0.1"), SctpPorts.getInstance().generateDynPort());
	}

	private SctpChannel channelGen() {
		return Mockito.mock(SctpChannel.class);
	}
}
