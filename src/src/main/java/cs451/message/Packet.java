package cs451.message;

import java.util.LinkedList;
import java.util.List;

/**
 * Abstraction for a network message.
 */
public class Packet {

    public static final int MAX_PAYLOAD_SIZE = 65507;

    private static final int NB_MESSAGES_OFFSET = 0;
    private static final int TIMESTAMP_OFFSET = 4;
    private static final int LAST_HOP_OFFSET = 8;
    private static final int ACK_OFFSET = 9;
    private static final int CONTENTS_OFFSET = 10;

    /**
     * The last hop of the message, i.e. the ID of the host that sent it (this is
     * not necessarily the same as the origin ID).
     */
    private final byte lastHop;

    /**
     * The list of individual messages gathered in this package.
     */
    private final List<Message> messages;

    /**
     * The acknowledgement flag of this message.
     */
    private final boolean ack;

    private final int timestampMs;

    private Packet(List<Message> messages, int lastHop, boolean ack) {
        this(messages, (byte)lastHop, ack);
    }

    private Packet(List<Message> messages, byte lastHop, boolean ack) {
        this(messages, lastHop, ack, (int) System.currentTimeMillis());
    }

    private Packet(List<Message> messages, byte lastHop, boolean ack, int timestamp) {
        this.messages = messages;
        this.lastHop = lastHop;
        this.ack = ack;
        this.timestampMs = timestamp;

    }

    /**
     * Create a new packet from scratch.
     *
     * @param message The list of messages of this packet.
     * @param lastHop The last hop of this packet (generally speaking, the ID of
     *                the local host).
     * @return The newly created packet.
     */
    public static Packet createPacket(List<Message> messages, int lastHop) {
        return new Packet(messages, lastHop, false);
    }

    /**
     * Create a new packet that is an acknowledgement of the current one. Change
     * the last hop with a new value, generally with the local host ID.
     *
     * @param id The new ID of the last hop.
     * @return The newly created packet.
     */
    public Packet toAck(int id) {
        return new Packet(messages, (byte)id, true, timestampMs);
    }

    /**
     * Create a new packet by changing the last hop of this message, generally with
     * the local host ID.
     *
     * @param id The new ID of the last hop.
     * @return The newly created packet.
     */
    public Packet changeLastHop(int id) {
        return new Packet(messages, (byte)id, ack, timestampMs);
    }

    public Packet resetTimestamp() {
        return new Packet(messages, lastHop, ack);
    }

    /**
     * Get the list of messages of this packet.
     *
     * @return The list of messages included in this packet.
     */
    public List<Message> getMessages() {
        return messages;
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

    /**
     * Check whether this message is an acknowledgement message. This is mainly used
     * for the stubborn layer of the link, so that there is no infinite loop of
     * acknowledgement message if this field did not exist.
     *
     * @return Whether this message is an acknowledgement.
     */
    public boolean isAck() {
        return ack;
    }

    public int getAgeInMs() {
        return (int) System.currentTimeMillis() - timestampMs;
    }

    @Override
    public String toString() {
        return "Packet-" + (ack ? "Ack" : "Message") + " contains " + messages.size() + " messages | last hop " + lastHop;
    }

    /**
     * Serialize the Packet in a byte array.
     *
     * @return The corresponding byte array.
     */
    public byte[] serialize() {
        int nbMessages = messages.size();
        byte[] datagram = new byte[4 + 4 + 1 + 1 + 5*nbMessages];
        ByteOp.intToByte(nbMessages, datagram, NB_MESSAGES_OFFSET);
        ByteOp.intToByte(timestampMs, datagram, TIMESTAMP_OFFSET);
        datagram[LAST_HOP_OFFSET] = lastHop;
        datagram[ACK_OFFSET] = (byte) (ack ? 1 : 0);

        int pointerOrigin = CONTENTS_OFFSET;
        int pointerId = CONTENTS_OFFSET + nbMessages;
        for (Message m : messages) {
            datagram[pointerOrigin] = (byte) m.getOriginId();
            ByteOp.intToByte(m.getMessageId(), datagram, pointerId);
            pointerOrigin += 1;
            pointerId += 4;
        }

        return datagram;
    }

    /**
     * Deserialize the given packet, and create a new Packet instance.
     *
     * @param datagram The content of the received packet.
     * @return A newly created Packet.
     */
    public static Packet deserialize(byte[] datagram) {
        int nbMessages = ByteOp.byteToInt(datagram, NB_MESSAGES_OFFSET);
        int timestamp = ByteOp.byteToInt(datagram, TIMESTAMP_OFFSET);
        byte lastHop = datagram[LAST_HOP_OFFSET];
        boolean ack = datagram[ACK_OFFSET] != 0;

        List<Message> messages = new LinkedList<>();
        int pointerOrigin = CONTENTS_OFFSET;
        int pointerId = CONTENTS_OFFSET + nbMessages;
        for (int i = 0; i < nbMessages; ++i) {
            byte originId = datagram[pointerOrigin];
            int messageId = ByteOp.byteToInt(datagram, pointerId);
            messages.add(Message.createMessage(originId, messageId).changeLastHop(lastHop));
            pointerOrigin += 1;
            pointerId += 4;
        }

        return new Packet(messages, lastHop, ack, timestamp);
    }
}
