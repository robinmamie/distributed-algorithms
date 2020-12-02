package cs451.broadcast;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

public class LCausalBroadcast implements Broadcast {

    /**
     * The underlying fifo broadcast.
     */
    private final URBroadcast urBroadcast;

    /**
     * The local message tallying: used to reorder messages that have been
     * URB-delivered.
     */
    private final Map<Integer, BlockingQueue<Message>> pending = new TreeMap<>();

    /**
     * The local message tallying: used to reorder messages that have been
     * URB-delivered.
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

    private final Map<Integer, List<Integer>> dependencies;

    public LCausalBroadcast(int port, List<Host> hosts, int myId, BListener deliver, IntConsumer broadcastListener,
            Map<Integer, List<Integer>> dependencies) {
        this.urBroadcast = new URBroadcast(port, hosts, myId, this::deliver);
        this.deliver = deliver;
        this.broadcastListener = broadcastListener;
        this.myId = myId;
        this.dependencies = dependencies;

        for (Host host : hosts) {
            pending.put(host.getId(), new LinkedBlockingQueue<>());
        }
        for (Host host : hosts) {
            delivered.put(host.getId(), new AtomicInteger(0));
        }
    }

    @Override
    public void broadcast(Message message) {
        List<Integer> dependencyIds = new LinkedList<>();
        List<Integer> dependency = dependencies.get(myId);
        // TODO synchronize on delivery!
        synchronized (dependency) {
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
        pending.get(message.getOriginId()).add(message);
        for (Map.Entry<Integer, BlockingQueue<Message>> entry : pending.entrySet()) {
            List<Message> messages = new LinkedList<>();
            entry.getValue().drainTo(messages);
            messages.sort(Comparator.comparing(Message::getMessageId));
            checkDependency(entry.getKey(), messages);
        }
    }

    private void checkDependency(int originId, List<Message> messages) {
        List<Integer> dependency = dependencies.get(originId);
        int count = 0;
        for (Message message : messages) {
            if (message.getMessageId() - 1 != delivered.get(originId).get()) {
                pending.get(originId).addAll(messages.subList(count, messages.size()));
                return;
            }
            for (int i = 0; i < dependency.size(); ++i) {
                int deliveredId = delivered.get(dependency.get(i)).get();
                int requiredId = message.getDependencies().get(i);
                if (deliveredId < requiredId) {
                    pending.get(originId).addAll(messages.subList(count, messages.size()));
                    return;
                }
            }
            synchronized (dependency) {
                delivered.get(originId).incrementAndGet();
                deliver.apply(message);
            }
            count += 1;
        }
    }
}
