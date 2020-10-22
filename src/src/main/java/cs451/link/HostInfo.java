package cs451.link;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import cs451.message.Message;

class HostInfo {
    public static final long TIMEOUT_MS = 200;

    // TODO find the usefulness of the vector clocks, not yet clear at this level
    private final AtomicLong receivedVectorClock = new AtomicLong(0L);
    private final AtomicLong sentVectorClock = new AtomicLong(0L);
    private final Set<Long> pendingVectorClock = ConcurrentHashMap.newKeySet();
    private final BlockingQueue<WaitingPacket> stubbornQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> waitingQueue = new LinkedBlockingQueue<>();
    private final AtomicLong currentTimeout = new AtomicLong(TIMEOUT_MS);

    private final InetAddress address;
    private final int port;

    public HostInfo(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public long getNextSeqNumber() {
        return sentVectorClock.incrementAndGet();
    }

    public boolean canSendMessage() {
        return stubbornQueue.size() < SeqLink.WINDOW_SIZE;
    }

    public WaitingPacket getNextStubborn() {
        // TODO synchronize on poll?
        return stubbornQueue.poll();
    }

    public void addPacketToConfirm(WaitingPacket wp) throws InterruptedException {
        stubbornQueue.put(wp);
    }

    public void addMessageInWaitingList(Message message) throws InterruptedException {
        waitingQueue.put(message);
    }

    public boolean canSendWaitingMessages() {
        return canSendMessage() && waitingQueue.size() > 0;
    }

    public Message getNextWaitingMessage() {
        return waitingQueue.poll();
    }

    public boolean hasAckedPacket(WaitingPacket wp) {
        return wp.getMessage().getSeqNumber() <= receivedVectorClock.get()
            || pendingVectorClock.contains(wp.getMessage().getSeqNumber());
    }

    public void updateReceiveVectorClock(long messageSeqNumber) {
        synchronized (pendingVectorClock) {
            if (messageSeqNumber == receivedVectorClock.get() + 1) {
                long got;
                do {
                    got = receivedVectorClock.incrementAndGet();
                    pendingVectorClock.remove(got);
                } while (pendingVectorClock.contains(got));
            } else {
                pendingVectorClock.add(messageSeqNumber);
            }
        }
    }

    public long getTimeout() {
        return currentTimeout.get();
    }

    public void resetTimeout() {
        currentTimeout.set(TIMEOUT_MS);
    }

    public void testAndDouble(long messageTimeout) {
        currentTimeout.compareAndSet(messageTimeout, messageTimeout*2);
    }

    @Override
    public String toString() {
        return "Number of waiting messages: " + waitingQueue.size();
    }
}
