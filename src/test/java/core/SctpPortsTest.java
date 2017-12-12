package core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpPorts;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SctpChannel.class)
public class SctpPortsTest {

	@SuppressWarnings("static-access")
	@Test
	public void sctpPortsTest()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		SctpPorts ports = SctpPorts.getInstance();

		Field field = SctpPorts.class.getDeclaredField("portMap");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		ConcurrentHashMap<SctpChannel, Integer> portMap = (ConcurrentHashMap<SctpChannel, Integer>) field.get(ports);

		ports.putPort(null, 0);
		assertEquals(0, portMap.size());
		ports.removePort(null);
		assertEquals(0, portMap.size());

		SctpChannel mockChannel = Mockito.mock(SctpChannel.class);
		ports.putPort(mockChannel, 7000);
		assertEquals(1, portMap.size());
		ports.removePort(mockChannel);
		ports.removePort(mockChannel);
		assertEquals(0, portMap.size());

		SctpChannel mockChannel2 = Mockito.mock(SctpChannel.class);
		SctpChannel mockChannel3 = Mockito.mock(SctpChannel.class);
		ports.putPort(mockChannel, 8000);
		assertEquals(1, portMap.size());
		ports.putPort(mockChannel2, 9000);
		assertEquals(2, portMap.size());
		ports.putPort(mockChannel3, 10000);
		assertEquals(3, portMap.size());
		assertTrue(ports.isUsedPort(10000));

		int count = (SctpPorts.MAX_PORT-SctpPorts.MIN_DYN_PORT) / 2; //this makes it likely to get a dyn port collision
		for (int i= 0; i<count; i++) {
			SctpChannel anotherMockChannel = Mockito.mock(SctpChannel.class);
			ports.putPort(anotherMockChannel, ports.generateDynPort());
		}
		
		assertTrue(ports.isInValidRange(SctpPorts.MAX_PORT));
		assertFalse(ports.isInValidRange(SctpPorts.MIN_DYN_PORT - SctpPorts.MAX_PORT));
		assertFalse(ports.isInValidRange(SctpPorts.MIN_DYN_PORT + SctpPorts.MAX_PORT));
		ports.shutdown();
		assertEquals(0, portMap.size());

	}
}
