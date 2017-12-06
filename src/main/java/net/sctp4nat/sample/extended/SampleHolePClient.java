package net.sctp4nat.sample.extended;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.connection.UdpClientLink;
import net.sctp4nat.core.NetworkLink;
import net.sctp4nat.core.SctpChannel;
import net.sctp4nat.core.SctpChannelBuilder;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;

public class SampleHolePClient extends AbstractSampleHoleP {

	private static final Logger LOG = LoggerFactory.getLogger(SampleClient.class);

	public static void main(String[] args) throws IOException, SctpInitException {

		LOG.debug("Setup of SampleHolePClient started.");
		if (args.length < 4) {
			LOG.error("Not enough arguments! System exiting application...");
			System.exit(1);
		}

		castArgs(args);

		LOG.debug("initiating usrsctp and setup of SctpMapper and UdpServerLink");
		SctpUtils.init(sourceIP, sourcePort + 1, cb);

		castArgs(args);

		SctpChannel so = new SctpChannelBuilder().localSctpPort(sourcePort).remoteAddress(destinationIP)
				.remotePort(destinationPort).sctpDataCallBack(cb).mapper(SctpUtils.getMapper()).build();

		NetworkLink link = new UdpClientLink(new InetSocketAddress(sourceIP, sourcePort),
				new InetSocketAddress(destinationIP, destinationPort), so);
		so.setLink(link);

		new Thread(holePuncher).start();

		Promise<SctpChannelFacade, Exception, Void> p = so
				.connect(new InetSocketAddress(destinationIP, destinationPort));

		p.done(new DoneCallback<SctpChannelFacade>() {

			@Override
			public void onDone(SctpChannelFacade result) {

				LOG.error("connected to {}/{}", destinationIP, destinationPort);
				connected = true;

				boolean exitLoop = false;
				while (!exitLoop) {
					LOG.error("1 = send \"Hello World\"");
					LOG.error("2 = exit program");

					Scanner scanner = new Scanner(System.in);
					int i = scanner.nextInt();

					switch (i) {
					case 1: {
						result.send("Hello World!".getBytes(), false, 0, 0);
						continue;
					}
					case 2: {
						LOG.error("now exiting program on command...");
						scanner.close();
						System.exit(0);
					}
					default:
					}
				}
			}
		});

		p.fail(new FailCallback<Exception>() {

			@Override
			public void onFail(Exception result) {
				LOG.error(result.getMessage());
				result.printStackTrace();
				System.exit(1);
			}
		});

		LOG.debug("Setup of SampleHolePClient finished");
	}
}
