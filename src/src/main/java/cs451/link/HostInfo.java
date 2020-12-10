package cs451.link;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import cs451.message.Message;
import cs451.message.Packet;
import cs451.vectorclock.MessageRange;
import cs451.vectorclock.VectorClock;

/**
 * Gathers all the information collected by the link layer.
 */
public class HostInfo {

    /**
     * The "stubborn" queue, i.e. messages sent to this host that have not been
     * acked yet.
     */
    private final BlockingQueue<WaitingPacket> stubbornQueue = new LinkedBlockingQueue<>();

    /**
     * The queue of waiting messages, to be emptied once the stubborn queue is small
     * enough, i.e. smaller than the window size.
     */
    private final Map<Integer, BlockingQueue<Message>> waitingQueue = new TreeMap<>();

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
     * The number of RTTs saved for the timeout computation.
     */
    private static final int SAVED_RTTS = 5;

    /**
     * The list of the last SAVED_RTTS for the timeout computation.
     */
    private final Queue<Long> lastRTTs = new LinkedList<>();

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
     * The count of used packetNumbers for this hist.
     */
    private final AtomicInteger packetNumbersSent = new AtomicInteger(0);

    /**
     * The vector clock of packet numbers originating from the local host and that
     * have been delivered by the distant host. Used to stop stubbornly resending
     * packets.
     */
    private final VectorClock myPacketNumberDelivered = new VectorClock();

    /**
     * The vector clock of packet numbers originating from the distant host and that
     * have been locally delivered.
     */
    private final VectorClock theirPacketNumberDelivered = new VectorClock();

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
        this.windowSize = Math.max(1, Link.WINDOW_SIZE / numHosts / 2);
        this.numHosts = numHosts;
        for (int i = 1; i <= numHosts; ++i) {
            waitingQueue.put(i, new LinkedBlockingQueue<>());
            delivered.put(i, new MessageRange());
        }
        for (int i = 0; i < SAVED_RTTS; i++) {
            lastRTTs.add(Link.TIMEOUT_MS);
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
     * Check whether a given packet was already delivered.
     *
     * @param p The packet to check.
     * @return Whether the given packet was already delivered.
     */
    public boolean isDelivered(Packet p) {
        return p.isAck() ? myPacketNumberDelivered.contains(p.getPacketNumber())
                : theirPacketNumberDelivered.contains(p.getPacketNumber());
    }

    /**
     * Check whether a given packet was already delivered, coming from this host.
     *
     * @param p The packet to check.
     * @return Whether the given packet was already delivered.
     */
    public boolean isMineDelivered(Packet p) {
        return myPacketNumberDelivered.contains(p.getPacketNumber());
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
     * Mark a given packet as delivered, coming from this host.
     *
     * @param p The packet to mark as delivered.
     */
    public void markDelivered(Packet p) {
        if (p.isAck()) {
            myPacketNumberDelivered.addMember(p.getPacketNumber());
        } else {
            theirPacketNumberDelivered.addMember(p.getPacketNumber());
        }
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
     * Atomically return a new packet number.
     * 
     * @return A new packet number.
     */
    public int getNewPacketNumber() {
        return packetNumbersSent.incrementAndGet();
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
        try {
            waitingQueue.get(message.getOriginId()).put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
        throw new RuntimeException("Obsolete FIFO implementation.");
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
        int nextHost = 1;
        Message message;
        for (int i = 1; i <= numHosts; ++i) {
            nextHost = nextOriginToSend;
            if (nextOriginToSend == numHosts) {
                nextOriginToSend = 1;
            } else {
                nextOriginToSend += 1;
            }
            message = waitingQueue.get(nextHost).poll();
            if (message != null) {
                return message;
            }
        }
        return null;
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
     * Reset the value of this host's timeout by adding the reported RTT to the list
     * of most recent RTTs for this host.
     * 
     * @param packet The packet reporting the timeout.
     */
    public void resetTimeout(Packet packet) {
        setTimeout(packet.getAgeInMs());
    }

    /**
     * Add double the actual timeout to the list of most recent RTTs for this host.
     * This method will therefore not actually double the actual RTT.
     */
    public void exponentialBackOff() {
        long timeout = getTimeout();
        setTimeout(Math.min(timeout * 2, Link.MAX_TIMEOUT));
    }

    /**
     * Adds the timeout to the list, and computes a new timeout value.
     *
     * @param messageTimeout The reported timeout, in ms.
     */
    private void setTimeout(long messageTimeout) {
        synchronized (lastRTTs) {
            lastRTTs.poll();
            lastRTTs.add(messageTimeout);
            long newTimeout = 0;
            for (Long t : lastRTTs) {
                newTimeout += t;
            }
            newTimeout /= lastRTTs.size();
            // Add 50ms for processing purposes, and to avoid any DDOS.
            newTimeout += 50;
            currentTimeout.set(newTimeout);
        }
    }

    public int numberOfWaitingMessages() {
        int size = 0;
        for (Map.Entry<Integer, BlockingQueue<Message>> e : waitingQueue.entrySet()) {
            size += e.getValue().size();
        }
        return size;
    }
}
