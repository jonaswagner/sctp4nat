package net.sctp4j.core;

import org.jdeferred.Promise;

/**
 * @author jonaswagner
 */
public interface SctpChannelFacade {
	Promise<Integer, Exception, Object> send(byte[] data, int offset, int len, boolean ordered, int sid, int ppid);
	Promise<Integer, Exception, Object> send(byte[] data, boolean ordered, int sid, int ppid);
	void setSctpDataCallback(SctpDataCallback cb);
	void shutdownInit();
}
