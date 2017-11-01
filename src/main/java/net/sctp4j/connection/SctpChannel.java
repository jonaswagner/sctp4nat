package net.sctp4j.connection;

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
import net.sctp4j.core.SctpChannelFacade;
import net.sctp4j.core.SctpDataCallback;
import net.sctp4j.core.SctpInitException;
import net.sctp4j.core.SctpPorts;
import net.sctp4j.core.SctpSocketAdapter;
import net.sctp4j.core.SctpSocketBuilder;
import net.sctp4j.core.UdpClientLink;

@Builder
public class SctpChannel {

	private static final Logger LOG = LoggerFactory.getLogger(SctpChannel.class);

	private SctpDefaultConfig config;
	private InetSocketAddress local;
	private InetSocketAddress remote;
	private SctpDataCallback cb;

	public Promise<SctpChannelFacade, Exception, UdpClientLink> connect() {
		Deferred<SctpChannelFacade, Exception, UdpClientLink> d = new DeferredObject<>();

		//TODO: get rid of runnable here
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

				SctpSocketAdapter socket = null;
				try {
					socket = new SctpSocketBuilder().
							remoteAddress(remote.getAddress()).
							remotePort(remote.getPort()).
							sctpDataCallBack(cb).
							mapper(SctpUtils.getMapper()).
							build();
				} catch (SctpInitException e1) {
					LOG.error("Sctp is currently not initialized! Try init it with SctpUtils.init(...)");
					d.reject(e1);
					return;
				}
				
				if (socket == null) {
					d.reject(new NullPointerException("Could not create SctpSocketAdapter!"));
					return;
				} 
				
				final SctpSocketAdapter so = socket;

				UdpClientLink link = null;
				try {
					link = new UdpClientLink(local, remote, so);
				} catch (IOException e) {
					LOG.error("Could not create UdpClientLink", e);
					releaseAssignedParams(d, so, e);
				}
				
				if (link == null) {
					d.reject(new NullPointerException("Could not create UdpClientLink!"));
					return;
				}
				
				so.setLink(link);
				d.notify(link);
				
				Promise<SctpSocketAdapter, Exception, Object> p = so.connect(remote);

				p.fail(new FailCallback<Exception>() {

					@Override
					public void onFail(Exception e) {
						LOG.error("Could not connect to remote host", e);
						releaseAssignedParams(d, so, e);
					}
				});

				p.done(new DoneCallback<SctpSocketAdapter>() {

					@Override
					public void onDone(SctpSocketAdapter result) {
						SctpUtils.getThreadPoolExecutor().execute(new Runnable() {

							@Override
							public void run() {
								d.resolve((SctpChannelFacade) so);
							}
						});
					}
				});
			}

			private void releaseAssignedParams(Deferred<SctpChannelFacade, Exception, UdpClientLink> d, SctpSocketAdapter so,
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
