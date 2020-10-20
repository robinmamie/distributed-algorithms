package cs451.link;

import java.net.InetAddress;

import cs451.listener.LinkListener;
import cs451.message.Message;

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
    void send(Message message, InetAddress address, int port);

    void addListener(LinkListener listener);

    void removeListener(LinkListener listener);

    static Link getLink(int port, int numHosts, int myId) {
        return new SeqLink(port, numHosts, myId);
    }
}
