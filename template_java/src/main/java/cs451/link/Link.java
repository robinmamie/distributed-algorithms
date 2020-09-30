package cs451.link;

import java.net.InetAddress;

import cs451.Message;
import cs451.listener.Listener;

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
    boolean send(Message message, InetAddress address, int port);

    void addListener(Listener listener);

    void removeListener(Listener listener);

    static Link getLink(int port) {
        return new PerfectLink(port);
    }
}
