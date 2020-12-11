package cs451.broadcast;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

/**
 * Localized causal broadcast abstraction. Implements the validity, no
 * duplication, no creation, uniform agreement, FIFO and (localized) causal
 * properties.
 */
public class LCausalBroadcast implements Broadcast {

    /**
     * The underlying fifo broadcast.
     */
    private final URBroadcast urBroadcast;

    /**
     * The local message tallying: used to reorder messages that have been
     * URB-delivered. It stores the dependencies mentioned in the message.
     */
    private final Map<Integer, Map<Integer, List<Integer>>> pending = new TreeMap<>();

    /**
     * The vector clocks of the current delivery of each process.
     */
    private final Map<Integer, AtomicInteger> delivered = new TreeMap<>();

    /**
     * The listener from the upper instance called when a message is effectively
     * LC-broadcast delivered.
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
     * The map of inter-process dependencies.
     */
    private final Map<Integer, List<Integer>> dependencies;

    /**
     * 
     * Create a FIFO broadcast instance.
     *
     * @param port              The port number, used to build the underlying link.
     * @param hosts             The list of hosts, used to broadcast messages.
     * @param myId              The ID of the local host, to keep track of which
     *                          local messages are broadcast.
     * @param deliver           The listener used when a message is delivered.
     * @param broadcastListener The listener used when a local message is broadcast.
     * @param dependencies      The map of inter-process dependencies.
     */
    public LCausalBroadcast(int port, List<Host> hosts, int myId, BListener deliver, IntConsumer broadcastListener,
            Map<Integer, List<Integer>> dependencies) {
        this.urBroadcast = new URBroadcast(port, hosts, myId, this::deliver);
        this.deliver = deliver;
        this.broadcastListener = broadcastListener;
        this.myId = myId;
        this.dependencies = dependencies;

        for (Host host : hosts) {
            pending.put(host.getId(), new TreeMap<>());
        }
        for (Host host : hosts) {
            delivered.put(host.getId(), new AtomicInteger(0));
        }
    }

    @Override
    public void broadcast(Message message) {
        List<Integer> dependencyIds = new LinkedList<>();
        List<Integer> dependency = dependencies.get(myId);

        synchronized (dependencies) {
            for (Integer i : dependency) {
                dependencyIds.add(delivered.get(i).get());
            }
            broadcastListener.accept(message.getMessageId());
        }

        message = message.addCausality(dependencyIds);
        urBroadcast.broadcast(message);
    }

    @Override
    public void broadcastRange(int numberOfMessages) {
        throw new RuntimeException("Should not be called.");
    }

    /**
     * Called when a message is URB-delivered.
     *
     * @param message The message to deliver.
     */
    private void deliver(Message message) {
        pending.get(message.getOriginId()).put(message.getMessageId(), message.getDependencies());
        int count = 1;
        while (count > 0) {
            count = 0;
            for (Map.Entry<Integer, List<Integer>> entry : dependencies.entrySet()) {
                count += checkPendingQueue(entry.getKey());
            }
        }
    }

    /**
     * Checks the pending messages' queue of the given process.
     *
     * @param originId The given process to check.
     * @return The number of messages delivered from the given process.
     */
    private int checkPendingQueue(int originId) {
        Map<Integer, List<Integer>> messages = pending.get(originId);

        List<Integer> dependency = dependencies.get(originId);
        int nextIdToDeliver = delivered.get(originId).get();
        int nbMessagesDelivered = 0;
        while (!messages.isEmpty()) {
            nextIdToDeliver += 1;

            // Messages always depend on the process ("FIFO")
            if (!messages.containsKey(nextIdToDeliver)) {
                return nbMessagesDelivered;
            }

            // Check the dependencies on *other* processes (LCausal)
            for (int i = 0; i < dependency.size(); ++i) {
                int deliveredId = delivered.get(dependency.get(i)).get();
                int requiredId = messages.get(nextIdToDeliver).get(i);
                if (deliveredId < requiredId) {
                    return nbMessagesDelivered;
                }
            }

            // Deliver next message
            synchronized (dependencies) {
                delivered.get(originId).incrementAndGet();
                deliver.apply(Message.createMessage(originId, nextIdToDeliver));
            }
            messages.remove(nextIdToDeliver); // garbage collecting
            nbMessagesDelivered += 1;
        }
        return nbMessagesDelivered;
    }
}
