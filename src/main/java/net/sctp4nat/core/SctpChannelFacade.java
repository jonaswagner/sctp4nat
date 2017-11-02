package net.sctp4nat.core;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.jdeferred.Promise;

import net.sctp4nat.origin.SctpSocket.NotificationListener;

/**
 * @author jonaswagner
 */
public interface SctpChannelFacade {
	Promise<Integer, Exception, Object> send(byte[] data, int offset, int len, boolean ordered, int sid, int ppid);
	Promise<Integer, Exception, Object> send(byte[] data, boolean ordered, int sid, int ppid);
	Promise<Object, Exception, Object> close();
	void setSctpDataCallback(SctpDataCallback cb);
	InetSocketAddress getRemote();
	void shutdownInit();
	void setNotificationListener(NotificationListener l);

}
