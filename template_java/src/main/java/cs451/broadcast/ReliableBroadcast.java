package cs451.broadcast;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cs451.Message;
import cs451.listener.Listener;
import cs451.parser.Host;

/**
 * Implements Eager Reliable Broadcast (p.76)
 */
class ReliableBroadcast implements Broadcast {

    private final Set<Message.IntPair> delivered = Collections.synchronizedSet(new HashSet<>());
    private final BestEffortBroadcast beb;
    private final Listener deliver;

    public ReliableBroadcast(int port, Listener deliver, List<Host> hosts, int myId) {
        this.deliver = deliver;
        this.beb = new BestEffortBroadcast(port, (m, a, p) -> {
            Message.IntPair id = m.getId();
            if (!delivered.contains(id)) {
                // TODO attention, hopefully it is the same than what is asked (p.76)
                broadcast(new Message(m, myId));
            }
        }, hosts);
    }

    @Override
    public void broadcast(Message m) {
        delivered.add(m.getId());
        // TODO check if address is necessary to deliver message
        // IMHO, listeners for broadcast algorithms can be simpler, with only the message (id of sender is inside)
        deliver.apply(m, null, 0);
        beb.broadcast(m);
    }
}
