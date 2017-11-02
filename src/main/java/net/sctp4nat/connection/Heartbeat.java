package net.sctp4nat.connection;

public class Heartbeat {

//	private static final Logger LOG = LoggerFactory.getLogger(Heartbeat.class);
//	private static final Heartbeat INSTANCE = new Heartbeat();
//	private static final ConcurrentHashMap<SctpSocketAdapter, Long> SOCKET_MAP = new ConcurrentHashMap<>();
//	private static final Long HEARTBEAT_INTERVAL = new Long(60000);
//	private static final long TICK = 1000l;
//	private static Timer TIMER;
//
//	private Heartbeat() {
//		TIMER = new Timer(true);
//		TIMER.scheduleAtFixedRate(new TimerTask() {
//
//			@Override
//			public void run() {
//				if (SOCKET_MAP.isEmpty()) {
//					return;
//				} else {
//					SOCKET_MAP.values().stream().forEach(lastHeartBeat -> lastHeartBeat++);
//					for (Map.Entry<SctpSocketAdapter, Long> entry : SOCKET_MAP.entrySet()) {
//						if (entry.getValue() >= HEARTBEAT_INTERVAL) {
//							entry.getKey().sendHeartBeat();
//						}
//					}
//				}
//			}
//		}, 0, TICK);
//	}
//
//	public synchronized static void register(final SctpSocketAdapter so) {
//		if (SOCKET_MAP.contains(so)) {
//			LOG.error("Socket already registered on Heartbeat. You can't register it twice");
//			return;
//		} else {
//			SOCKET_MAP.put(so, 0);
//		}
//	}
//
//	public synchronized static void unregister(final SctpSocketAdapter so) {
//		if (SOCKET_MAP.contains(so)) {
//			LOG.error("Socket is not registered on Heartbeat. Thus it can't be removed!");
//			return;
//		} else {
//			SOCKET_MAP.remove(so);
//		}
//	}
}
