package cs451.broadcast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cs451.link.Link;
import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;
import cs451.vectorclock.VectorClock;

public class UrbFifo implements Broadcast {

    private final Link link;

    private final Map<Integer, VectorClock> delivered = new HashMap<>();
    private final Map<Message.IntPair, Set<Integer>> acks = new ConcurrentHashMap<>();

    private final List<Host> hosts;
    private final int threshold;
    private final int myId;
    private final BListener deliver;

    private long startTime = 0;

    public UrbFifo(int port, List<Host> hosts, int myId, BListener deliver) {
        this.link = Link.getLink(port, hosts, myId);
        this.threshold = hosts.size() / 2;
        this.myId = myId;
        this.hosts = hosts;
        this.link.addListener(m -> deliver(m));
        this.deliver = deliver;

        for (Host host: hosts) {
            delivered.put(host.getId(), new VectorClock());
        }
    }

    private void deliver(Message m) {
        int origin = m.getOriginId();
        int mId = m.getMessageId();
        if (!delivered.get(origin).isPast(mId)) {
            if (!acks.containsKey(m.getId())) {
                broadcast(m);
            }
            Set<Integer> ack = acks.getOrDefault(m.getId(), null);
            if (ack == null) {
                return;
            }
            ack.add(m.getLastHop());
            if (ack.size() > threshold) {
                int start = (int) delivered.get(origin).getStateOfVc();
                delivered.get(origin).addMember(mId);
                int end = (int) delivered.get(origin).getStateOfVc();
                for (int i = start + 1; i <= end; ++i) {
                    deliver.apply(Message.createMessage(origin, i));
                }
                if (startTime == 0) {
                    startTime = System.nanoTime();
                }
                System.err.println((System.nanoTime() - startTime));
                acks.remove(m.getId());
            }
        }
    }

    @Override
    public void broadcast(Message m) {
        Message.IntPair id = m.getId();
        Set<Integer> ack = ConcurrentHashMap.newKeySet();
        acks.put(id, ack);
        ack.add(myId);
        ack.add(m.getLastHop());

        m = m.changeLastHop(myId);
        for (Host host: hosts) {
            if (!ack.contains(host.getId())) {
                link.send(m, host.getId());
            }
        }
    }  
}
