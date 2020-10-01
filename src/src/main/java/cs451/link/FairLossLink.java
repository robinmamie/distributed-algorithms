package cs451.link;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import cs451.message.Message;

class FairLossLink extends AbstractLink {

    public static class Packet {
        public Message message;
        public InetAddress address;
        public int port;
    }

    private final DatagramSocket listen_socket;
    private final byte[] buf = new byte[UDP_SAFE_PACKET_MAX_SIZE];

    public FairLossLink(int port) {
        try {
            listen_socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> listen()).start();
    }

    @Override
    public boolean send(Message message, InetAddress address, int port) {
        DatagramSocket send_socket;
        try {
            send_socket = new DatagramSocket();
        } catch (SocketException e) {
            return false;
        }
        byte[] buf = message.serialize();
        //System.out.println(buf.length);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        try {
            listen_socket.send(packet);
        } catch (IOException e) {
            send_socket.close();
            return false;
        }
        send_socket.close();
        return true;
    }

    public void listen() {
        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            Message message = null;
            try {
                listen_socket.receive(packet);
                // TODO enqueue packet to get back to listening as fast as possible (copy packet)
                byte[] datagram = packet.getData();
                message = Message.deserialize(datagram);
            } catch (IOException e) {
                continue;
            }
            // TODO create new thread
            handleListeners(message, packet.getAddress(), packet.getPort());
        }
    }
}
