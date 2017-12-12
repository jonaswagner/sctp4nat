package core;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;
import org.mockito.Mockito;

import net.sctp4nat.connection.NetworkLink;
import net.sctp4nat.connection.SctpDefaultStreamConfig;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelBuilder;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.origin.SctpDataCallback;

public class channelBuilderTest {

	@Test
	public void testBuilder() throws NoSuchFieldException, SecurityException, IllegalArgumentException,
			IllegalAccessException, UnknownHostException {

		SctpChannel mockChannel = Mockito.mock(SctpChannel.class);

		SctpChannelBuilder builder = new SctpChannelBuilder();

		Field field = SctpChannelBuilder.class.getDeclaredField("localSctpPort");
		field.setAccessible(true);
		int currentLocalSctpPort = (int) field.get(builder);

		SctpPorts.getInstance().putPort(mockChannel, 10);
		@SuppressWarnings("unused")
		SctpChannelBuilder current = builder.localSctpPort(10);
		current = builder.localSctpPort(-3000);
		currentLocalSctpPort = (int) field.get(builder);
		assertEquals(SctpPorts.PORT_NOT_INITIALIZED, currentLocalSctpPort);
		current = builder.localSctpPort(20);
		currentLocalSctpPort = (int) field.get(builder);
		assertEquals(20, currentLocalSctpPort);
		current = builder.localSctpPort(30);
		currentLocalSctpPort = (int) field.get(builder);
		assertEquals(30, currentLocalSctpPort);

		Field field2 = SctpChannelBuilder.class.getDeclaredField("remotePort");
		field2.setAccessible(true);
		int currentRemotePort = (int) field2.get(builder);
		current = builder.remotePort(68000);
		currentRemotePort = (int) field2.get(builder);
		assertEquals(SctpPorts.PORT_NOT_INITIALIZED, currentRemotePort);
		current = builder.remotePort(40);
		currentRemotePort = (int) field2.get(builder);
		assertEquals(40, currentRemotePort);

		Field field3 = SctpChannelBuilder.class.getDeclaredField("remoteAddress");
		field3.setAccessible(true);
		InetAddress currentRemoteAddress = (InetAddress) field3.get(builder);
		current = builder.remoteAddress(null);
		assertEquals(null, currentRemoteAddress);
		current = builder.remoteAddress(InetAddress.getByName("127.0.0.1"));
		currentRemoteAddress = (InetAddress) field3.get(builder);
		assertEquals(InetAddress.getByName("127.0.0.1"), currentRemoteAddress);

		Field field4 = SctpChannelBuilder.class.getDeclaredField("cb");
		field4.setAccessible(true);
		SctpDataCallback currentCb = (SctpDataCallback) field4.get(builder);
		current = builder.sctpDataCallBack(null);
		currentCb = (SctpDataCallback) field4.get(builder);
		assertEquals(null, currentCb);
		SctpDataCallback cb = new SctpDefaultStreamConfig().getCb();
		currentCb = (SctpDataCallback) field4.get(builder);
		current = builder.sctpDataCallBack(cb);

		Field field5 = SctpChannelBuilder.class.getDeclaredField("link");
		field5.setAccessible(true);
		NetworkLink currentlink = (NetworkLink) field5.get(builder);
		current = builder.networkLink(null);
		currentlink = (NetworkLink) field5.get(builder);
		assertEquals(null, currentlink);
		current = builder.networkLink(Mockito.mock(NetworkLink.class));

		Field field6 = SctpChannelBuilder.class.getDeclaredField("mapper");
		field6.setAccessible(true);
		SctpMapper currentMapper = (SctpMapper) field6.get(builder);
		current = builder.mapper(null);
		currentMapper = (SctpMapper) field6.get(builder);
		assertEquals(null, currentMapper);
		current = builder.mapper(Mockito.mock(SctpMapper.class));

	}

}
