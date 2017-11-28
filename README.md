# sctp4nat
This is a simple Java library, which provides the user to connect two endpoints (at least one with Java) to each other via SCTP. Since each SCTP packet is wrapped into a UDP packet, it is possible to overcome NAT devices easily (e.g. with hole punching).

This library is not finished yet. But if you still want to try it out, you should know some stuff beforehand:
- This library uses usrsctp via JNI interface. 
- The usrsctp-part of this library is not cross-compiled yet. Thus, it only works on linux. It is tested on Ubuntu 16.04.

## Code samples
To make the library more easy-to-use for you guys, here's a small code sample of how to create a client and a server.

### Server
First, specify the local address and port. Optionally (as shown in the code sample below), specify the callback, which is called on incoming SCTP data packets. Second, initialize usrsctp via the static method "SctpUtils.ini(...). If you wonder, which port is the SCTP tunneling port: It is the standardized IANA SCTP via UDP port (which is 9899). 
```
InetAddress localHost = Inet6Address.getByName("::1");
		
SctpDataCallback cb = new SctpDataCallback() {
			
	@Override
	public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
			SctpChannelFacade so) {
		System.out.println("I WAS HERE");
		System.out.println("got data: " + new String(data, StandardCharsets.UTF_8));
		so.send(data, new SctpDefaultConfig());
	}
};
		
SctpUtils.init(localHost, SctpPorts.SCTP_TUNNELING_PORT, cb);
```

### Client
Furst, initialze the usrsctp library (Sctp.getInstance().init() is an alternative way to do it). Second, specify the local and remote IP address and port. Optionally, specify the "SctpDataCallback". Third, create a "SctpConnection" object. Fourth, connect this connection object to the remote SCTP endpoint. Once the endpoints are connected, Promise p will call p.done(...) with a "SctpChannelFacade" object. This SctpChannelFacade is the interface for the association between client and server.
```
Sctp.getInstance().init();

InetAddress localHost = Inet6Address.getByName("::1");
InetSocketAddress local = new InetSocketAddress(localHost, SctpPorts.getInstance().generateDynPort());
InetSocketAddress remote = new InetSocketAddress(localHost, SctpPorts.SCTP_TUNNELING_PORT);
		
SctpDataCallback cb = new SctpDataCallback() {
			
	@Override
	public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
			SctpChannelFacade so) {
		System.out.println("I WAS HERE");
		System.out.println("got data: " + new String(data, StandardCharsets.UTF_8));
		System.out.println("Now closing channel");
		so.close();
	}
};

SctpConnection channel = SctpConnection.builder().cb(cb).local(local).remote(remote).build();
Promise<SctpChannelFacade, Exception, Object> p = channel.connect(null);
p.done(new DoneCallback<SctpChannelFacade>() {
			
	@Override
	public void onDone(SctpChannelFacade result) {
		result.send("Hello World!".getBytes(), false, 0, 0);
	}
});
```
