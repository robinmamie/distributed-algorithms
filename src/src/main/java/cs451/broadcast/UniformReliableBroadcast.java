package cs451.broadcast;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

class UniformReliableBroadcast implements Broadcast {

    private final Set<Message.IntPair> pending = ConcurrentHashMap.newKeySet();
    private final Set<Message.IntPair> delivered = ConcurrentHashMap.newKeySet();
    private final Map<Message.IntPair, Set<Integer>> acks = new ConcurrentHashMap<>();
    private final BestEffortBroadcast beb;
    private final int threshold;
    private final int myId;

    public UniformReliableBroadcast(int port, List<Host> hosts, int myId, BListener deliver) {
        this.threshold = hosts.size() / 2;
        this.myId = myId;
        this.beb = new BestEffortBroadcast(port, hosts, myId, m -> {
            Message.IntPair id = m.getId();
            if (!pending.contains(id) && !delivered.contains(id)) {
                broadcast(m);
            }
            acks.get(id).add(m.getLastHop());
            if (!delivered.contains(id) && acks.get(id).size() > threshold) {
                delivered.add(id);
                pending.remove(id);
                deliver.apply(m);
            }
        });
    }

    @Override
    public void broadcast(Message m) {
        Message.IntPair id = m.getId();
        acks.put(id, ConcurrentHashMap.newKeySet());
        acks.get(id).add(myId);
        pending.add(id);

        beb.broadcast(m);
    }
}
