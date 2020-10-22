package cs451.link;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.logger.Logger;
import cs451.message.Message;
import cs451.parser.Host;

public class SeqLink extends AbstractLink {

    public static final int WINDOW_SIZE = 100;

    private final DatagramSocket socket;
    private final int myId;

    private final Set<Message.IntTriple> delivered = ConcurrentHashMap.newKeySet();

    private final BlockingQueue<DatagramPacket> sendQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<>();

    private final Map<Integer, HostInfo> hostInfo = new HashMap<>();

    // private final Map<Integer, Long> messageClock = new ConcurrentHashMap<>();

    public SeqLink(int port, List<Host> hosts, int myId) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        for (Host host : hosts) {
            int i = host.getId();
            if (i != myId) {
                try {
                    hostInfo.put(i, new HostInfo(InetAddress.getByName(host.getIp()), host.getPort()));
                } catch (UnknownHostException e) {
                    throw new RuntimeException("Invalid IP address given!");
                }
            }
        }

        this.myId = myId;

        new Thread(() -> listen(port)).start();
        new Thread(() -> sendPackets()).start();
        new Thread(() -> stubbornSend()).start();
        for (int i = 0; i < 1; ++i) {
            // FIXME concurrency not good!!
            new Thread(() -> handleListenersLowLevel()).start();
        }
    }

    @Override
    public void send(Message message, int hostId, InetAddress address, int port) {
        try {
            // EXPERIMENTAL - message already received from them, so they either got an ACK
            // or we already sent them! No need to resend.
            if (delivered.contains(new Message.IntTriple(message.getOriginId(), message.getMessageId(), hostId))) {
                return;
            }

            // Set sequence number to message
            HostInfo host = hostInfo.get(hostId);
            long seqNumber = host.getNextSeqNumber();
            Message seqMessage = message.changeSeqNumber(seqNumber);

            synchronized (host) {
                if (host.canSendMessage()) {
                    sendNewMessage(seqMessage, host);
                } else {
                    host.addMessageInWaitingList(seqMessage);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // -- MESSAGE OUT --

    private void sendNewMessage(Message message, HostInfo host) throws InterruptedException {
        WaitingPacket wp = new WaitingPacket(message, host);
        sendMessage(wp, host);
        host.addPacketToConfirm(wp);
    }

    private void sendMessage(WaitingPacket wp, HostInfo host) throws InterruptedException {
        sendMessage(wp.getMessage(), host.getAddress(), host.getPort());
    }

    private void sendMessage(Message message, HostInfo host) throws InterruptedException {
        sendMessage(message, host.getAddress(), host.getPort());
    }

    private void sendMessage(Message message, InetAddress address, int port) throws InterruptedException {
        byte[] buf = message.serialize();
        sendQueue.put(new DatagramPacket(buf, buf.length, address, port));
    }

    public void sendPackets() {
        Logger logger = new Logger(this, false);
        try {
            while (true) {
                DatagramPacket packet = sendQueue.take();
                socket.send(packet);
                logger.log("SEND");
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot send packets!");
        }
    }

    // -- MESSAGE OUT, BIS REPETITA --

    private void emptyWaitingQueue(HostInfo host) throws InterruptedException {
        while (host.canSendWaitingMessages()) {
            Message m = host.getNextWaitingMessage();
            sendNewMessage(m, host);
        }
    }

    private void stubbornSend() {
        while (true) {
            hostInfo.forEach((hostId, host) -> {
                final WaitingPacket wp = host.getNextStubborn();
                try {
                    if (wp == null) {
                        emptyWaitingQueue(host);
                    } else if (!host.hasAckedPacket(wp)) { // && receiveQueue.size() < WINDOW_SIZE
                        WaitingPacket newWp = wp.resendIfTimedOut(() -> {
                            try {
                                sendMessage(wp, host);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        });
                        host.addPacketToConfirm(newWp);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            });
        }
    }

    // -- MESSAGE IN, TREATING --

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

            int hostId = m.getLastHop();
            HostInfo host = hostInfo.get(hostId);

            try {
                if (m.isAck()) {
                    host.resetTimeout();
                    host.updateReceiveVectorClock(m.getSeqNumber());
                    emptyWaitingQueue(host);
                } else {
                    sendMessage(m.toAck(myId), host);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            Message.IntTriple id = m.getFullId();
            if (!delivered.contains(id)) {
                delivered.add(id);
                handleListeners(m, packet.getAddress(), packet.getPort());
            }
        }
    }

    // -- MESSAGE IN --

    private void listen(int port) {
        Logger logger = new Logger(this);
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
}
