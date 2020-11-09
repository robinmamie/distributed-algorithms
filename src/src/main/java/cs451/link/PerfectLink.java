package cs451.link;

import java.util.List;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

/**
 * Perfect link abstraction.
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
     * Check if received messages were already delivered. If not, deliver
     *
     * @param message The message that is delivered by the underlying link.
     */
    private void deliver(Message message) {
        HostInfo hostInfo = getHostInfo(message.getLastHop());
        if (!hostInfo.isDelivered(message)) {
            hostInfo.markDelivered(message);
            handleListener(message);
        }
    }
}
