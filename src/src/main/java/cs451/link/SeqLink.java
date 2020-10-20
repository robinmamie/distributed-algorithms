package cs451.link;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import cs451.message.Message;

public class SeqLink extends AbstractLink {

    public static final long TIMEOUT_MS = 200;
    private static final int WINDOW_SIZE = 100;

    private final DatagramSocket socket;
    private final int myId;
    private final int numHosts;

    private final Set<Message.IntTriple> delivered = ConcurrentHashMap.newKeySet();

    private final Map<Integer, AtomicLong> sentVectorClock = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicLong> gotVectorClock = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Long>> pendingSeq = new ConcurrentHashMap<>();

    private final Map<Integer, BlockingDeque<CustomPacket>> sendQueues = new ConcurrentHashMap<>();
    private final Map<Integer, BlockingQueue<DatagramPacket>> waitingQueues = new ConcurrentHashMap<>();

    private final BlockingQueue<DatagramPacket> sendQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<>();

    // private final Map<Integer, Long> messageClock = new ConcurrentHashMap<>();

    public SeqLink(int port, int numHosts, int myId) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        for (int i = 1; i <= numHosts; ++i) {
            sentVectorClock.put(i, new AtomicLong(0));
            gotVectorClock.put(i, new AtomicLong(0));
            pendingSeq.put(i, ConcurrentHashMap.newKeySet());
            sendQueues.put(i, new LinkedBlockingDeque<>());
            waitingQueues.put(i, new LinkedBlockingQueue<>());
        }

        this.myId = myId;
        this.numHosts = numHosts;

        new Thread(() -> listen(port)).start();
        new Thread(() -> sendPackets()).start();
        new Thread(() -> stubbornSend()).start();
        for (int i = 0; i < 3; ++i) {
            new Thread(() -> handleListenersLowLevel()).start();
        }
    }

    @Override
    public void send(Message message, int hostId, InetAddress address, int port) {
        try {
            // EXPERIMENTAL - message already received from them, so they either got an ACK or we already sent them! No need to resend.
            if (delivered.contains(new Message.IntTriple(message.getOriginId(), message.getMessageId(), hostId))) {
                return;
            }
            CustomPacket cp = new CustomPacket(message, address, port);
            synchronized (sentVectorClock.get(hostId)) {
                byte[] buf = message.serialize();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                if (sentVectorClock.get(hostId).get() < gotVectorClock.get(hostId).get() + WINDOW_SIZE) {
                    cp.message = cp.message.changeSeqNumber(sentVectorClock.get(hostId).incrementAndGet());
                    buf = cp.message.serialize();
                    packet = new DatagramPacket(buf, buf.length, address, port);
                    sendQueue.put(packet);
                    sendQueues.get(hostId).put(cp);
                } else {
                    waitingQueues.get(hostId).put(packet);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void stubbornSend() {
        for (int hostId = 1;; ++hostId) {

            if (hostId > numHosts) {
                hostId = 1;
            }

            BlockingDeque<CustomPacket> mySendQueue = sendQueues.get(hostId);

            CustomPacket cp;
            cp = mySendQueue.poll();

            if (cp != null
                    && cp.message.getSeqNumber() > gotVectorClock.get(hostId).get()
                    && !pendingSeq.get(hostId).contains(cp.message.getSeqNumber())) {
                try {
                    if (cp.shouldResend()) { // receiveQueue.size() < WINDOW_SIZE && 
                        byte[] buf = cp.message.serialize();
                        sendQueue.put(new DatagramPacket(buf, buf.length, cp.address, cp.port));
                    }
                    mySendQueue.put(cp);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public void sendPackets() {
        try {
            while (true) {
                DatagramPacket packet = sendQueue.take();
                socket.send(packet);
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot send packets!");
        }
    }

    private void listen(int port) {
        try {
            while (true) {
                byte[] buf = new byte[UDP_SAFE_PACKET_MAX_SIZE];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                receiveQueue.put(packet);
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot receive packets!");
        }
    }

    private void handleListenersLowLevel() {
        while (true) {
            DatagramPacket packet;
            try {
                packet = receiveQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            byte[] datagram = packet.getData();
            Message m = Message.deserialize(datagram);

            // Stubborn link part
            int hostId = m.getLastHop();
            if (m.isAck()) {
                AtomicLong gotRegister = gotVectorClock.get(hostId);
                if (m.getSeqNumber() == gotRegister.get() + 1) {
                    long got = gotRegister.incrementAndGet();
                    Set<Long> pending = pendingSeq.get(hostId);
                    while (pending.contains(got)) {
                        got = gotRegister.incrementAndGet();
                        pending.remove(got);
                    }
                } else {
                    pendingSeq.get(hostId).add(m.getSeqNumber());
                }

                // Send messages until WINDOW_SIZE
                BlockingQueue<DatagramPacket> wq = waitingQueues.get(hostId);
                synchronized (sentVectorClock.get(hostId)) {
                    while (wq.size() > 0
                            && sendQueues.get(hostId).size() < WINDOW_SIZE) {
                        DatagramPacket newPacket = wq.poll();
                        Message newM = Message.deserialize(newPacket.getData());
                        CustomPacket cp = new CustomPacket(newM, newPacket.getAddress(), newPacket.getPort());
                        cp.message = cp.message.changeSeqNumber(sentVectorClock.get(hostId).incrementAndGet());
                        byte[] buf = cp.message.serialize();
                        newPacket = new DatagramPacket(buf, buf.length, newPacket.getAddress(),
                                newPacket.getPort());
                        try {
                            sendQueue.put(newPacket);
                            sendQueues.get(hostId).put(cp);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            } else {
                byte[] buf = m.toAck(myId).serialize();
                try {
                    sendQueue.put(new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            Message.IntTriple id = m.getFullId();
            if (!delivered.contains(id)) {
                delivered.add(id);
                handleListeners(m, packet.getAddress(), packet.getPort());
            }
        }
    }

    private class CustomPacket {
        public Message message;
        public InetAddress address;
        public int port;
        public long timestamp;
        public long timeout;

        public CustomPacket(Message m, InetAddress a, int p) {
            message = m;
            address = a;
            port = p;
            timestamp = System.currentTimeMillis();
            timeout = TIMEOUT_MS;
        }

        private void resetTimestamp() {
            timestamp = System.currentTimeMillis();
        }

        public boolean shouldResend() {
            if (System.currentTimeMillis() - timestamp < timeout) {
                return false;
            }
            timeout *= 2;
            resetTimestamp();
            return true;
        }

        @Override
        public boolean equals(Object that) {
            return (that instanceof CustomPacket)
                && this.message.getOriginId() == ((CustomPacket)that).message.getOriginId()
                && this.message.getMessageId() == ((CustomPacket)that).message.getMessageId()
                && this.address.toString().equals(((CustomPacket)that).address.toString())
                && this.port == ((CustomPacket)that).port;
        }
    }
}
