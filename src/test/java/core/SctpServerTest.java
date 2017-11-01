package core;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpMapper;
import net.sctp4j.core.UdpServerLink;
import net.sctp4j.origin.Sctp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SctpServerTest {

    private InetSocketAddress local;
    private SctpMapper mapper;

    @Before
    public void setUp() throws Exception {
    	local = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 2000);
        mapper = new SctpMapper();
    }

    @Test
    public void testAll() throws Exception {
    	simpleSetUpTest();
    	extendedSetUpTest();
    }
    
    public void simpleSetUpTest() throws Exception {
    	CountDownLatch simpleSetUpTest = new CountDownLatch(1);
		SctpUtils.init(local.getAddress(), local.getPort(), null);
		Promise<Object, Exception, Object> p = SctpUtils.shutdownAll(null, null);
		p.done(new DoneCallback<Object>() {
			
			@Override
			public void onDone(Object result) {
				simpleSetUpTest.countDown();
			}
		});
		
		p.fail(new FailCallback<Exception>() {
			
			@Override
			public void onFail(Exception result) {
				fail();
			}
		});
		
		simpleSetUpTest.await(10, TimeUnit.SECONDS);
		
		if (simpleSetUpTest.getCount() > 0) {
			fail();
		}
    }
    
    public void extendedSetUpTest() throws Exception {
        CountDownLatch extendedSetUpTest = new CountDownLatch(1);
    	
    	Sctp.init();
    	
    	SctpDataCallback cb = new SctpDataCallback() {
			
			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpAdapter so) {
				//doNothing
			}
		};
		
		UdpServerLink link = new UdpServerLink(mapper, local.getAddress(), cb);
		Promise<Object, Exception, Object> p = SctpUtils.shutdownAll(link, mapper);
		
		p.done(new DoneCallback<Object>() {
			
			@Override
			public void onDone(Object result) {
				extendedSetUpTest.countDown();
			}
		});
		
		p.fail(new FailCallback<Exception>() {

			@Override
			public void onFail(Exception result) {
				fail();
			}
		});
		
		extendedSetUpTest.await(10, TimeUnit.SECONDS);
		
		if (extendedSetUpTest.getCount() > 0) {
			fail();
		}
    }
}
