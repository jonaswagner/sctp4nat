package core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpMapper;
import net.sctp4j.core.SctpSocketAdapter;

//@RunWith(PowerMockRunner.class)
@PrepareForTest(SctpSocketAdapter.class)
public class SctpAdapterTest {

	@Test
	@PrepareForTest(SctpSocketAdapter.class)
	public void testConnect() throws Exception {

		SctpSocketAdapter mockSocketAdapter = PowerMockito.mock(SctpSocketAdapter.class);
		InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName("localhost"), 1000);
		Deferred<SctpAdapter, Exception, Object> def = new DeferredObject<>();
		def.reject(new IOException("Could not connect via SCTP!"));
		PowerMockito.when(mockSocketAdapter.connect(remote)).thenReturn(def.promise());

		InetSocketAddress local = new InetSocketAddress(InetAddress.getByName("localhost"), 1000);
		SctpMapper mapper = new SctpMapper();

		SctpSocketAdapter so = mockSocketAdapter;
		Promise<SctpAdapter, Exception, Object> prms = so.connect(remote);
		prms.done(new DoneCallback<SctpAdapter>() {

			@Override
			public void onDone(SctpAdapter result) {
				fail("you should not be here!");
			}
		});
	}
}