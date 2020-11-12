package cs451.message;

/**
 * Abstraction for a network message.
 */
public class Message {

    public static final int PACKET_SIZE = 1+1+4+4;
    public static final int MAX_PAYLOAD_SIZE = 65507;
    private static final int LAST_HOP_OFFSET = 0;
    private static final int ORIGIN_ID_OFFSET = 1;
    private static final int MESSAGE_ID_OFFSET = 2;
    private static final int TIMESTAMP_OFFSET = 6;

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

    /**
     * The acknowledgement flag of this message.
     */
    private final boolean ack;
    
    private final int timestampMs;

    private Message(int originId, int messageId, int lastHop, boolean ack) {
        this((byte)originId, messageId, (byte)lastHop, ack);
    }

    private Message(byte originId, int messageId, byte lastHop, boolean ack) {
        this(originId, messageId, lastHop, ack, (int) System.currentTimeMillis());
    }

    private Message(byte originId, int messageId, byte lastHop, boolean ack, int timestamp) {
        this.originId = originId;
        this.messageId = messageId;
        this.lastHop = lastHop;
        this.ack = ack;
        this.timestampMs = timestamp;

    }

    /**
     * Create a new message from scratch.
     *
     * @param originId  The origin ID of the message.
     * @param messageId The message ID of the message.
     * @return The newly created message.
     */
    public static Message createMessage(int originId, int messageId) {
        return new Message(originId, messageId, originId, false);
    }

    /**
     * Create a new message that is an acknowledgement of the current one. Change
     * the last hop with a new value, generally with the local host ID.
     *
     * @param id The new ID of the last hop.
     * @return The newly created message.
     */
    public Message toAck(int id) {
        return new Message(originId, messageId, id, true);
    }

    /**
     * Create a new message by changing the last hop of this message, generally with
     * the local host ID.
     *
     * @param id The new ID of the last hop.
     * @return The newly created message.
     */
    public Message changeLastHop(int id) {
        return new Message(originId, messageId, id, ack);
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
    public boolean equals(Object that) {
        return that instanceof Message && this.getMessageId() == (((Message) that).getMessageId())
                && this.getOriginId() == (((Message) that).getOriginId())
                && this.getLastHop() == (((Message) that).getLastHop());
    }

    @Override
    public int hashCode() {
        return ((getMessageId() * 17 + getOriginId()) * 31 + getLastHop()) * 37;
    }

    @Override
    public String toString() {
        return (ack ? "Ack" : "Message") + " #" + messageId + " from " + originId + " | last hop " + lastHop;
    }

    /**
     * Serialize the message in a byte array.
     *
     * @return The corresponding byte array.
     */
    public byte[] serialize() {
        byte[] datagram = new byte[PACKET_SIZE];
        datagram[LAST_HOP_OFFSET] = lastHop;
        datagram[ORIGIN_ID_OFFSET] = originId;
        ByteOp.intToByte(messageId, datagram, MESSAGE_ID_OFFSET);
        if (ack) {
            datagram[MESSAGE_ID_OFFSET] |= (byte) 0x80;
        }
        ByteOp.intToByte(timestampMs, datagram, TIMESTAMP_OFFSET);
        return datagram;
    }

    /**
     * Deserialize the given message, and create a new Message instance.
     *
     * @param datagram The content of the received packet.
     * @return A newly created Message.
     */
    public static Message deserialize(byte[] datagram) {
        byte lastHop = datagram[LAST_HOP_OFFSET];
        byte originId = datagram[ORIGIN_ID_OFFSET];
        boolean ack = (datagram[MESSAGE_ID_OFFSET] & (byte) 0x80) != 0;
        datagram[MESSAGE_ID_OFFSET] &= (byte) 0x7F;
        int messageId = ByteOp.byteToInt(datagram, MESSAGE_ID_OFFSET);
        int timestamp = ByteOp.byteToInt(datagram, TIMESTAMP_OFFSET);
        return new Message(originId, messageId, lastHop, ack, timestamp);
    }
}
