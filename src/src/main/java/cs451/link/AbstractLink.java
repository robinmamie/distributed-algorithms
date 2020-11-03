package cs451.link;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

public abstract class AbstractLink implements Link {

    private static final Map<Integer, HostInfo> hostInfo;

    private final BListener listener;
    private final int myId;

    static {
        hostInfo = new TreeMap<>();
    }

    protected AbstractLink(BListener listener, int myId, List<Host> hosts) {
        this.listener = listener;
        this.myId = myId;

        if (hostInfo.isEmpty()) {
            for (Host host : hosts) {
                int i = host.getId();
                if (i != getMyId()) {
                    try {
                        hostInfo.put(i, new HostInfo(InetAddress.getByName(host.getIp()), host.getPort(), hosts.size(), host.getId()));
                    } catch (UnknownHostException e) {
                        throw new RuntimeException("Invalid IP address given!");
                    }
                }
            }
        }
    }

    protected void handleListener(Message m) {
        listener.apply(m);
    }

    protected int getMyId() {
        return myId;
    }

    public static HostInfo getHostInfo(int hostId) {
        return hostInfo.get(hostId);
    }

    public static Map<Integer, HostInfo> getHostInfo() {
        return hostInfo;
    }
}
