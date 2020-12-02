package cs451.message;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstraction for a network message.
 */
public class Message {

    /**
     * The last hop of the message, i.e. the ID of the host that sent it (this is
     * not necessarily the same as the origin ID).
     */
    private final byte lastHop;

    /**
     * The origin ID of the message, i.e. where it originally comes from.
     */
    private final byte originId;

    /**
     * The message ID of the message, i.e. the sequence number used by the host
     * where it originally comes from.
     */
    private final int messageId;

    private final List<Integer> dependencies;

    private Message(int originId, int messageId, int lastHop) {
        this((byte) originId, messageId, (byte) lastHop, new LinkedList<>());
    }

    private Message(Message message, List<Integer> dependencies) {
        this(message.originId, message.messageId, message.lastHop, dependencies);
    }

    private Message(byte originId, int messageId, byte lastHop, List<Integer> dependencies) {
        this.originId = originId;
        this.messageId = messageId;
        this.lastHop = lastHop;
        this.dependencies = Collections.unmodifiableList(new LinkedList<>(dependencies));
    }

    /**
     * Create a new message from scratch.
     *
     * @param originId  The origin ID of the message.
     * @param messageId The message ID of the message.
     * @return The newly created message.
     */
    public static Message createMessage(int originId, int messageId) {
        return new Message(originId, messageId, originId);
    }

    /**
     * Create a new message from scratch.
     *
     * @param originId  The origin ID of the message.
     * @param messageId The message ID of the message.
     * @param lastHop   The last hop of the message.
     * @return The newly created message.
     */
    public static Message createMessage(int originId, int messageId, int lastHop) {
        return new Message(originId, messageId, lastHop);
    }

    /**
     * Create a new message from scratch.
     *
     * @param originId     The origin ID of the message.
     * @param messageId    The message ID of the message.
     * @param lastHop      The last hop of the message.
     * @param dependencies The list of causality dependencies.
     * @return The newly created message.
     */
    public static Message createMessage(int originId, int messageId, int lastHop, List<Integer> dependencies) {
        return new Message((byte) originId, messageId, (byte) lastHop, dependencies);
    }

    public Message addCausality(List<Integer> dependencies) {
        return new Message(this, dependencies);
    }

    /**
     * Get the origin ID of the message, i.e. where it originally comes from.
     *
     * @return The origin ID of the message.
     */
    public int getOriginId() {
        return (int) originId & 0xFF;
    }

    /**
     * Get the message ID of the message, i.e. the sequence number used by the host
     * where it originally comes from.
     *
     * @return The message ID of the message.
     */
    public int getMessageId() {
        return messageId;
    }

    /**
     * Get the last hop of the message, i.e. the ID of the host that sent it (this
     * is not necessarily the same as the origin ID).
     *
     * @return The ID of the host that sent this message, the last hop.
     */
    public int getLastHop() {
        return (int) lastHop & 0xFF;
    }

    public List<Integer> getDependencies() {
        return dependencies;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message" + " #" + messageId + " from " + originId + " | last hop " + lastHop + " | dep: ");
        for (int e : dependencies) {
            sb.append(e).append(" ");
        }
        return sb.toString();
    }
}
