package core;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareEverythingForTest;
import org.powermock.reflect.Whitebox;

import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpMapper;
import net.sctp4j.core.SctpSocketAdapter;

public class SctpMapperTest {

	SctpMapper mapper = new SctpMapper();
	
	@Test
	@PrepareEverythingForTest
	public void testPut() throws UnknownHostException, IllegalArgumentException, IllegalAccessException {
		SctpSocketAdapter so = Mockito.mock(SctpSocketAdapter.class);
		InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName("localhost"), 9899);
		mapper.register(remote, so);
		
		Field socketMap = Whitebox.getField(SctpMapper.class, "socketMap");
		socketMap.get(SctpAdapter.class);
		
	}
	
}
