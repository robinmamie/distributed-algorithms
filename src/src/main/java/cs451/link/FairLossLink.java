package cs451.link;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

class FairLossLink extends AbstractLink {

    private final DatagramSocket socket;
    private final BlockingQueue<DatagramPacket> sendQueue = new LinkedBlockingQueue<>();

    public FairLossLink(int port, List<Host> hosts, BListener listener, int myId) {
        super(listener, myId, hosts);
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        Executor executor = Executors.newFixedThreadPool(2);
        executor.execute(this::sendPackets);
        executor.execute(this::deliver);
    }

    // *** High level packet handling, outgoing ***

    @Override
    public void send(Message message, int hostId) {
        HostInfo host = getHostInfo(hostId);
        message = message.changeLastHop(getMyId());
        byte[] buf = message.serialize();
        try {
            sendQueue.put(new DatagramPacket(buf, buf.length, host.getAddress(), host.getPort()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // *** High level packet handling, incoming ***

    private void deliver() {
        DatagramPacket packet;
        byte[] buf;
        while (true) {
            buf = new byte[6];
            packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException("Cannot receive packets!");
            }
            Message message = Message.deserialize(packet.getData());
            handleListener(message);
        }
    }

    // *** Low level packet handling **

    private void sendPackets() {
        try {
            while (true) {
                DatagramPacket packet = sendQueue.take();
                socket.send(packet);
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot send packets!");
        }
    }

    @Override
    public void sendRange(int hostId, int originId, int mId) {
        throw new RuntimeException();
    }
}
