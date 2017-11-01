package net.sctp4j.core;

import java.net.SocketAddress;

import org.jdeferred.Promise;

import net.sctp4j.origin.SctpSocket.NotificationListener;

/**
 * @author jonaswagner
 */
public interface SctpChannelFacade {
	Promise<Integer, Exception, Object> send(byte[] data, int offset, int len, boolean ordered, int sid, int ppid);
	Promise<Integer, Exception, Object> send(byte[] data, boolean ordered, int sid, int ppid);
	Promise<Object, Exception, Object> close();
	void setSctpDataCallback(SctpDataCallback cb);
	void shutdownInit();
	public void setNotificationListener(NotificationListener l);
	SocketAddress getRemote();

}
