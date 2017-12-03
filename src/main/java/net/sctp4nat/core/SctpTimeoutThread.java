/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sctp4nat.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jdeferred.Deferred;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Thread will notify the client if something goes wrong during the connection setup.
 * 
 * @author Jonas Wagner
 *
 */
public class SctpTimeoutThread extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(SctpTimeoutThread.class);
	
	private final Deferred<SctpChannelFacade, Exception, Void> d;
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
	public SctpTimeoutThread(final Deferred<SctpChannelFacade, Exception, Void> d, final long timeout, final TimeUnit unit, final CountDownLatch countDown) {
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
