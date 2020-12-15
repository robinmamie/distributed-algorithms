package cs451.link;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cs451.listener.PListener;
import cs451.message.Message;
import cs451.message.Packet;
import cs451.parser.Host;

/**
 * Stubborn link abstraction. Implements the stubborn delivery and no creation
 * properties.
 */
class StubbornLink extends AbstractLink {

    /**
     * The maximum number of attempts to fill a packet before sending it. This is to
     * avoid "empty" packets, i.e having only a few messages.
     */
    private static final int RETRIEVING_ATTEMPTS = 3;

    /**
     * The underlying fair-loss link.
     */
    private final FairLossLink fLink;

    /**
     * Create a stubborn link.
     *
     * @param port     The port number of the socket.
     * @param hosts    The complete list of hosts of the network.
     * @param listener The listener to call once a packet is delivered.
     * @param myId     The ID of the local host.
     */
    public StubbornLink(int port, List<Host> hosts, PListener listener, int myId) {
        super(listener, myId, hosts);
        this.fLink = new FairLossLink(port, hosts, this::deliver, myId);

        // Create a thread whose sole job is to empty waiting queues and check if
        // messages were acked, or otherwise resend them.
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

        // An ack gives us valuable information. We treat them as if they were messages
        // in themselves.
        handleListener(packet);
    }

    /**
     * Core function of the stubborn link, which empties waiting queues and re-sends
     * packets if and when necessary.
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
            if (!host.isMineDelivered(wp.getPacket())) {
                WaitingPacket newWp = wp.resendIfTimedOut(() -> fLink.send(wp.getPacket().resetTimestamp(), hostId));
                try {
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
        if (host.canSendWaitingMessages()) {
            List<Message> messages = retrieveAListOfMessages(host);
            createAndSendPacket(messages, hostId, host);
        }
    }

    /**
     * For a given host, retrieve a list of waiting messages.
     *
     * @param host The network information related to the host.
     * @return The retrieved list of waiting messages.
     */
    private List<Message> retrieveAListOfMessages(HostInfo host) {
        List<Message> messages = new LinkedList<>();
        int byteCount = Packet.CONTENTS_OFFSET;
        int attempts = 0;

        // Fill a network packet to the maximum safe capacity
        while (byteCount < Packet.SAFE_MAX_PAYLOAD_SIZE && attempts < RETRIEVING_ATTEMPTS) {
            Message m = host.getNextWaitingMessage();
            if (m == null) {
                // Sleep for a moment, in order to fill the packet to the max
                attempts += 1;
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return messages;
                }
            } else {
                messages.add(m);
                byteCount += Packet.BASIC_MESSAGE_SIZE + Packet.SIZE_OF_DEPENDENCY * m.getDependencies().size();
                attempts = 0;
            }
        }
        return messages;
    }

    /**
     * Creates and sends a packet out of the given list of messages.
     * 
     * @param messages The list of message to send in one packet.
     * @param hostId   The ID of the host.
     * @param host     The network information related to the host.
     */
    private void createAndSendPacket(List<Message> messages, int hostId, HostInfo host) {
        if (!messages.isEmpty()) {
            Packet packet = Packet.createPacket(messages, host.getNewPacketNumber(), getMyId());
            fLink.send(packet, hostId);
            WaitingPacket wpa = new WaitingPacket(packet, host);
            try {
                host.addPacketToConfirm(wpa);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
