package cs451.link;

import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import cs451.message.Message;
import cs451.vectorclock.MessageRange;

/**
 * Gathers all the information collected by the link layer.
 */
public class HostInfo {

    private final BlockingQueue<WaitingPacket> stubbornQueue = new LinkedBlockingQueue<>();
    private final Map<Integer, MessageRange> waitingQueue = new TreeMap<>();
    private final Map<Integer, MessageRange> delivered = new TreeMap<>();
    private final AtomicLong currentTimeout = new AtomicLong(Link.TIMEOUT_MS);

    private final InetAddress address;
    private final int port;
    private final int windowSize;
    private final int numHosts;
    private int nextOriginToSend = 1;

    /**
     * Create a new HostInfo instance. The local window size, i.e. the max. number
     * of "stubborn" (not acked) messages for this host, depends on the number of
     * hosts in the topology.
     *
     * @param address  The address of the host.
     * @param port     The port number of the host.
     * @param numHosts The number of hosts in the topology.
     */
    public HostInfo(InetAddress address, int port, int numHosts) {
        this.address = address;
        this.port = port;
        this.windowSize = Link.WINDOW_SIZE / numHosts / 2;
        this.numHosts = numHosts;
        for (int i = 1; i <= numHosts; ++i) {
            waitingQueue.put(i, new MessageRange());
            delivered.put(i, new MessageRange());
        }
    }

    /**
     * Get the address of the host.
     *
     * @return The address of the host.
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Get the port number of the host.
     *
     * @return The port number of the host.
     */
    public int getPort() {
        return port;
    }

    /**
     * Check whether a given message was already delivered, coming from this host.
     *
     * @param m The message to check.
     * @return Whether the given message was already delivered.
     */
    public boolean isDelivered(Message m) {
        return delivered.get(m.getOriginId()).contains(m.getMessageId());
    }

    /**
     * Mark a given message as delivered, coming from this host.
     *
     * @param m The message to mark as delivered.
     */
    public void markDelivered(Message m) {
        delivered.get(m.getOriginId()).add(m.getMessageId());
    }

    /**
     * Get the map of delivered messages, associating an origin to the message IDs
     * already delivered.
     *
     * @return The map of delivered messages.
     */
    public Map<Integer, MessageRange> getDelivered() {
        return delivered;
    }

    /**
     * Get the next "stubborn" (not acked) message.
     *
     * @return The next stubborn message.
     */
    public WaitingPacket getNextStubborn() {
        return stubbornQueue.poll();
    }

    /**
     * Add a given WaitingPacket to the "stubborn" (not acked) queue.
     *
     * @param wp The waiting packet to add to the stubborn queue.
     * @throws InterruptedException
     */
    public void addPacketToConfirm(WaitingPacket wp) throws InterruptedException {
        stubbornQueue.put(wp);
    }

    /**
     * Store a new message to the waiting list, i.e. a message not yet sent, waiting
     * that the window is big enough.
     *
     * @param message The message to add to the waiting list.
     */
    public void addMessageInWaitingList(Message message) {
        waitingQueue.get(message.getOriginId()).add(message.getMessageId());
    }

    /**
     * "Send" a range of messages, i.e. add the range to the WaitingList.
     *
     * @param originId The origin ID of th message.
     * @param a        The first value of the message IDs (generally 1).
     * @param b        The last value of the message IDs (generally the number of
     *                 messages to broadcast).
     */
    public void sendRange(int originId, int a, int b) {
        waitingQueue.get(originId).setRange(a, b);
    }

    public boolean canSendWaitingMessages() {
        return stubbornQueue.size() < windowSize;
    }

    public Message getNextWaitingMessage() {
        int nextHost = nextOriginToSend;
        if (nextOriginToSend == numHosts) {
            nextOriginToSend = 1;
        } else {
            nextOriginToSend += 1;
        }

        int mId = waitingQueue.get(nextHost).poll();
        return mId < 0 ? null : Message.createMessage(nextHost, mId);
    }

    public long getTimeout() {
        return currentTimeout.get();
    }

    public void resetTimeout() {
        currentTimeout.set(Link.TIMEOUT_MS);
    }

    public void testAndDouble(long messageTimeout) {
        if (currentTimeout.get() < Link.TIMEOUT_MS * 16) {
            currentTimeout.compareAndSet(messageTimeout, messageTimeout * 2);
        }
    }
}
