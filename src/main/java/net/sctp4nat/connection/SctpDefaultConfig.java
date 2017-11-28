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
 *
 *         This class is a default configuration for connecting, handling and
 *         sending messages via sctp. If a extended configuration is needed,
 *         feel free extend this class and/or overwrite it with your needs.
 */
public class SctpDefaultConfig {

	private static final Logger LOG = LoggerFactory.getLogger(SctpDefaultConfig.class);

	/**
	 * This field represents the handler for the stream. Since all messages are
	 * ignored by default, you need to set it with your handler.
	 */
	@Getter
	@Setter
	private SctpDataCallback cb = new SctpDataCallback() {

		@Override
		public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
				SctpChannelFacade so) {
			// do nothing and notify the log
			LOG.info("ignored message from " + so.getRemote().getAddress().getHostAddress() + ":"
					+ so.getRemote().getPort());
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
