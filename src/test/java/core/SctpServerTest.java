package core;

import net.sctp4j.connection.SctpUtils;
import net.sctp4j.core.*;
import net.sctp4j.origin.Sctp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

    @Test
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