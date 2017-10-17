package core;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Assert;
import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.SctpAdapter;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpMapper;
import net.sctp4j.core.UdpServerLink;
import net.sctp4j.origin.Sctp;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SctpServerTest {

    private static final Logger LOG = LoggerFactory.getLogger(SctpServerTest.class);

    private InetSocketAddress local;
    private SctpMapper mapper;
    private boolean isInit = false;

    @Before
    public void setUp() throws Exception {
    	local = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 2000);
        mapper = new SctpMapper();
    }

    @Test
    public void simpleSetUpTest() throws Exception {
		SctpUtils.init(local.getAddress(), local.getPort(), null);
		SctpUtils.shutdownSctp(null, null);
    }
    
    @Test
    public void extendedSetUpTest() throws Exception {
    	
    	Sctp.init();
    	
    	SctpDataCallback cb = new SctpDataCallback() {
			
			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
					SctpAdapter so) {
				//doNothing
			}
		};
		
		UdpServerLink link = new UdpServerLink(mapper, local.getAddress(), cb);
		SctpUtils.shutdownSctp(link, mapper);
    }
}
