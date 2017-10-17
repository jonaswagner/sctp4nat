package net.sctp4j.core;

import java.net.InetSocketAddress;

import org.jdeferred.Promise;

import net.sctp4j.origin.SctpSocket;

public interface SctpAdapter extends SctpChannelFacade{
	void listen();
	Promise<SctpAdapter, Exception, Object> connect(InetSocketAddress remote);
	boolean containsSctpSocket(SctpSocket so);
	void onConnIn(byte[] data, int offset, int length);
	boolean accept();
	void setSctpDataCallback(SctpDataCallback cb);
	void setLink(NetworkLink link);
	InetSocketAddress getRemote();
	public void setNotificationListener(SctpSocket.NotificationListener l);
}
