package cs451.broadcast;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.link.Link;
import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

/**
 * Best effort broadcast abstraction.
 */
class BEBroadcast implements Broadcast {

    /**
     * The maximum number of messages that can wait in the BEB-deliver buffer.
     */
    private static final int MAX_BUFFER_SIZE = 1 << 17;

    /**
     * The list of hosts, used during the broadcast phase
     */
    private final List<Host> hosts;

    /**
     * The ID of the local host, to deliver the message directly instead of sending
     * it to itself.
     */
    private final int myId;

    /**
     * The listener used by the "upper" echelon (either URB broadcast, or directly
     * the user).
     */
    private final BListener deliver;

    /**
     * The link used to send the messages (in the case of this project:
     * PerfectLink).
     */
    private final Link link;

    /**
     * The waiting queue of messages delivered by the link layer.
     */
    private final BlockingQueue<Message> toHandle = new LinkedBlockingQueue<>(MAX_BUFFER_SIZE);

    /**
     * Builds a best effort broadcaster.
     *
     * @param port    The port number, used to build the underlying link.
     * @param hosts   The list of hosts, used to broadcast messages.
     * @param myId    The ID of the local host, to avoid sending the message locally
     *                via the network.
     * @param deliver The listener used when a message is delivered.
     */
    public BEBroadcast(int port, List<Host> hosts, int myId, BListener deliver) {
        this.hosts = hosts;
        this.myId = myId;
        this.deliver = deliver;
        this.link = Link.getLink(port, hosts, this::deliver, myId);
    }

    /**
     * Used by the underlying link to deliver the message. Serves as a buffer to the
     * thread working in the broadcast layer.
     *
     * @param message The message to be delivered
     */
    private void deliver(Message message) {
        try {
            toHandle.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void broadcast(Message message) {
        // Do not send message to where it originated from: unnecessary.
        int sentFrom = message.getLastHop();
        for (Host host : hosts) {
            if (host.getId() != sentFrom || sentFrom == myId) {
                if (myId != host.getId()) {
                    link.send(message, host.getId());
                } else {
                    deliver.apply(message);
                }
            }
        }
    }

    @Override
    public void broadcastRange(int numberOfMessages) {
        for (Host host : hosts) {
            if (myId != host.getId()) {
                link.sendRange(host.getId(), myId, numberOfMessages);
            }
        }
        run();
    }

    /**
     * Empties the deliver-buffer when it can, by calling the listener of the upper
     * instance.
     */
    public void run() {
        Message message;
        while (true) {
            try {
                message = toHandle.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            deliver.apply(message);
        }
    }
}
