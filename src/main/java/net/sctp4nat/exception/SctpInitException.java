package net.sctp4nat.exception;

/**
 * @author jonaswagner
 *
 * This Exception signals that the user tried to call Sctp.init() twice or more.
 *
 */
public class SctpInitException extends Exception {

	private static final long serialVersionUID = 1L;

	public SctpInitException(String string) {
		super(string);
	}


}
