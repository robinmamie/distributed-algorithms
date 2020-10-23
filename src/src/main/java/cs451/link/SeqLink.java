package cs451.link;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.message.Message;
import cs451.parser.Host;

public class SeqLink extends AbstractLink {

    public static final int WINDOW_SIZE = 1 << 15;

    private final DatagramSocket socket;
    private final int myId;

    private final BlockingQueue<DatagramPacket> sendQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<>();

    private final Map<Integer, HostInfo> hostInfo = new TreeMap<>();

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
        ExecutorService executor = Executors.newFixedThreadPool(4);
        executor.execute(() -> listen());
        executor.execute(() -> sendPackets());
        executor.execute(() -> stubbornSend());
        executor.execute(() -> handleListenersLowLevel());
    }

    @Override
    public void send(Message message, int hostId) {
        try {
            // Set sequence number to message
            HostInfo host = hostInfo.get(hostId);
            host.addMessageInWaitingList(message);
            emptyWaitingQueue(host);
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
        message.signalBroadcast();
        byte[] buf = message.serialize();
        sendQueue.put(new DatagramPacket(buf, buf.length, address, port));
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

    // -- MESSAGE OUT, BIS REPETITA --

    private void emptyWaitingQueue(HostInfo host) throws InterruptedException {
        while (host.canSendWaitingMessages()) {
            Message m = host.getNextWaitingMessage();
            if (m != null) {
                long seqNumber = host.getNextSeqNumber();
                Message seqMessage = m.changeSeqNumber(seqNumber);
                sendNewMessage(seqMessage, host);
            }
        }
    }

    private void stubbornSend() {
        while (true) {
            hostInfo.forEach((hostId, host) -> {
                final WaitingPacket wp = host.getNextStubborn();
                try {
                    if (wp == null) {
                        emptyWaitingQueue(host);
                    } else if (!host.hasAckedPacket(wp.getMessage().getSeqNumber())) { // && receiveQueue.size() < WINDOW_SIZE
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
        DatagramPacket packet;

        while (true) {
            try {
                packet = receiveQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            Message m = Message.deserialize(packet.getData());
            HostInfo host = hostInfo.get(m.getLastHop());
            host.resetTimeout();
            // TODO compute timeout according to RTT, like TCP!

            // Stubborn link (ack-handling)
            boolean alreadyHandled;
            try {
                if (m.isAck()) {
                    alreadyHandled = host.hasAckedPacket(m.getSeqNumber());
                    host.updateReceiveVectorClock(m.getSeqNumber());
                } else {
                    sendMessage(m.toAck(myId), host);
                    alreadyHandled = host.hasReceivedMessage(m.getSeqNumber());
                    host.updateLocalReceiveVectorClock(m.getSeqNumber());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Perfect link
            if (!alreadyHandled) {
                handleListeners(m, packet.getAddress(), packet.getPort());
            }
        }
    }

    // -- MESSAGE IN --

    private void listen() {
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
