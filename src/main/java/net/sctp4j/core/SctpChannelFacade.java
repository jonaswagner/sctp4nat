package net.sctp4j.core;
/**
 * @author jonaswagner
 */
public interface SctpChannelFacade {
	int send(byte[] data, int offset, int len, boolean ordered, int sid, int ppid);
	int send(byte[] data, boolean ordered, int sid, int ppid);
	int close();
}
