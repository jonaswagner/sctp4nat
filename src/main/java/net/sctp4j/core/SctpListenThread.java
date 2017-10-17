package net.sctp4j.core;

import org.jdeferred.Deferred;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SctpListenThread extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(SctpListenThread.class);
	
	final private SctpAdapter so;
	final private Deferred<SctpAdapter, Exception, Object> d;

	public SctpListenThread(final SctpAdapter so, Deferred<SctpAdapter, Exception, Object> d) {
		this.so = so;
		this.d = d;
	}

	@Override
	public void run() {
		super.run();

		so.listen();
		try {
			Thread.sleep(50); //listen needs to kick in (JNI)
		} catch (InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
		
		boolean visited = false;
		try {
			while (!so.accept()) {
				Thread.sleep(100);
				if (!visited) {
					LOG.debug("new connection attempt accepted successfully!");
					d.resolve(so); //we should fire resolved only once
					visited = true;
				}
			}
		} catch (InterruptedException e) {
			LOG.error(e.getMessage());
			d.reject(e);
		}
	}
}
