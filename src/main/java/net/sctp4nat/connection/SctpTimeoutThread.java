package net.sctp4nat.connection;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jdeferred.Deferred;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.core.SctpChannelFacade;

/**
 * This Thread will notify the client if something goes wrong during the connection setup.
 * @author root
 *
 */
public class SctpTimeoutThread extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(SctpTimeoutThread.class);
	
	private final Deferred<SctpChannelFacade, Exception, Object> d;
	private final long timeout;
	private final TimeUnit unit;
	private final CountDownLatch countDown;
	
	/**
	 * @param d 
	 * 			the deferred object
	 * @param timeout
	 * 			how long should the timeout be?
	 * @param unit
	 * 			in what units should the timeout be matched?
	 * @param countDown
	 * 			the {@link CountDownLatch}, which specifies when the task is finished
	 */
	public SctpTimeoutThread(final Deferred<SctpChannelFacade, Exception, Object> d, final long timeout, final TimeUnit unit, final CountDownLatch countDown) {
		this.d = d;
		this.timeout = timeout;
		this.unit = unit;
		this.countDown = countDown;
	}
	
	@Override
	public void run() {
		
		try {
			countDown.await(timeout, unit);
		} catch (InterruptedException e) {
			LOG.error("Error in SctpTimeoutThread!");
			//we only need to reject the task if it is not already finished
			if (d.isPending()) {
				d.reject(e);
			}
		}
		
		if (countDown.getCount()>0 && d.isPending()) {
			LOG.error("The connection setup took too long to finish!");
			d.reject(new TimeoutException("Timeout triggered! Connection could not be set up!"));
		}
	}
}
