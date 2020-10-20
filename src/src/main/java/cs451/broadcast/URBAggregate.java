package cs451.broadcast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cs451.link.Link;
import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

public class URBAggregate implements Broadcast {

    private final Link link;

    private final Set<Message.IntPair> pending = ConcurrentHashMap.newKeySet();
    private final Set<Message.IntPair> delivered = ConcurrentHashMap.newKeySet();
    private final Map<Message.IntPair, Set<Integer>> acks = new ConcurrentHashMap<>();
    private final List<Host> hosts;
    private final int threshold;
    private final int myId;
    private final BListener urbDeliver;

    public URBAggregate(int port, List<Host> hosts, int myId, BListener deliver) {
        this.link = Link.getLink(port, hosts.size(), myId);
        this.threshold = hosts.size() / 2;
        this.myId = myId;
        this.hosts = hosts;
        this.link.addListener((m, a, p) -> deliver(m, a, p));
        this.urbDeliver = deliver;
    }

    private void deliver(Message m, InetAddress a, int port) {
        Message.IntPair id = m.getId();
        if (!delivered.contains(id)) {
            if (!pending.contains(id)) {
                broadcast(m);
            }
            acks.get(id).add(m.getLastHop());
            if (acks.get(id).size() > threshold) {
                delivered.add(id);
                pending.remove(id);
                acks.remove(id);
                urbDeliver.apply(m);
            }
        }
    }

    @Override
    public void broadcast(Message m) {
        Message.IntPair id = m.getId();
        acks.put(id, ConcurrentHashMap.newKeySet());
        acks.get(id).add(myId);
        if (!pending.add(id)) {
            return;
        }

        m = m.changeLastHop(myId);
        for (Host host: hosts) {
            if (host.getId() == myId) {
                deliver(m, null, 0);
            } else if (!acks.get(id).contains(host.getId())) {
                try {
                    link.send(m, InetAddress.getByName(host.getIp()), host.getPort());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
}
