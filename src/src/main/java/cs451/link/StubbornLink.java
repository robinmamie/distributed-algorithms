package cs451.link;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.message.Packet;
import cs451.parser.Host;

/**
 * Stubborn link abstraction.
 */
class StubbornLink extends AbstractLink {

    /**
     * The underlying fair-loss link.
     */
    private final FairLossLink fLink;

    /**
     * Create a stubborn link.
     *
     * @param port     The port number of the socket.
     * @param hosts    The complete list of hosts of the network.
     * @param listener The listener to call once a message is delivered.
     * @param myId     The ID of the local host.
     */
    public StubbornLink(int port, List<Host> hosts, BListener listener, int myId) {
        super(listener, myId, hosts);
        this.fLink = new FairLossLink(port, hosts, this::deliver, myId);

        // Create threads whose sole job is to empty waiting queues and check if
        // messages were acked.
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(this::stubbornSend);
    }

    @Override
    public void send(Message message, int hostId) {
        HostInfo host = getHostInfo(hostId);
        host.addMessageInWaitingList(message);
    }

    @Override
    public void sendRange(int hostId, int originId, int messageId) {
        HostInfo hostInfo = getHostInfo(hostId);
        hostInfo.sendRange(originId, 1, messageId);
    }

    /**
     * Send acks to hosts sending us messages (not acks), and deliver the message
     * (to the next layer).
     *
     * @param message The message that is delivered by the underlying link.
     */
    private void deliver(Packet packet) {
        int hostId = packet.getLastHop();

        if (!packet.isAck()) {
            fLink.send(packet.toAck(getMyId()), hostId);
        } else {
            // Reset the timeout, as we got an answer from the distant host.
            getHostInfo(hostId).resetTimeout(packet);
        }

        for (Message message: packet.getMessages()) {
            // An ack gives us valuable information. We treat them as if they were messages
            // in themselves.
            handleListener(message);
        }
    }

    /**
     * Core function of the stubborn link, which empties waiting queues and re-sends
     * packets when necessary.
     */
    private void stubbornSend() {
        while (true) {
            getHostInfo().forEach(this::checkNextPacketToConfirm);
        }
    }

    /**
     * Check if the next "stubborn" packet, i.e. waiting to be acked, of this
     * particular host was acked or should be resent.
     *
     * @param hostId The ID of the host.
     * @param host   The network information related to the host.
     */
    private void checkNextPacketToConfirm(int hostId, HostInfo host) {
        List<WaitingPacket> wps = host.getNextStubbornPackets();
        for (WaitingPacket wp : wps) {
            if (!host.isDelivered(wp.getPacket())) {
                try {
                    WaitingPacket newWp = wp.resendIfTimedOut(() -> fLink.send(wp.getPacket().resetTimestamp(), hostId));
                    host.addPacketToConfirm(newWp);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        emptyWaitingQueue(hostId, host);
    }

    /**
     * For a given host, check if new packets can be sent, according to the defined
     * window.
     *
     * @param hostId The ID of the host.
     * @param host   The network information related to the host.
     */
    private void emptyWaitingQueue(int hostId, HostInfo host) {
        List<Message> messages = new LinkedList<>();
        for (int i = 0; i < 10000 && host.canSendWaitingMessages(); ++i) {
            Message m = host.getNextWaitingMessage();
            if (m != null) {
                messages.add(m);
            }
        }
        if (!messages.isEmpty()) {
            Packet p = Packet.createPacket(messages, getMyId());
            fLink.send(p, hostId);
            WaitingPacket wpa = new WaitingPacket(p, host);
            try {
                host.addPacketToConfirm(wpa);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
