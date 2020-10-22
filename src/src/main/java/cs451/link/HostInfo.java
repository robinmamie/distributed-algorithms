package cs451.link;

import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import cs451.message.Message;
import cs451.vectorclock.VectorClock;

class HostInfo {
    public static final long TIMEOUT_MS = 500;

    private final VectorClock vcAtDistantHost = new VectorClock();
    private final VectorClock vcOfMessagesFromDistantHost = new VectorClock();

    private final AtomicLong sentCounter = new AtomicLong(0L);
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
        return sentCounter.incrementAndGet();
    }

    public boolean canSendMessage() {
        return vcAtDistantHost.getStateOfVc() + SeqLink.WINDOW_SIZE > sentCounter.get();
    }

    public WaitingPacket getNextStubborn() {
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

    public boolean hasAckedPacket(long seqNumber) {
        return vcAtDistantHost.isPast(seqNumber);
    }

    public void updateReceiveVectorClock(long messageSeqNumber) {
        vcAtDistantHost.addMember(messageSeqNumber);
    }

    public boolean hasReceivedMessage(long seqNumber) {
        return vcOfMessagesFromDistantHost.isPast(seqNumber);
    }

    public void updateLocalReceiveVectorClock(long messageSeqNumber) {
        vcOfMessagesFromDistantHost.addMember(messageSeqNumber);
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
}
