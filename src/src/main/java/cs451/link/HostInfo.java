package cs451.link;

import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import cs451.message.Message;
import cs451.message.MessageRange;
import cs451.vectorclock.VectorClock;

class HostInfo {
    public static final long TIMEOUT_MS = 1000;

    private final VectorClock vcAtDistantHost = new VectorClock();
    private final VectorClock vcOfMessagesFromDistantHost = new VectorClock();

    private final AtomicLong sentCounter = new AtomicLong(0L);
    private final BlockingQueue<WaitingPacket> stubbornQueue = new LinkedBlockingQueue<>();
    private final Map<Integer, MessageRange> waitingQueue = new TreeMap<>();
    private final AtomicLong currentTimeout = new AtomicLong(TIMEOUT_MS);

    private final InetAddress address;
    private final int port;
    private final int windowSize;

    public HostInfo(InetAddress address, int port, int numHosts) {
        this.address = address;
        this.port = port;
        this.windowSize = Link.WINDOW_SIZE / numHosts;
        for (int i = 1; i <= numHosts; ++i) {
            waitingQueue.put(i, new MessageRange());
        }
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
        return stubbornQueue.size() < windowSize;//sentCounter.get() - windowSize < vcAtDistantHost.getStateOfVc();
    }

    public WaitingPacket getNextStubborn() {
        return stubbornQueue.poll();
    }

    public void addPacketToConfirm(WaitingPacket wp) throws InterruptedException {
        stubbornQueue.put(wp);
    }

    public void addMessageInWaitingList(Message message) throws InterruptedException {
        waitingQueue.get(message.getOriginId()).add(message.getMessageId());
    }

    public void sendRange(int originId, long a, long b) {
        waitingQueue.get(originId).setRange(a, b);
    }

    public int peekNextToSend(int originId) {
        return (int)waitingQueue.get(originId).peek();
    }

    public boolean canSendWaitingMessages() {
        return canSendMessage();
    }

    private Message getNextWaitingMessage(int hostId) {
        long mId = waitingQueue.get(hostId).poll();
        if (mId < 0) {
            return null;
        }
        return Message.createMessage(hostId, (int)mId);
    }

    public Message getNextWaitingMessage() {
        int index = -1;
        long min = Long.MAX_VALUE;
        for (Map.Entry<Integer, MessageRange> me: waitingQueue.entrySet()) {
            long candidate = me.getValue().peek();
            if (candidate < 0) {
                continue;
            } else if (candidate < min) {
                index = me.getKey();
                min = candidate;
            }
        }
        if (index == -1) {
            return null;
        }
        return getNextWaitingMessage(index);
    }

    public boolean hasAckedPacket(long seqNumber) {
        return vcAtDistantHost.isPast(seqNumber);
    }

    public void updateReceiveVectorClock(long messageSeqNumber) {
        vcAtDistantHost.addMember(messageSeqNumber);
        //System.out.println("distant: " + vcAtDistantHost.getRange());
    }

    public boolean hasReceivedMessage(long seqNumber) {
        return vcOfMessagesFromDistantHost.isPast(seqNumber);
    }

    public void updateLocalReceiveVectorClock(long messageSeqNumber) {
        vcOfMessagesFromDistantHost.addMember(messageSeqNumber);
        //System.out.println("local: " + vcOfMessagesFromDistantHost.getRange());
    }

    public long getTimeout() {
        return currentTimeout.get();
    }

    public void resetTimeout() {
        currentTimeout.set(TIMEOUT_MS);
    }

    public void testAndDouble(long messageTimeout) {
        currentTimeout.compareAndSet(messageTimeout, messageTimeout * 2);
    }
}
