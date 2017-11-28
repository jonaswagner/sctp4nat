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
package net.sctp4nat.origin;

import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelFacade;

/**
 * Callback used to listen for incoming data on SCTP socket.
 *
 * @author Pawel Domas
 * @author Jonas Wagner
 * 
 *         </br>
 *         </br>
 *         <b>Modifications</b> </br>
 *         This interface was modified, such that the {@link SctpDataCallback}
 *         knows the corresponding {@link SctpChannel}. This modification allows
 *         the user to reply on the same channel instantly.
 */
public interface SctpDataCallback {
	/**
	 * Callback fired by <tt>SctpSocket</tt> to notify about incoming data.
	 * 
	 * @param data
	 *            buffer holding received data.
	 * @param sid
	 *            SCTP stream identifier.
	 * @param ssn
	 *            the stream sequence number
	 * @param tsn
	 *            the transmission sequence number
	 * @param ppid
	 *            payload protocol identifier.
	 * @param context
	 * @param flags
	 *            the sctp chunk flags
	 * @param so
	 *            the {@link SctpChannelFacade} the packet was sent on
	 */
	void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags, SctpChannelFacade so);
}
