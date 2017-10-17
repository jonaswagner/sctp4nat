package net.tomp2p.sctp.connection;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import net.tomp2p.sctp.core.Sctp;
import net.tomp2p.sctp.core.SctpDataCallback;
import net.tomp2p.sctp.core.SctpMapper;
import net.tomp2p.sctp.core.SctpPorts;
import net.tomp2p.sctp.core.UdpServerLink;

public class SctpUtils {

	private static final Logger LOG = LoggerFactory.getLogger(SctpUtils.class);

	@Getter
	private static final SctpMapper mapper = new SctpMapper();
	@Getter
	private static final ExecutorService threadPoolExecutor = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	@Getter
	private static volatile boolean isInitialized = false;

	public static synchronized void init(final InetAddress serverAddress, final int sctpServerPort,
			SctpDataCallback cb) {

		if (isInitialized) {
			return; // we only need to init once
		} else {
			Sctp.init();
			isInitialized = true;
		}

		if (cb == null) {
			cb = new SctpDefaultConfig().getCb();
		}

		try {
			if (!checkFreePort(sctpServerPort) || !checkRange(sctpServerPort)) {
				new UdpServerLink(mapper, serverAddress, cb);
			} else {
				new UdpServerLink(mapper, serverAddress, sctpServerPort, cb);
			}
		} catch (SocketException e) {
			LOG.error("Could not create UdpServerLink!", e);
		}
	}

	private static boolean checkFreePort(final int sctpServerPort) {
		return SctpPorts.getInstance().isFreePort(sctpServerPort);
	}

	private static boolean checkRange(final int sctpServerPort) {
		return sctpServerPort <= 65535 || sctpServerPort > 0;
	}
}
