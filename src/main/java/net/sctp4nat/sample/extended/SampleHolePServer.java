package net.sctp4nat.sample.extended;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.origin.Sctp;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;

public class SampleHolePServer extends AbstractSampleHoleP {

	static final Logger LOG = LoggerFactory.getLogger(SampleHolePServer.class);
	/**
	 * 
	 * This class
	 * 
	 * @param args </br>
	 *            args[0] --> source IPv4 address </br>
	 *            args[1] --> source port </br>
	 *            args[3] --> destination IPv4 address </br>
	 *            args[4] --> destination port </br>
	 * 
	 * @throws UnknownHostException
	 *             thrown if String could not be casted to {@link InetAddress}
	 * @throws SocketException
	 *             thrown if socket could not be created
	 * @throws NumberFormatException
	 *             thrown if args[1] or args[3] could not be casted to
	 *             {@link Integer}
	 * @throws SctpInitException
	 *             thrown if {@link Sctp}.init() would be called twice or more
	 */
	public static void main(String[] args)
			throws UnknownHostException, SocketException, NumberFormatException, SctpInitException {

		LOG.debug("Setup of SampleHolePServer started.");
		
		if (args.length < 4) {
			LOG.error("Not enough arguments! System exiting application...");
			System.exit(1);
		}
		
		castArgs(args);
		
		LOG.debug("initiating usrsctp and setup of SctpMapper and UdpServerLink");
		SctpUtils.init(sourceIP, sourcePort, cb);

		holePuncher.run();

		LOG.debug("Setup of SampleHolePServer finished");
	}

	
}