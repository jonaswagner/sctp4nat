package core;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SctpServerErrorTest {
	
	private static final String TEST_STR = "Hello World!";

	private static final Logger LOG = LoggerFactory.getLogger(SctpTest.class);

	Thread server;
	Thread client;
	InetAddress localhost;

	@Before
	public void setUp() throws UnknownHostException {
		localhost = InetAddress.getByName("127.0.0.1");
	}
	
	@Test
	public void clientError() {
		
	}
}
