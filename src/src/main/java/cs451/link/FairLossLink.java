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

/**
 * Fair-loss link abstraction.
 */
class FairLossLink extends AbstractLink {

    /**
     * The UDP socket.
     */
    private final DatagramSocket socket;

    /**
     * The sending queue, which avoids concurrency on the sending part of the
     * socket.
     */
    private final BlockingQueue<DatagramPacket> sendQueue = new LinkedBlockingQueue<>();

    /**
     * Create a fair-loss link.
     *
     * @param port     The port number of the socket.
     * @param hosts    The complete list of hosts of the network.
     * @param listener The listener to call once a message is delivered.
     * @param myId     The ID of the local host.
     */
    public FairLossLink(int port, List<Host> hosts, BListener listener, int myId) {
        super(listener, myId, hosts);
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        // Create 2 threads: one for sending packets, another to deliver incoming
        // packets.
        Executor executor = Executors.newFixedThreadPool(2);
        executor.execute(this::sendPackets);
        executor.execute(this::deliver);
    }

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

    @Override
    public void sendRange(int hostId, int originId, int messageId) {
        // This function is not designed for this level of Link.
        throw new RuntimeException();
    }

    /**
     * Receive, de-serialize and deliver incoming packets (to the next layer).
     */
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

    /**
     * Low level packet sending: takes from the queue and uses the socket, to avoid
     * any concurrency problem.
     */
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
}
