package cs451.broadcast;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

/**
 * Implements Eager Reliable Broadcast (p.76)
 */
class ReliableBroadcast implements Broadcast {

    private final Set<Message.IntPair> delivered = ConcurrentHashMap.newKeySet();
    private final BestEffortBroadcast beb;

    public ReliableBroadcast(int port, List<Host> hosts, int myId, BListener deliver) {
        this.beb = new BestEffortBroadcast(port, hosts, myId, m -> {
            Message.IntPair id = m.getId();
            if (!delivered.contains(id)) {
                // TODO attention, hopefully it is the same than what is asked (p.76)
                deliver.apply(m);
                broadcast(m);
            }
        });
    }

    @Override
    public void broadcast(Message m) {
        delivered.add(m.getId());
        // Local message is delivered by BEB before sending
        beb.broadcast(m);
    }
}
