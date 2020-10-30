package cs451.broadcast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;
import cs451.vectorclock.BroadcastAcks;
import cs451.vectorclock.VectorClock;

class URBroadcast implements Broadcast {

    private final BEBroadcast beBroadcast;

    private final Map<Integer, VectorClock> delivered = new HashMap<>();
    private final BroadcastAcks acks;

    private final BListener deliver;
    private final int threshold;
    private final int myId;

    public URBroadcast(int port, List<Host> hosts, int myId, BListener deliver, IntConsumer broadcastListener) {
        this.beBroadcast = new BEBroadcast(port, hosts, myId, this::deliver, broadcastListener);
        this.acks = new BroadcastAcks(hosts.size(), myId);
        this.deliver = deliver;
        this.threshold = hosts.size() / 2;
        this.myId = myId;

        for (Host host : hosts) {
            delivered.put(host.getId(), new VectorClock());
        }
    }

    private void deliver(Message m) {
        int origin = m.getOriginId();
        int mId = m.getMessageId();
        synchronized (delivered) {
            if (!delivered.get(origin).isPast(mId)) {
                int count = acks.addAndReturnCount(m);
                if (count == 1) {
                    broadcast(m);
                }
                if (count > threshold) {
                    delivered.get(origin).addMember(mId);
                    deliver.apply(m);
                }
            }
        }
    }

    @Override
    public void broadcast(Message m) {
        beBroadcast.broadcast(m);
    }

    public long getLocallyLastDeliveredMessage() {
        return delivered.get(myId).getStateOfVc();
    }

    @Override
    public void broadcastRange(int originId, int mId) {
        beBroadcast.broadcastRange(originId, mId);
    }
}
