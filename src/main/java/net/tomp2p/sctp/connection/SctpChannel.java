package net.tomp2p.sctp.connection;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Builder;
import net.tomp2p.sctp.core.SctpChannelFacade;
import net.tomp2p.sctp.core.SctpAdapter;
import net.tomp2p.sctp.core.SctpDataCallback;
import net.tomp2p.sctp.core.SctpPorts;
import net.tomp2p.sctp.core.SctpSocketBuilder;
import net.tomp2p.sctp.core.UdpClientLink;

@Builder
public class SctpChannel {

	private static final Logger LOG = LoggerFactory.getLogger(SctpChannel.class);

	private SctpDefaultConfig config;
	private InetSocketAddress local;
	private InetSocketAddress remote;
	private SctpDataCallback cb;

	public Promise<SctpChannelFacade, Exception, UdpClientLink> connect() {
		Deferred<SctpChannelFacade, Exception, UdpClientLink> d = new DeferredObject<>();

		Runnable connectSeq = new Runnable() {

			@Override
			public void run() {
				if (remote == null) {
					LOG.error("Remote InetSocketAddress was null. We can't connect to null!");
					d.reject(new NullPointerException("Remote InetSocketAddress was null. We can't connect to null!"));
					return;
				}

				if (local == null) {
					LOG.error("Local InetSocketAddress was null. We can't connect to null!");
					d.reject(new NullPointerException("Local InetSocketAddress was null. We can't connect to null!"));
					return;
				}

				if (config == null) {
					config = new SctpDefaultConfig();
				}
				
				if (cb == null) {
					cb = config.getCb();
				}

				SctpAdapter so = new SctpSocketBuilder().
						localAddress(local.getAddress()).
						localPort(local.getPort()).
						remoteAddress(remote.getAddress()).
						remotePort(remote.getPort()).
						sctpDataCallBack(cb).
						mapper(SctpUtils.getMapper()).
						build();

				UdpClientLink link = null;
				try {
					link = new UdpClientLink(local, remote, so);
				} catch (IOException e) {
					LOG.error("Could not create UdpClientLink", e);
					releaseAssignedParams(d, so, e);
				}
				so.setLink(link);
				d.notify(link);
				
				Promise<SctpAdapter, Exception, Object> p = so.connect(remote);

				p.fail(new FailCallback<Exception>() {

					@Override
					public void onFail(Exception e) {
						LOG.error("Could not connect to remote host", e);
						releaseAssignedParams(d, so, e);
					}
				});

				p.done(new DoneCallback<SctpAdapter>() {

					/**
					 * There is (at the moment) no mechanism available, which allows to the usrsctp
					 * library to notify the JNI interface about completing a task like connect.
					 */
					@Override
					public void onDone(SctpAdapter result) {
						SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

							@Override
							public void run() {

								try {
									Thread.sleep(config.getConnectPeriodMillis()); // wait for the connection setup
								} catch (InterruptedException e) {
									LOG.error("Waiting for connection failed! Cause:" + e.getMessage(), e);
									releaseAssignedParams(d, so, e);
								}
								d.resolve((SctpChannelFacade) so);
							}
						});
					}
				});
			}

			private void releaseAssignedParams(Deferred<SctpChannelFacade, Exception, UdpClientLink> d, SctpAdapter so,
					Exception e) {
				d.reject(e);
				SctpPorts.getInstance().removePort(so);
				so.close();
			}
		};

		SctpUtils.getThreadPoolExecutor().execute(connectSeq);
		return d.promise();
	}

}
