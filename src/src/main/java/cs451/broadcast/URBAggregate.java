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

    private long start = 0;

    public URBAggregate(int port, List<Host> hosts, int myId, BListener deliver) {
        this.link = Link.getLink(port, hosts, myId);
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
            } else {
                acks.get(id).add(m.getLastHop());
            }
            if (acks.get(id).size() > threshold) {
                delivered.add(id);
                pending.remove(id);
                // TODO stderr time here, to graph the progression (delete loggers and other stderr)
                if (start == 0) {
                    start = System.nanoTime();
                }
                System.err.println((System.nanoTime() - start));
                urbDeliver.apply(m);
            }
        } else if (acks.containsKey(id)) {
            acks.remove(id);
        }
    }

    @Override
    public void broadcast(Message m) {
        Message.IntPair id = m.getId();
        acks.put(id, ConcurrentHashMap.newKeySet());
        acks.get(id).add(myId);
        acks.get(id).add(m.getLastHop());
        if (!pending.add(id)) {
            return;
        }

        m = m.changeLastHop(myId);
        for (Host host: hosts) {
            if (!acks.get(id).contains(host.getId())) {
                try {
                    link.send(m, host.getId(), InetAddress.getByName(host.getIp()), host.getPort());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public int status() {
        return delivered.size();
    }
    
}
