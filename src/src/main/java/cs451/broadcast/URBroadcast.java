package cs451.broadcast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;
import cs451.vectorclock.VectorClock;

class URBroadcast implements Broadcast {

    private final BEBroadcast beBroadcast;

    private final Map<Integer, VectorClock> delivered = new HashMap<>();
    private final Map<Message.IntPair, Integer> acks = new ConcurrentHashMap<>();

    private final BListener deliver;
    private final int threshold;

    public URBroadcast(int port, List<Host> hosts, int myId, BListener deliver) {
        this.beBroadcast = new BEBroadcast(port, hosts, myId, this::deliver);
        this.deliver = deliver;
        this.threshold = hosts.size() / 2;

        for (Host host : hosts) {
            delivered.put(host.getId(), new VectorClock());
        }
    }

    private void deliver(Message m) {
        int origin = m.getOriginId();
        int mId = m.getMessageId();
        if (!delivered.get(origin).isPast(mId)) {
            Message.IntPair messageId = m.getId();
            if (!acks.containsKey(messageId)) {
                broadcast(m);
            }
            int numAcks = acks.getOrDefault(messageId, -1);
            if (0 <= numAcks) {
                numAcks += 1;
                if (numAcks > threshold) {
                    delivered.get(origin).addMember(mId);
                    deliver.apply(m);
                    acks.remove(messageId);
                } else {
                    acks.put(messageId, numAcks);
                }
            }
        }
    }

    @Override
    public void broadcast(Message m) {
        Message.IntPair id = m.getId();
        acks.put(id, 0);
        beBroadcast.broadcast(m);
    }
}
