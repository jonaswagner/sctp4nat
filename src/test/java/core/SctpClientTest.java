package core;

import net.tomp2p.sctp.core.SctpMapper;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public void serverSetUpTest() throws Exception {

    }
}
