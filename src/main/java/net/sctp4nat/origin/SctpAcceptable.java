package net.sctp4nat.origin;

import java.io.IOException;

public interface SctpAcceptable {

	boolean acceptNative() throws IOException;
}
