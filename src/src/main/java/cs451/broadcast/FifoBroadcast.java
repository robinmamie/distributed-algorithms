package cs451.broadcast;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

public class FifoBroadcast implements Broadcast {

    private final Broadcast urb;
    private final Map<Integer, Integer> delivered = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> past = new ConcurrentHashMap<>();

    public FifoBroadcast(int port, List<Host> hosts, int myId, BListener deliver) {
        for (int i = 1; i <= hosts.size(); ++i) {
            delivered.put(i, 1);
            past.put(i, ConcurrentHashMap.newKeySet());
        }
        urb = new URBAggregate(port, hosts, myId, m -> {
            synchronized (past) {
                int deliveredUntil = delivered.get(m.getOriginId());
                past.get(m.getOriginId()).add(m.getMessageId());
                while (past.get(m.getOriginId()).contains(deliveredUntil)) {
                    // Retrieve correct message data
                    deliver.apply(Message.createMessage(m.getOriginId(), deliveredUntil));
                    deliveredUntil += 1;
                }
                delivered.put(m.getOriginId(), deliveredUntil);
                final int newMax = deliveredUntil;
                past.get(m.getOriginId()).removeIf(x -> x < newMax);
            }
        });
    }

    @Override
    public void broadcast(Message m) {
        urb.broadcast(m);
    }

    public int status() {
        return urb.status();
    }
    
}
