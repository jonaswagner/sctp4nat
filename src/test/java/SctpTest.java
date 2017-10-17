package net.tomp2p.sctp.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SctpTest {

    private static final Logger LOG = LoggerFactory.getLogger(SctpTest.class);

    Thread server;
    Thread client;
    
    @Before
    public void setUp() {
    	
    }

    @Test
    public void sctpTransmissionTest() {

    }
}
