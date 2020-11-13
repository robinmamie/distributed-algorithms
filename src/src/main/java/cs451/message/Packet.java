package cs451.message;

import java.util.Arrays;
import java.util.List;

import cs451.listener.BListener;

/**
 * Abstraction for a network message.
 */
public class Packet {

    /**
     * The maximum number of bytes authorized for the payload of a given UDP packet.
     */
    public static final int MAX_PAYLOAD_SIZE = 65507;

    // Byte offsets used for the byte datagram.
    private static final int NB_MESSAGES_OFFSET = 0;
    private static final int PACKET_NUMBER_OFFSET = 4;
    private static final int TIMESTAMP_OFFSET = 8;
    private static final int LAST_HOP_OFFSET = 12;
    private static final int ACK_OFFSET = 13;
    private static final int CONTENTS_OFFSET = 14;

    /**
     * The number of bytes used by a message inside the packet. 1 byte for the host
     * ID (because it is between 1 and 128), 4 for the message ID (because it is
     * between 1 and MAX_INT).
     */
    private static final int SIZE_OF_MESSAGE = 5;

    /**
     * The maximum number of messages that can be concatenated inside a packet.
     */
    public static final int MAX_MESSAGES_PER_PACKET = (MAX_PAYLOAD_SIZE - CONTENTS_OFFSET) / SIZE_OF_MESSAGE;

    /**
     * The last hop of the message, i.e. the ID of the host that sent it (this is
     * not necessarily the same as the origin ID).
     */
    private final byte lastHop;

    /**
     * The datagram of this package.
     */
    private final byte[] datagram;

    /**
     * The number of messages in this package.
     */
    private final int nbMessages;

    /**
     * The acknowledgement flag of this message.
     */
    private final boolean ack;

    /**
     * The timestamp of the packet, when it was first sent out.
     */
    private final int timestampMs;

    /**
     * The packet number, given by the host originally handing out the packet.
     */
    private final int packetNumber;

    private Packet(List<Message> messages, int packetNumber, int lastHop, boolean ack) {
        this(messages, packetNumber, (byte) lastHop, ack);
    }

    private Packet(List<Message> messages, int packetNumber, byte lastHop, boolean ack) {
        this(messages, packetNumber, lastHop, ack, (int) System.currentTimeMillis());
    }

    private Packet(List<Message> messages, int packetNumber, byte lastHop, boolean ack, int timestamp) {
        int nbMessage = messages.size();
        byte[] data = new byte[CONTENTS_OFFSET + 5 * nbMessage];
        ByteOp.intToByte(nbMessage, data, NB_MESSAGES_OFFSET);
        ByteOp.intToByte(packetNumber, data, PACKET_NUMBER_OFFSET);
        ByteOp.intToByte(timestamp, data, TIMESTAMP_OFFSET);
        data[LAST_HOP_OFFSET] = lastHop;
        data[ACK_OFFSET] = (byte) (ack ? 1 : 0);

        int pointerOrigin = CONTENTS_OFFSET;
        int pointerId = CONTENTS_OFFSET + nbMessage;
        for (Message m : messages) {
            // NB: the byte layout is optimized for an eventual compression of
            // the data. However, every characteristic of the message is present.
            data[pointerOrigin] = (byte) m.getOriginId();
            ByteOp.intToByte(m.getMessageId(), data, pointerId);
            pointerOrigin += 1;
            pointerId += 4;
        }

        this.packetNumber = packetNumber;
        this.lastHop = lastHop;
        this.ack = ack;
        this.timestampMs = timestamp;
        this.datagram = data;
        this.nbMessages = nbMessage;
    }

    private Packet(byte[] datagram, int nbMessages, int packetNumber, byte lastHop, boolean ack, int timestamp) {
        this.packetNumber = packetNumber;
        this.lastHop = lastHop;
        this.ack = ack;
        this.timestampMs = timestamp;
        this.datagram = datagram;
        this.nbMessages = nbMessages;
    }

    /**
     * Create a new packet from scratch.
     *
     * @param message The list of messages of this packet.
     * @param lastHop The last hop of this packet (generally speaking, the ID of the
     *                local host).
     * @return The newly created packet.
     */
    public static Packet createPacket(List<Message> messages, int packetNumber, int lastHop) {
        return new Packet(messages, packetNumber, lastHop, false);
    }

    /**
     * Create a new packet that is an acknowledgement of the current one. Change the
     * last hop with a new value, generally with the local host ID.
     *
     * @param id The new ID of the last hop.
     * @return The newly created packet.
     */
    public Packet toAck(int id) {
        byte[] newDatagram = datagram.clone();
        newDatagram[LAST_HOP_OFFSET] = (byte) id;
        newDatagram[ACK_OFFSET] = 1;
        return new Packet(newDatagram, nbMessages, packetNumber, (byte) id, true, timestampMs);
    }

    /**
     * Create a new packet by changing the last hop of this message, generally with
     * the local host ID.
     *
     * @param id The new ID of the last hop.
     * @return The newly created packet.
     */
    public Packet changeLastHop(int id) {
        byte[] newDatagram = datagram.clone();
        newDatagram[LAST_HOP_OFFSET] = (byte) id;
        return new Packet(newDatagram, nbMessages, packetNumber, (byte) id, ack, timestampMs);
    }

    /**
     * Create a new packet by updating its timestamp, generally done when resending
     * an originally locally created packet.
     *
     * @return The newly created packet.
     */
    public Packet resetTimestamp() {
        byte[] newDatagram = datagram.clone();
        int newTimestamp = (int) System.currentTimeMillis();
        ByteOp.intToByte(newTimestamp, newDatagram, TIMESTAMP_OFFSET);
        return new Packet(newDatagram, nbMessages, packetNumber, lastHop, ack, newTimestamp);
    }

    /**
     * Get the list of messages of this packet and apply the given function on each
     * message.
     */
    public void deliverMessages(BListener toExecute) {
        int pointerOrigin = CONTENTS_OFFSET;
        int pointerId = CONTENTS_OFFSET + nbMessages;
        for (int i = 0; i < nbMessages; ++i) {
            byte originId = datagram[pointerOrigin];
            int messageId = ByteOp.byteToInt(datagram, pointerId);
            toExecute.apply(Message.createMessage(originId, messageId, getLastHop()));
            pointerOrigin += 1;
            pointerId += 4;
        }
    }

    /**
     * Get the packet number.
     * 
     * @return The packet number.
     */
    public int getPacketNumber() {
        return packetNumber;
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

    int getTimestamp() {
        return timestampMs;
    }

    public int getAgeInMs() {
        return (int) System.currentTimeMillis() - timestampMs;
    }

    @Override
    public String toString() {
        return "Packet-" + (ack ? "Ack" : "Message") + " contains " + nbMessages + " messages, id " + packetNumber
                + " | last hop " + lastHop;
    }

    /**
     * Serialize the Packet in a byte array.
     *
     * @return The corresponding byte array.
     */
    public byte[] serialize() {
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
        int packetNumber = ByteOp.byteToInt(datagram, PACKET_NUMBER_OFFSET);
        int timestamp = ByteOp.byteToInt(datagram, TIMESTAMP_OFFSET);
        byte lastHop = datagram[LAST_HOP_OFFSET];
        boolean ack = datagram[ACK_OFFSET] != 0;

        return new Packet(Arrays.copyOfRange(datagram, 0, CONTENTS_OFFSET + SIZE_OF_MESSAGE * nbMessages), nbMessages,
                packetNumber, lastHop, ack, timestamp);
    }
}
