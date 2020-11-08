package cs451.link;

import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import cs451.message.Message;
import cs451.vectorclock.MessageRange;

public class HostInfo {
    public static final long TIMEOUT_MS = 1000;

    private final BlockingQueue<WaitingPacket> stubbornQueue = new LinkedBlockingQueue<>();
    private final Map<Integer, MessageRange> waitingQueue = new TreeMap<>();
    private final Map<Integer, MessageRange> delivered = new TreeMap<>();
    private final AtomicLong currentTimeout = new AtomicLong(TIMEOUT_MS);

    private final int hostId;
    private final InetAddress address;
    private final int port;
    private final int windowSize;
    private final int numHosts;
    private int nextOriginToSend = 1;

    public HostInfo(InetAddress address, int port, int numHosts, int hostId) {
        this.address = address;
        this.port = port;
        this.hostId = hostId;
        this.windowSize = Link.WINDOW_SIZE / numHosts / 2;
        this.numHosts = numHosts;
        for (int i = 1; i <= numHosts; ++i) {
            waitingQueue.put(i, new MessageRange());
            delivered.put(i, new MessageRange());
        }
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public int getHostId() {
        return hostId;
    }

    public boolean canSendMessage() {
        return stubbornQueue.size() < windowSize;
    }

    public boolean isDelivered(Message m) {
        return delivered.get(m.getOriginId()).contains(m.getMessageId());
    }

    public void markDelivered(Message m) {
        delivered.get(m.getOriginId()).add(m.getMessageId());
    }

    public Map<Integer, MessageRange> getDelivered() {
        return delivered;
    }

    public WaitingPacket getNextStubborn() {
        return stubbornQueue.poll();
    }

    public void addPacketToConfirm(WaitingPacket wp) throws InterruptedException {
        stubbornQueue.put(wp);
    }

    public void addMessageInWaitingList(Message message) {
        waitingQueue.get(message.getOriginId()).add(message.getMessageId());
    }

    public void sendRange(int originId, int a, int b) {
        waitingQueue.get(originId).setRange(a, b);
    }

    public boolean canSendWaitingMessages() {
        return canSendMessage();
    }

    private Message getNextWaitingMessage(int hostId) {
        long mId = waitingQueue.get(hostId).poll();
        return mId < 0 ? null : Message.createMessage(hostId, (int) mId);
    }

    public Message getNextWaitingMessage() {
        int nextHost = nextOriginToSend;
        if (nextOriginToSend == numHosts) {
            nextOriginToSend = 1;
        } else {
            nextOriginToSend += 1;
        }
        return getNextWaitingMessage(nextHost);
    }

    public long getTimeout() {
        return currentTimeout.get();
    }

    public void resetTimeout() {
        currentTimeout.set(TIMEOUT_MS);
    }

    public void testAndDouble(long messageTimeout) {
        if (currentTimeout.get() < TIMEOUT_MS * 16) {
            currentTimeout.compareAndSet(messageTimeout, messageTimeout * 2);
        }
    }
}
