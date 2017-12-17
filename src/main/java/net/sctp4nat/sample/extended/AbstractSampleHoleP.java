package net.sctp4nat.sample.extended;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jdeferred.Deferred;
import org.jdeferred.impl.DeferredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpTimeoutThread;
import net.sctp4nat.origin.SctpDataCallback;

public abstract class AbstractSampleHoleP {

	static final Logger LOG = LoggerFactory.getLogger(AbstractSampleHoleP.class);
	protected static volatile boolean connected = false;
	private static final int TIMEOUT_MILLIS = 10000;

	protected static Inet4Address sourceIP = null;
	protected static int sourcePort = -1;
	protected static Inet4Address destinationIP = null;
	protected static int destinationPort = -1;
	protected static int messageCap = 1000;
	protected static int counter = 0;

	protected static final SctpDataCallback cb = new SctpDataCallback() {

		CountDownLatch countDown = new CountDownLatch(1);
		Deferred<SctpChannelFacade, Exception, Void> d = new DeferredObject<>();

		@Override
		public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
				SctpChannelFacade facade) {
			if (connected == false) {
				new SctpTimeoutThread(d, 2, TimeUnit.MINUTES, countDown).start();
			}

//			if (messageCap > 0 && d.isPending()) {
				connected = true;
				counter ++;
				LOG.debug("packet #" + counter + " received with length {}. Now sending the very same packet back...", data.length);
				LOG.debug("onSctpPacket() called with data {}", new String(data, StandardCharsets.UTF_8));
				facade.send(data, false, sid, (int) ppid);
//				messageCap--;
//			} else if (!d.isPending() && d.isRejected()) {
//				LOG.error("Test was not successful!");
//				System.exit(1);
//			} else {
//				LOG.error("Test sucessfully completed");
//				facade.close();
//				System.exit(0);
//			}
		}
	};
	protected static final Runnable holePuncher = new Runnable() {

		@Override
		public void run() {

			while (!connected) {
				LOG.warn("creating NAT mapping");
				try {
					HolePuncher.createNatMapping(sourceIP.getHostAddress(), sourcePort, destinationIP.getHostAddress(),
							destinationPort);
					Thread.sleep(TIMEOUT_MILLIS);
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
	};

	/**
	 * 
	 * casts all arguments from the {@link String}[] to the respective variables.
	 * 
	 * @param args
	 *            </br>
	 *            args[0] --> source IPv4 address </br>
	 *            args[1] --> source port </br>
	 *            args[3] --> destination IPv4 address </br>
	 *            args[4] --> destination port </br>
	 */
	protected static void castArgs(String[] args) {
		LOG.debug("casting args...");
		try {
			sourceIP = (Inet4Address) InetAddress.getByName(args[0]);
			sourcePort = Integer.valueOf(args[1]);
			destinationIP = (Inet4Address) InetAddress.getByName(args[2]);
			destinationPort = Integer.valueOf(args[3]);
		} catch (Exception e) {
			LOG.error(e.getMessage());
			System.exit(1);
		}

		LOG.debug("source {}/{}, destination {}/{}", sourceIP.getHostAddress(), sourcePort,
				destinationIP.getHostAddress(), destinationPort);

		if (sourceIP == null || sourcePort == -1 || destinationIP == null || destinationPort == -1) {
			LOG.error("could cast args! System exiting application");
			System.exit(1);
		}
	}

}
