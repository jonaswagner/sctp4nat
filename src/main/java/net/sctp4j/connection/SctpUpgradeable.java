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

	
}
