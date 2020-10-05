package cs451.link;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.message.Message;

class FairLossLink extends AbstractLink {

    private static final int NB_LISTENER_HANDLERS = 5;

    private final DatagramSocket socket;
    private final BlockingQueue<DatagramPacket> sendQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<>();

    public FairLossLink(int port) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> listen(port)).start();
        new Thread(() -> sendPackets()).start();
        for (int i = 0; i < NB_LISTENER_HANDLERS; ++i) {
            new Thread(() -> handleListenersLowLevel()).start();
        }
    }

    @Override
    public void send(Message message, InetAddress address, int port) {
        byte[] buf = message.serialize();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        try {
            sendQueue.put(packet);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void sendPackets() {
        while (true) {
            try {
                while (true) {
                    DatagramPacket packet = sendQueue.take();
                    socket.send(packet);
                }
            } catch (Exception e) {
                continue;
            }
        }
    }

    private void listen(int port) {
        while (true) {
            try {
                while (true) {
                    byte[] buf = new byte[UDP_SAFE_PACKET_MAX_SIZE];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    receiveQueue.put(packet);
                }
            } catch (Exception e) {
                continue;
            }
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
            Message message = Message.deserialize(datagram);
            handleListeners(message, packet.getAddress(), packet.getPort());
        }
    }
}
