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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.util.SctpUtils;

/**
 * Class encapsulates native SCTP counterpart (which is usrsctp).
 *
 * @author Pawel Domas (initial Author) </br>
 * 
 * @author Jonas Wagner (modifier) </br>
 *         </br>
 *         Important changes: </br>
 *         <ul>
 *         <li>{@link Logger} is now done by slf4j.</li>
 *         <li>Since the Sctp part got removed from Jitsi many renaming were
 *         needed. Additionally, the native c++ code also had to be modified to
 *         match the new class paths and names. Sctp.shutdown() and finish() had
 *         to be added and fixed.</li>
 *         </ul>
 */
public class Sctp {

	/**
	 * This class is a thread-safe Singleton
	 */
	@Getter
	private static final Sctp instance = new Sctp();

	private Sctp() {
	}

	@Getter	private static boolean initialized = false;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(Sctp.class);

	/**
	 * SCTP notification
	 */
	public static final int MSG_NOTIFICATION = 0x2000;

	/**
	 * Track the number of currently running SCTP engines. Each engine calls
	 * {@link #init()} on startup and {@link #finish()} on shutdown. We want
	 * {@link #init()} to be effectively called only when there are 0 engines
	 * currently running and {@link #finish()} when the last one is performing a
	 * shutdown.
	 */
	private static int sctpEngineCount;

	/**
	 * List of instantiated <tt>SctpSockets</tt> mapped by native pointer.
	 */
	private static final Map<Long, SctpSocket> sockets = new ConcurrentHashMap<>();

	static {
		String lib = "jnsctp";

		try {
			JNIUtils.loadLibrary(lib, Sctp.class.getClassLoader());
		} catch (Throwable t) {
			logger.error("Failed to load native library " + lib + ": " + t.getMessage());
			if (t instanceof Error)
				throw (Error) t;
			else if (t instanceof RuntimeException)
				throw (RuntimeException) t;
			else
				throw new RuntimeException(t);
		}
	}

	/**
	 * Sends a shutdown control packet to the remote.
	 * 
	 * @param ptr
	 *            native socket pointer.
	 * @param how
	 *            SHUT_RD = 1 (Disables further receive operations. No SCTP protocol
	 *            action is taken.) SHUT_WR = 2 (Disables further send operations,
	 *            and initiates the SCTP shutdown sequence.) SHUT_RDWR = 3 (Disables
	 *            further send operations, and initiates the SCTP shutdown
	 *            sequence.)
	 */
	static int shutdown(final long ptr, final int how) {
		return usrsctp_shutdown(ptr, how);
	}

	/**
	 * Closes SCTP socket addressed by given native pointer.
	 *
	 * @param ptr
	 *            native socket pointer.
	 */
	static void closeSocket(long ptr) {
		usrsctp_close(ptr);

		sockets.remove(Long.valueOf(ptr));
		logger.info("SctpSocket with ptr:" + ptr + " closed and destroyed");
	}

	/**
	 * Creates new <tt>SctpSocket</tt> for given SCTP port. Allocates native
	 * resources bound to the socket.
	 *
	 * @param localPort
	 *            local SCTP socket port.
	 * @return new <tt>SctpSocket</tt> for given SCTP port.
	 */
	public static SctpSocket createSocket(int localPort) {
		long ptr = usrsctp_socket(localPort);
		SctpSocket socket;

		if (ptr == 0) {
			socket = null;
		} else {
			socket = new SctpSocket(ptr, localPort);
			sockets.put(Long.valueOf(ptr), socket);
		}

		logger.info("SctpSocket with ptr:" + ptr + " created and initialized");

		return socket;
	}

	/**
	 * Disposes of the resources held by native counterpart.
	 *
	 * @throws IOException
	 *             if usrsctp stack has failed to shutdown.
	 */
	public synchronized void finish() throws IOException {
		// Skip if we're not the last one
		if (--sctpEngineCount > 0)
			return;

		try {
			if (usrsctp_finish()) {
				Sctp.initialized = false;
				logger.debug("usrsctp_finish() successfully executed");
				return;
			}
		} finally {
			if (Sctp.initialized == true) {
				Sctp.initialized = false;
				throw new IOException("Failed to shutdown usrsctp stack. Sctp.initialized set to false!");
			}
		}
	}

	/**
	 * Initializes native SCTP counterpart.
	 */
	public synchronized void init() {
		// Skip if we're not the first one
		// if(sctpEngineCount++ > 0)
		// return;
		synchronized (this) {
			if (!initialized) {
				logger.debug("Init'ing brian's & jonas' patched usrsctp");
				usrsctp_init(0);
				initialized = true;
			}
		}
	}

	/**
	 * Passes network packet to native SCTP stack counterpart.
	 * 
	 * @param ptr
	 *            native socket pointer.
	 * @param pkt
	 *            buffer holding network packet data.
	 * @param off
	 *            the position in the buffer where packet data starts.
	 * @param len
	 *            packet data length.
	 */
	private static native void on_network_in(long ptr, byte[] pkt, int off, int len);

	/**
	 * Used by {@link SctpSocket} to pass received network packet to native
	 * counterpart.
	 *
	 * @param socketPtr
	 *            native socket pointer.
	 * @param packet
	 *            network packet data.
	 * @param offset
	 *            position in the buffer where packet data starts.
	 * @param len
	 *            length of packet data in the buffer.
	 * @param remote
	 */
	static void onConnIn(long socketPtr, byte[] packet, int offset, int len) {
		on_network_in(socketPtr, packet, offset, len);
	}

	/**
	 * Method fired by native counterpart to notify about incoming data.
	 *
	 * @param socketAddr
	 *            native socket pointer
	 * @param data
	 *            buffer holding received data
	 * @param sid
	 *            stream id
	 * @param ssn
	 * @param tsn
	 * @param ppid
	 *            payload protocol identifier
	 * @param context
	 * @param flags
	 * @throws IOException
	 */
	public static void onSctpInboundPacket(long socketAddr, byte[] data, int sid, int ssn, int tsn, long ppid,
			int context, int flags) throws IOException {
		SctpSocket socket = sockets.get(Long.valueOf(socketAddr));

		if (socket == null) {
			logger.error("No SctpSocket found for ptr: " + socketAddr);
		} else {
			socket.onSctpInboundPacket(data, sid, ssn, tsn, ppid, context, flags, SctpMapper.locate(socket));
		}

	}

	/**
	 * Method fired by native counterpart when SCTP stack wants to send network
	 * packet.
	 * 
	 * @param socketAddr
	 *            native socket pointer
	 * @param data
	 *            buffer holding packet data
	 * @param tos
	 *            type of service???
	 * @param set_df
	 *            use IP don't fragment option
	 * @return 0 if the packet has been successfully sent or -1 otherwise.
	 */
	public static int onSctpOutboundPacket(long socketAddr, byte[] data, int tos, int set_df) {
		// FIXME handle tos and set_df

		SctpSocket socket = sockets.get(Long.valueOf(socketAddr));
		int ret;

		if (socket == null) {
			ret = -1;
			logger.error("No SctpSocket found for ptr: " + socketAddr);
		} else {
			ret = socket.onSctpOut(data, tos, set_df);
		}
		return ret;
	}

	/**
	 * Waits for incoming connection.
	 * 
	 * @param ptr
	 *            native socket pointer.
	 */
	static native boolean usrsctp_accept(long ptr);

	/**
	 * Closes SCTP socket.
	 * 
	 * @param ptr
	 *            native socket pointer.
	 */
	private static native void usrsctp_close(long ptr);

	/**
	 * Connects SCTP socket to remote socket on given SCTP port.
	 * 
	 * @param ptr
	 *            native socket pointer.
	 * @param remotePort
	 *            remote SCTP port.
	 * @return <tt>true</tt> if the socket has been successfully connected.
	 */
	static native boolean usrsctp_connect(long ptr, int remotePort);

	/**
	 * Disposes of the resources held by native counterpart.
	 * 
	 * @return <tt>true</tt> if stack successfully released resources.
	 */
	private static native boolean usrsctp_finish();

	/**
	 * Initializes native SCTP counterpart.
	 * 
	 * @param port
	 *            UDP encapsulation port.
	 * @return <tt>true</tt> on success.
	 */
	private static native boolean usrsctp_init(int port);

	/**
	 * Makes socket passive.
	 * 
	 * @param ptr
	 *            native socket pointer.
	 */
	static native void usrsctp_listen(long ptr);

	/**
	 * Sends given <tt>data</tt> on selected SCTP stream using given payload
	 * protocol identifier. FIXME add offset and length buffer parameters.
	 * 
	 * @param ptr
	 *            native socket pointer.
	 * @param data
	 *            the data to send.
	 * @param off
	 *            the position of the data inside the buffer
	 * @param len
	 *            data length.
	 * @param ordered
	 *            should we care about message order ?
	 * @param sid
	 *            SCTP stream identifier
	 * @param ppid
	 *            payload protocol identifier
	 * @return sent bytes count or <tt>-1</tt> in case of an error.
	 */
	static native int usrsctp_send(long ptr, byte[] data, int off, int len, boolean ordered, int sid, int ppid);

	/**
	 * Creates native SCTP socket and returns pointer to it.
	 * 
	 * @param localPort
	 *            local SCTP socket port.
	 * @return native socket pointer or 0 if operation failed.
	 */
	private static native long usrsctp_socket(int localPort);

	/**
	 * @author jonaswagner
	 * 
	 * @param ptr
	 *            native socket pointer.
	 * @param how
	 *            (see {@link SctpUtils} constants) </br>
	 *            SHUT_RD = 1 (Disables further receive operations. No SCTP protocol
	 *            action is taken.) </br>
	 *            SHUT_WR = 2 (Disables further send operations, and initiates the
	 *            SCTP shutdown sequence.) </br>
	 *            SHUT_RDWR = 3 (Disables further send and receive operations, and
	 *            initiates the SCTP shutdown sequence.)
	 */
	static native int usrsctp_shutdown(long ptr, int how);

}
