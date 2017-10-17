package core;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4j.core.SctpMapper;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SctpClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(SctpClientTest.class);

    private InetSocketAddress local;
    private InetSocketAddress remote;
    private int localSctpPort;
    private SctpMapper mapper;

    @Before
    public void setUp() throws Exception {
        local = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 2000);
        remote = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 9899);
        localSctpPort = 12345;
        mapper = new SctpMapper();
    }

    @Test
    public void serverSetUpTest() throws Exception {
    	
    }
}
