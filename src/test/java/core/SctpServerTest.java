package core;

import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.junit.Before;
import org.junit.Test;

import net.sctp4nat.connection.UdpServerLink;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.util.SctpUtils;

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
		Promise<Void, Exception, Void> p = SctpUtils.shutdownAll(null, null);
		p.done(new DoneCallback<Void>() {
			
			@Override
			public void onDone(Void result) {
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
		
		CountDownLatch close = new CountDownLatch(1);
		Promise<Void, Exception, Void> promise = SctpUtils.shutdownAll();
		promise.done(new DoneCallback<Void>() {
			
			@Override
			public void onDone(Void result) {
				close.countDown();
			}
		});
		
		if (!close.await(10, TimeUnit.SECONDS)) {
			fail("Timeout called because close() could not finish");
		}
		Sctp.getInstance().finish();
    }
    
    public void extendedSetUpTest() throws Exception {
        CountDownLatch extendedSetUpTest = new CountDownLatch(1);
    	
    	Sctp.getInstance().init();
    	
    	SctpDataCallback cb = new SctpDataCallback() {
			
			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpChannelFacade so) {
				//doNothing
			}
		};
		
		UdpServerLink link = new UdpServerLink(mapper, local.getAddress(), cb);
		Promise<Void, Exception, Void> p = SctpUtils.shutdownAll(link, mapper);
		
		p.done(new DoneCallback<Void>() {
			
			@Override
			public void onDone(Void result) {
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
		link.close();
    }
}
