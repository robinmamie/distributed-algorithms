package cs451.broadcast;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntConsumer;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;
import cs451.vectorclock.VectorClock;

class FIFOBroadcast implements Broadcast {

    private final URBroadcast urBroadcast;

    private final Map<Integer, VectorClock> delivered = new TreeMap<>();

    private final BListener deliver;
    private final IntConsumer broadcastListener;
    private final int myId;

    private long startTime;

    public FIFOBroadcast(int port, List<Host> hosts, int myId, BListener deliver, IntConsumer broadcastListener) {
        this.urBroadcast = new URBroadcast(port, hosts, myId, this::deliver);
        this.deliver = deliver;
        this.broadcastListener = broadcastListener;
        this.myId = myId;

        for (Host host : hosts) {
            delivered.put(host.getId(), new VectorClock());
        }
    }

    private void deliver(Message m) {
        int origin = m.getOriginId();
        int mId = m.getMessageId();
        int start = (int) delivered.get(origin).getStateOfVc();
        delivered.get(origin).addMember(mId);
        int end = (int) delivered.get(origin).getStateOfVc();
        for (int i = start + 1; i <= end; ++i) {
            if (m.getOriginId() == myId) {
                broadcastListener.accept(i);
            }
            deliver.apply(Message.createMessage(origin, i));
        }
        if (startTime == 0) {
            startTime = System.nanoTime();
        }
        System.err.println((System.nanoTime() - startTime));
    }

    @Override
    public void broadcast(Message m) {
        urBroadcast.broadcast(m);
    }

    @Override
    public void broadcastRange(int originId, int mId) {
        urBroadcast.broadcastRange(originId, mId);
    }
}
