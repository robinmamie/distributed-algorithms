package cs451.broadcast;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntConsumer;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;
import cs451.vectorclock.VectorClock;

/**
 * First-in, first-out broadcast abstraction.
 */
class FIFOBroadcast implements Broadcast {

    /**
     * The underlying uniform reliable broadcast.
     */
    private final URBroadcast urBroadcast;

    /**
     * The local message tallying: used to reorder messages that have been
     * URB-delivered.
     */
    private final Map<Integer, VectorClock> delivered = new TreeMap<>();

    /**
     * The listener from the upper instance called when a message is effectively
     * FIFO-broadcast delivered.
     */
    private final BListener deliver;

    /**
     * The listener from the upper instance called when a local message is
     * effectively FIFO-broadcast to everyone.
     */
    private final IntConsumer broadcastListener;

    /**
     * The ID of the local instance, used to keep track of which local messages are
     * broadcast.
     */
    private final int myId;

    /**
     * Create a FIFO broadcast instance.
     *
     * @param port              The port number, used to build the underlying link.
     * @param hosts             The list of hosts, used to broadcast messages.
     * @param myId              The ID of the local host, to keep track of which
     *                          local messages are broadcast.
     * @param deliver           The listener used when a message is delivered.
     * @param broadcastListener The listener used when a local message is broadcast.
     */
    public FIFOBroadcast(int port, List<Host> hosts, int myId, BListener deliver, IntConsumer broadcastListener) {
        this.urBroadcast = new URBroadcast(port, hosts, myId, this::deliver);
        this.deliver = deliver;
        this.broadcastListener = broadcastListener;
        this.myId = myId;

        for (Host host : hosts) {
            delivered.put(host.getId(), new VectorClock());
        }
    }

    @Override
    public void broadcast(Message message) {
        urBroadcast.broadcast(message);
    }

    @Override
    public void broadcastRange(int numberOfMessages) {
        urBroadcast.broadcastRange(numberOfMessages);
    }

    /**
     * Called when a message is URB-delivered.
     *
     * @param message The message to deliver.
     */
    private void deliver(Message message) {
        int origin = message.getOriginId();
        int messageId = message.getMessageId();

        // Check if new messages can be delivered.
        int start = (int) delivered.get(origin).getStateOfVc();
        delivered.get(origin).addMember(messageId);
        int end = (int) delivered.get(origin).getStateOfVc();

        // Reconstruct all buffered messages.
        for (int i = start + 1; i <= end; ++i) {
            if (message.getOriginId() == myId) {
                broadcastListener.accept(i);
            }
            deliver.apply(Message.createMessage(origin, i));
        }
        if (startTime == 0) {
            startTime = System.nanoTime();
        }
        System.err.println(System.nanoTime() - startTime);
    }
    private long startTime = 0;
}
