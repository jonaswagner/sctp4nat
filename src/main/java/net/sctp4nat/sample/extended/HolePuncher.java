package net.sctp4nat.sample.extended;

import java.io.IOException;

/**
 * This class calls the os terminal to send out a single UDP packet to create a
 * UDP NAT mapping. Since it uses sendIP the resources assigned for the sending
 * of the packet will be released as soon as the packet was sent.
 * 
 * This class allows the user to test hole punching in its own environment. The
 * user is required to know IP addresses and ports from the respective entities
 * upfront. Both endpoints need to run the createNATMapping() method. The method
 * requires sendIP to be installed (apt-get install sendip).
 * 
 * @author Jonas Wagner
 *
 */
public class HolePuncher {

	/**
	 * This allows the user to run {@link HolePuncher} as a stand alone application.
	 * 
	 * @param args
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws NumberFormatException, IOException, InterruptedException {
		createNatMapping(args[0], Integer.valueOf(args[1]), args[2], Integer.valueOf(args[3]));
	}

	/**
	 * This method only works if the OS is linux and sendip is installed. The class
	 * calls the linux terminal to punch a specific hole in the NAT used. Thus a
	 * specific UDP NAT mapping will be created.
	 * 
	 * @param localAddress
	 * @param localPort
	 * @param remoteAddress
	 * @param remotePort
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void createNatMapping(String localAddress, int localPort, String remoteAddress, int remotePort)
			throws IOException, InterruptedException {

		StringBuilder builder = new StringBuilder();
		builder.append("sendip -p ipv4 "); // ipv6 would also be possible. For simplicity, only ipv4 is supported
		builder.append("-is " + localAddress + " "); // source IP address
		builder.append("-p udp "); // used protocol
		builder.append("-us " + localPort + " "); // source port
		builder.append("-ud " + remotePort + " "); // destination port
		builder.append("-d \"HoleP\" "); // this is the content of the packet
		builder.append("-v " + remoteAddress + " "); // destination IP address

		// this calls the linux terminal and executes the command built with the
		// StringBuilder
		Runtime.getRuntime().exec(builder.toString()).waitFor();
	}
}
