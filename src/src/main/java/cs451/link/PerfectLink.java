package cs451.link;

import java.util.List;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.message.Packet;
import cs451.parser.Host;

/**
 * Perfect link abstraction. Implements the reliable delivery, no duplication
 * and no creation properties.
 */
class PerfectLink extends AbstractLink {

    /**
     * The underlying stubborn link.
     */
    private final StubbornLink sLink;

    /**
     * Create a perfect link.
     *
     * @param port     The port number of the socket.
     * @param hosts    The complete list of hosts of the network.
     * @param listener The listener to call once a message is delivered.
     * @param myId     The ID of the local host.
     */
    public PerfectLink(int port, List<Host> hosts, BListener listener, int myId) {
        super(listener, myId, hosts);
        this.sLink = new StubbornLink(port, hosts, this::deliver, myId);
    }

    @Override
    public void send(Message message, int hostId) {
        sLink.send(message, hostId);
    }

    @Override
    public void sendRange(int hostId, int originId, int messageId) {
        sLink.sendRange(hostId, originId, messageId);
    }

    /**
     * Check if received messages were already delivered. If not, deliver them.
     *
     * @param packet The packet that is delivered by the underlying link.
     */
    private void deliver(Packet packet) {
        HostInfo hostInfo = getHostInfo(packet.getLastHop());
        // First, check if the packet was already delivered.
        if (!hostInfo.isDelivered(packet)) {
            hostInfo.markDelivered(packet);
            packet.deliverMessages(message -> {
                // Then, only deliver new messages.
                if (!hostInfo.isDelivered(message)) {
                    hostInfo.markDelivered(message);
                    handleListener(message);
                }
            });
        }
    }
}
