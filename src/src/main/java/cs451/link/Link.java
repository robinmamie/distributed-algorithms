package cs451.link;

import java.util.List;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

/**
 * Defines the global Link interface, i.e. what the broadcast part of the
 * program can see.
 */
public interface Link {

    /**
     * The number of messages to be sent at a maximum, for all hosts.
     */
    public static final int WINDOW_SIZE = 1 << 10;

    /**
     * The base timeout value for lost messages, in milliseconds.
     */
    public static final long TIMEOUT_MS = 1000;

    /**
     * Send a message through a link.
     *
     * @param message The message to be sent.
     * @param hostId  The ID of the recipient.
     */
    void send(Message message, int hostId);

    /**
     * Create and schedule the sending of locally created messages.
     *
     * @param hostId    The ID of the recipient.
     * @param originId  The origin ID of the message.
     * @param messageId The maximum message ID of the range (starts at 1).
     */
    void sendRange(int hostId, int originId, int messageId);

    /**
     * Create the default link for this project, i.e. PerfectLink.
     *
     * @param port     The port number to be used for communication purposes.
     * @param hosts    The list of Hosts, as given by the parser.
     * @param listener The listener to be called once a message is delivered.
     * @param myId     The ID of the local host.
     *
     * @return The newly created (Perfect)Link.
     */
    static Link getLink(int port, List<Host> hosts, BListener listener, int myId) {
        return new PerfectLink(port, hosts, listener, myId);
    }
}
