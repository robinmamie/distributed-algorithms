package cs451.broadcast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cs451.link.AbstractLink;
import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;
import cs451.vectorclock.BroadcastAcks;
import cs451.vectorclock.VectorClock;

/**
 * Uniform reliable broadcast abstraction. Delivers messages once it is sure
 * that more than half of all hosts have BEB-delivered the message.
 */
class URBroadcast implements Broadcast {

    /**
     * The underlying best effort broadcast.
     */
    private final BEBroadcast beBroadcast;

    /**
     * The tally of already URB-delivered messages.
     */
    private final Map<Integer, VectorClock> delivered = new HashMap<>();

    /**
     * The registers which hosts have BEB-delivered which messages.
     */
    private final BroadcastAcks acks;

    /**
     * The listener from the upper instance called when a message is effectively
     * UR-broadcast delivered.
     */
    private final BListener deliver;

    /**
     * Half of the number of hosts. Used to check if a message can be UR-broadcast.
     */
    private final int threshold;

    /**
     * Create a URB broadcast instance.
     *
     * @param port    The port number, used to build the underlying link.
     * @param hosts   The list of hosts, used to broadcast messages.
     * @param myId    The ID of the local host.
     * @param deliver The listener used when a message is delivered.
     */
    public URBroadcast(int port, List<Host> hosts, int myId, BListener deliver) {
        this.beBroadcast = new BEBroadcast(port, hosts, myId, this::deliver);
        this.acks = new BroadcastAcks(hosts.size(), myId, AbstractLink.getHostInfo());
        this.deliver = deliver;
        this.threshold = hosts.size() / 2;

        for (Host host : hosts) {
            delivered.put(host.getId(), new VectorClock());
        }
    }

    @Override
    public void broadcast(Message message) {
        beBroadcast.broadcast(message);
    }

    @Override
    public void broadcastRange(int numberOfMessages) {
        beBroadcast.broadcastRange(numberOfMessages);
    }

    /**
     * Called when a message is BEB-delivered.
     *
     * @param message The message to deliver.
     */
    private void deliver(Message message) {
        int origin = message.getOriginId();
        int messageId = message.getMessageId();
        if (!delivered.get(origin).contains(messageId)) {
            // If not already delivered, broadcast new message, or check if number of
            // acknowledgements is good to deliver said message.
            if (!acks.wasAlreadyBroadcast(message)) {
                acks.add(message);
                broadcast(message);
            } else {
                int count = acks.ackCount(message);
                if (count > threshold) {
                    delivered.get(origin).addMember(messageId);
                    deliver.apply(message);
                    if (start == 0) {
                        start = System.nanoTime();
                    }
                    System.err.println(System.nanoTime() - start);
                }
            }
        }
    }

    private long start = 0;
}
