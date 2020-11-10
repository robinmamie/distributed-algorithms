package cs451.link;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
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

    /**
     * The "stubborn" queue, i.e. messages sent to this host that have not been
     * acked yet.
     */
    private final BlockingQueue<WaitingPacket> stubbornQueue = new LinkedBlockingQueue<>(Link.WINDOW_SIZE);

    /**
     * The queue of waiting messages, to be emptied once the stubborn queue is small
     * enough, i.e. smaller than the window size.
     */
    private final Map<Integer, MessageRange> waitingQueue = new TreeMap<>();

    /**
     * The messages already delivered from this host, i.e. messages that had this
     * host as last hop.
     */
    private final Map<Integer, MessageRange> delivered = new TreeMap<>();

    /**
     * The current timeout of this host.
     */
    private final AtomicLong currentTimeout = new AtomicLong(Link.TIMEOUT_MS);

    /**
     * The address of this host.
     */
    private final InetAddress address;

    /**
     * The port number of this host.
     */
    private final int port;

    /**
     * The amount of messages that can be sent to this host
     */
    private final int windowSize;

    /**
     * The total number of hosts in the topology.
     */
    private final int numHosts;

    /**
     * The origin ID cycle, used to retrieve the next waiting message.
     */
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
        // We divide the window by 2 to take acks into account.
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
     * Get the next "stubborn" (not acked) messages.
     *
     * @return The list of next stubborn messages.
     */
    public List<WaitingPacket> getNextStubbornPackets() {
        List<WaitingPacket> stubbornPackets = new LinkedList<>();
        stubbornQueue.drainTo(stubbornPackets);
        return stubbornPackets;
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

    /**
     * Check whether we can send a new message, i.e. if the size of the "stubborn"
     * queues (messages not yet acked) is less than the window size.
     *
     * @return Whether we can send a new message.
     */
    public boolean canSendWaitingMessages() {
        return stubbornQueue.size() < windowSize;
    }

    /**
     * Retrieve the next waiting message from the waiting queue. Origins are chosen
     * cyclically.
     *
     * @return A new message to "stubborn" send.
     */
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

    /**
     * Get the current timeout of the host, in milliseconds.
     *
     * @return The current timeout, in milliseconds.
     */
    public long getTimeout() {
        return currentTimeout.get();
    }

    /**
     * Reset the value of this host's timeout to the baseline.
     */
    public void resetTimeout() {
        currentTimeout.set(Link.TIMEOUT_MS);
    }

    /**
     * Test the timeout of this current hosts, and if it checks out and is not equal
     * or greater than 16 times the baseline value, double it.
     *
     * @param messageTimeout The supposed current timeout. It will only double if
     *                       this value and the saved timeout match up.
     */
    public void testAndDouble(long messageTimeout) {
        if (currentTimeout.get() < Link.TIMEOUT_MS * 16) {
            currentTimeout.compareAndSet(messageTimeout, messageTimeout * 2);
        }
    }
}
