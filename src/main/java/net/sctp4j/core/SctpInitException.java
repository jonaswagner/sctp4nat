package net.sctp4j.core;

/**
 * @author root
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
