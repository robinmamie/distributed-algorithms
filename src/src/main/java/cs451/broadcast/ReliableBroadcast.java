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
    private final BListener deliver;

    public ReliableBroadcast(int port, List<Host> hosts, int myId, BListener deliver) {
        this.deliver = deliver;
        this.beb = new BestEffortBroadcast(port, hosts, myId, m -> {
            Message.IntPair id = m.getId();
            if (!delivered.contains(id)) {
                // TODO attention, hopefully it is the same than what is asked (p.76)
                broadcast(m);
            }
        });
    }

    @Override
    public void broadcast(Message m) {
        delivered.add(m.getId());
        // TODO check if address is necessary to deliver message
        // IMHO, listeners for broadcast algorithms can be simpler, with only the message (id of sender is inside)
        deliver.apply(m);
        beb.broadcast(m);
    }
}