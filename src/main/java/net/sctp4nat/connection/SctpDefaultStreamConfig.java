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
package net.sctp4nat.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.origin.SctpDataCallback;

/**
 * 
 * @author Jonas Wagner
 * </br>
 * </br>
 * {@link SctpDefaultStreamConfig} is a convenience class, which is used for
 * sending or defining the default {@link SctpDataCallback} in sctp4nat.
 * In future, the class can be extented with additional variables or
 * parameters for the sake of configuring the association.
 * 
 */
public class SctpDefaultStreamConfig {

	private static final Logger LOG = LoggerFactory.getLogger(SctpDefaultStreamConfig.class);

	/**
	 * This field represents the handler for the stream. Since all messages are
	 * ignored by default, you need to set it with your handler.
	 */
	@Getter
	@Setter
	private SctpDataCallback cb = new SctpDataCallback() {

		@Override
		public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
				SctpChannelFacade facade) {
			// do nothing and notify the log
			LOG.info("ignored message from " + facade.getRemote().getAddress().getHostAddress() + ":"
					+ facade.getRemote().getPort());
		}
	};

	/**
	 * This is the stream id, which is used defaultly.
	 */
	@Getter
	@Setter
	private int sid = 0;

	/**
	 * This is the payload protocol identifier. It is a feature, which is offered by
	 * SCTP, but not used by it. For a better description see the SCTP
	 * specification.
	 */
	@Getter
	@Setter
	private int ppid = 0;

	/**
	 * This field specifies if the receiver should respect the order of packets.
	 */
	@Getter
	@Setter
	private boolean ordered = false;
}
