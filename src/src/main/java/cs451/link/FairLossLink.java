package cs451.link;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.message.Message;

class FairLossLink extends AbstractLink {

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
        new Thread(() -> handleListenersLowLevel()).start();
    }

    @Override
    public void send(Message message, InetAddress address, int port) {
        byte[] buf = message.serialize();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        while (true) {
            try {
                sendQueue.put(packet);
                break;
            } catch (InterruptedException e) {
                // Ignore
            }
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
            while (true) {
                try {
                    packet = receiveQueue.take();
                    break;
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
            byte[] datagram = packet.getData();
            Message message = Message.deserialize(datagram);
            handleListeners(message, packet.getAddress(), packet.getPort());
        }
    }
}
