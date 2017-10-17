package core;

import net.tomp2p.sctp.connection.SctpUtils;
import net.tomp2p.sctp.core.*;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SctpServerTest {

    private static final Logger LOG = LoggerFactory.getLogger(SctpServerTest.class);

    InetSocketAddress local;

    @Before
    public void setUp() throws Exception {
        InetSocketAddress local = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), SctpPorts.SCTP_TUNNELING_PORT);
    }

    public void testInit() throws Exception {
        LOG.error("Start");
        SctpDataCallback cb = new SctpDataCallback() {
            @Override
            public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags, SctpAdapter so) {
                //do nothing
            }
        };

        SctpUtils.init(local.getAddress(), SctpPorts.SCTP_TUNNELING_PORT, cb);

        LOG.error("Finished");
    }

    public void testSetup() throws Exception{
        LOG.error("Start");
        Sctp.init();

        SctpDataCallback cb = new SctpDataCallback() {
            @Override
            public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags, SctpAdapter so) {
                //do nothing
            }
        };

        SctpMapper mapper = new SctpMapper();
        UdpServerLink link = new UdpServerLink(mapper, local.getAddress(), cb);

        Assert.assertFalse(SctpPorts.getInstance().isFreePort(SctpPorts.SCTP_TUNNELING_PORT));

        LOG.error("Finished");
    }
}