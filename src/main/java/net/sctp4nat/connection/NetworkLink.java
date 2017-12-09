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

import java.io.IOException;

import javassist.NotFoundException;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.origin.SctpSocket;

/**
 * Interface used by {@link SctpSocket} and {@link SctpChannel} for sending
 * network packets.
 *
 * This class is heavily inspired by sctp4j's NetworkLInk class.
 *
 * @author Jonas Wagner
 */
public interface NetworkLink {
	
	int UDP_DEFAULT_BUFFER_SIZE = 2048;

	/**
	 * Callback triggered by <tt>SctpSocket</tt> whenever it wants to send some
	 * network packet.
	 * 
	 * @param facade
	 *            SctpChannelFacade instance.
	 * @param packet
	 *            network packet buffer.
	 * @param tos
	 *            Type of Service flag
	 * @param set_df
	 *            IP don't fragment option
	 * @throws IOException
	 *             in case of transport error.
	 * @throws NotFoundException
	 */
	void onConnOut(final SctpChannelFacade facade, final byte[] packet, final int tos)
			throws IOException, NotFoundException;

	/**
	 * This method initiates the shutdown of the {@link NetworkLink}.
	 */
	void close();
}
