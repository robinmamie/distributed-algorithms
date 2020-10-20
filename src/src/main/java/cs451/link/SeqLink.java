package cs451.link;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.logger.Logger;
import cs451.message.Message;

public class SeqLink extends AbstractLink {

    public static final long TIMEOUT_MS = 100;

    private final DatagramSocket socket;
    private final BlockingQueue<DatagramPacket> sendQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<>();

    private final BlockingQueue<CustomPacket> stubbornQueue = new LinkedBlockingQueue<>();
    private final Set<CustomPacket> ackSet = ConcurrentHashMap.newKeySet();

    private final Set<Message.IntTriple> delivered = ConcurrentHashMap.newKeySet();

    private final int myId;

    //private final Map<Integer, Long> messageClock = new ConcurrentHashMap<>();

    public SeqLink(int port, int numHosts, int myId) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        // for (int i = 1; i <= numHosts; ++i) {
        //     messageClock.put(i, 0L);
        // }

        this.myId = myId;

        new Thread(() -> listen(port)).start();
        new Thread(() -> sendPackets()).start();
        new Thread(() -> stubbornSend()).start();
        for (int i = 0; i < 1; ++i) {
            new Thread(() -> handleListenersLowLevel()).start();
        }
    }

    @Override
    public void send(Message message, InetAddress address, int port) {
        byte[] buf = message.serialize();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        try {
            // Fair-loss link
            sendQueue.put(packet);
            // Stubborn link
            stubbornQueue.put(new CustomPacket(message, address, port));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void stubbornSend() {
        while (true) {
            CustomPacket cp;
            try {
                cp = stubbornQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }

            if (ackSet.contains(cp)) {
                ackSet.remove(cp);
            } else {
                try {
                    if (cp.shouldResend()) {
                        byte[] buf = cp.message.serialize();
                        sendQueue.put(new DatagramPacket(buf, buf.length, cp.address, cp.port));
                    }
                    stubbornQueue.put(cp);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
        Logger logger = new Logger(this);
        while (true) {
            // Fair-loss link part
            DatagramPacket packet;
            try {
                logger.log("END: " + receiveQueue.size());
                packet = receiveQueue.take();
                logger.log("STA: " + receiveQueue.size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            byte[] datagram = packet.getData();
            Message m = Message.deserialize(datagram);

            // Stubborn link part
            if (m.isAck()) {
                ackSet.add(new CustomPacket(m, packet.getAddress(), packet.getPort()));
            } else {
                send(m.toAck(myId), packet.getAddress(), packet.getPort());
            }
            Message.IntTriple id = m.getFullId();
            if (!delivered.contains(id)) {
                delivered.add(id);
                handleListeners(m, packet.getAddress(), packet.getPort());
            }
            //handleListeners(message, packet.getAddress(), packet.getPort());
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
                && this.message.equals(((CustomPacket)that).message)
                && this.address.equals(((CustomPacket)that).address)
                && this.port == ((CustomPacket)that).port;
        }
    }
}
