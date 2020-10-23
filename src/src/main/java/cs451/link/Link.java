package cs451.link;

import java.net.InetAddress;
import java.util.List;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

public interface Link {

    /**
     * Determines the safe size for a UDP packet in this project.
     */
    public final static int UDP_SAFE_PACKET_MAX_SIZE = 508;

    /**
     * Send a message through a link.
     * 
     * @param message The message to be sent.
     * @param address The address of the recipient.
     * @param port The port number of the recipient.
     */
    void send(Message message, int hostId, InetAddress address, int port);

    void addListener(BListener listener);

    void removeListener(BListener listener);

    static Link getLink(int port, List<Host> hosts, int myId) {
        return new SeqLink(port, hosts, myId);
    }
}
