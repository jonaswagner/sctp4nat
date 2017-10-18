package net.sctp4j.connection;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.jdeferred.Promise;

import net.sctp4j.core.NetworkLink;
import net.sctp4j.core.SctpChannelFacade;

public interface SctpUpgradeable {

	/**
	 * This method initiates the transition from udp to sctp via udp.
	 * 
	 * @param config
	 * @param local
	 * @param remote
	 * @return
	 */
	Promise<SctpChannelFacade, Exception, NetworkLink> upgrade(SctpDefaultConfig config, InetSocketAddress local,
			InetSocketAddress remote);

	/**
	 * This method answers the transition attempt from the server side and
	 * transitions the connection from plain udp to sctp via udp.
	 * 
	 * @param local
	 * @return
	 */
	Promise<NetworkLink, Exception, Object> initUpgrade(SocketAddress local);

	
}
