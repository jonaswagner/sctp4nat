package net.sctp4nat.origin;

import java.io.IOException;

/**
 * This class is used to shield {@link SctpSocket} from the user.
 * 
 * @author Jonas Wagner
 *
 */
public interface SctpAcceptable {

	/**
	 * This method provides the user the possiblity to accept incoming connection
	 * attempts, once a {@link SctpNotification} is called.
	 * 
	 * @return true if association was accepted
	 * @throws IOException
	 */
	boolean acceptNative() throws IOException;
}
