package core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tomp2p.sctp.connection.SctpUtils;
import net.tomp2p.sctp.core.SctpAdapter;
import net.tomp2p.sctp.core.SctpDataCallback;
import net.tomp2p.sctp.core.SctpMapper;
import net.tomp2p.sctp.core.UdpClientLink;
import net.tomp2p.sctp.core.UdpServerLink;

public class SctpTest {

    private static final Logger LOG = LoggerFactory.getLogger(SctpTest.class);

    Thread server;
    Thread client;
    InetAddress localhost = InetAddress.getByName("127.0.0.1");
    
    @Before
    public void setUp() {
    	
    }

    @Test
    public void sctpTransmissionTest() {
    	CountDownLatch serverCd = new CountDownLatch(1);
    	CountDownLatch clientCd = new CountDownLatch(1);
    	CountDownLatch comCd = new CountDownLatch(1);
    	
    	
    	server = new Thread(new Runnable() {
			public void run() {
				InetSocketAddress local = new InetSocketAddress(localhost, 9899);
				
				SctpMapper mapper = new SctpMapper();
				
				SctpDataCallback cb = new SctpDataCallback() {
					
					@Override
					public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
							SctpAdapter so) {
						LOG.debug("I WAS HERE");
						LOG.debug("got data: " + new String(data, StandardCharsets.UTF_8));
						so.send(data, false, sid, (int) ppid);
						comCd.countDown();
					}
				};
				
				UdpServerLink link = new UdpServerLink(mapper, local.getAddress(), cb);
				
				LOG.debug("SETUP COMPLETE");
				serverCd.countDown();
			}
		});
    	
    	client = new Thread(new Runnable{
    		serverCd.await();
    		
    		InetSocketAddress local = new InetSocketAddress(localhost, 2000);
    		InetSocketAddress remote = new InetSocketAddress(localhost, 9899);
    		int localSctpPort = 12345;
    		SctpMapper mapper = new SctpMapper();

    		SctpDataCallback cb = new SctpDataCallback() {

    			@Override
    			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
    					SctpAdapter so) {
    				LOG.debug("I WAS HERE");
    			}
    		};

    		SctpAdapter so = new SctpSocketBuilder().
    				localAddress(local.getAddress()).
    				localPort(local.getPort()).
    				localSctpPort(localSctpPort).
    				remoteAddress(remote.getAddress()).
    				remotePort(remote.getPort()).
    				sctpDataCallBack(cb).
    				mapper(mapper).
    				build();
    		
    		UdpClientLink link = new UdpClientLink(local, remote, so);
    		so.setLink(link);
    		
    		Promise<SctpAdapter, Exception, Object> p = so.connect(remote);
    		
    		p.done(new DoneCallback<SctpAdapter>() {
    			
    			@Override
    			public void onDone(SctpAdapter result) {
    				SctpUtils.getThreadPoolExecutor().execute(new Runnable() {
    					
    					@Override
    					public void run() {
    						
    						clientCd.countDown();
    						
    						try {
    							Thread.sleep(2000); //wait for the connection setup
    						} catch (InterruptedException e) {
    							e.printStackTrace();
    						}
    						
    						int success = result.send("Hello World!".getBytes(), false, 0, 0);
    						if (success > 0) {
    							LOG.debug("Message sent");
    						} else {
    							LOG.error("ERROR WHILE SENDING THE MESSAGE!");
    						}
    					}
    				});
    			}
    		});
    		
    		LOG.debug("SETUP COMPLETE");
    	});
    	
    	server.run();
    	client.run();
    	
    	comCd.await();
    	
    }
}