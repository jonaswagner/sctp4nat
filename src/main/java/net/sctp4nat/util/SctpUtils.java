package net.sctp4nat.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import net.sctp4nat.connection.SctpDefaultConfig;
import net.sctp4nat.connection.UdpServerLink;
import net.sctp4nat.core.SctpDataCallback;
import net.sctp4nat.core.SctpMapper;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.exception.SctpInitException;
import net.sctp4nat.origin.Sctp;

public class SctpUtils {

	private static final int THREADPOOL_MULTIPLIER = 20;

	private static final Logger LOG = LoggerFactory.getLogger(SctpUtils.class);

	@Getter
	private static final SctpMapper mapper = new SctpMapper();
	@Getter
	private static final ExecutorService threadPoolExecutor = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * THREADPOOL_MULTIPLIER);

	@Getter
	@Setter
	private static UdpServerLink link;
	
	public static final int SHUT_RD = 1;
	public static final int SHUT_WR = 2;
	public static final int SHUT_RDWR = 3;
	
	public static synchronized void init(final InetAddress localAddr, final int localSctpPort, SctpDataCallback cb)
			throws SocketException, SctpInitException {

		if (Sctp.isInitialized()) {
			throw new SctpInitException("Sctp is already initialized. You should not initialize it twice!");
		} else {
			Sctp.init();
		}

		if (cb == null) {
			cb = new SctpDefaultConfig().getCb();
		}

		if (localAddr == null) {
			throw new SctpInitException("ServerAddress was null! Can't init sctp without a valid InetSocketAddress!");
		} else if (!checkFreePort(localSctpPort) || !checkRange(localSctpPort)) {
			link = new UdpServerLink(mapper, localAddr, cb);
		} else {
			link = new UdpServerLink(mapper, localAddr, localSctpPort, cb);
		}
	}

	private static boolean checkFreePort(final int sctpServerPort) {
		return SctpPorts.getInstance().isFreePort(sctpServerPort);
	}

	private static boolean checkRange(final int sctpServerPort) {
		return sctpServerPort <= 65535 || sctpServerPort > 0;
	}

	public static Promise<Object, Exception, Object> shutdownAll() {
		return shutdownAll(null, null);
	}
	
	public static Promise<Object, Exception, Object> shutdownAll(UdpServerLink customLink, SctpMapper customMapper) {
		// TODO jwa shutdown every single connection
		Deferred<Object, Exception, Object> d = new DeferredObject<>();

		threadPoolExecutor.execute(new Runnable() {

			@Override
			public void run() {
				LOG.debug("sctp4j shutdownAll initialized");

				if (customLink != null) {
					customLink.close();
				}
				link.close();

				if (customMapper != null) {
					customMapper.shutdown();
				} 
				mapper.shutdown();

				SctpPorts.shutdown();

				try {
					Sctp.finish();
				} catch (IOException e) {
					d.reject(e);
				}

				LOG.debug("sctp4j shutdownAll done");
				d.resolve(null);
			}
		});

		return d.promise();
	}
}
