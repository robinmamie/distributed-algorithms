package cs451.message;

import java.util.LinkedList;
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

    /**
     * The number of bytes used by a message inside the packet. 1 byte for the host
     * ID (because it is between 1 and 128), 4 for the message ID (because it is
     * between 1 and MAX_INT), and 1 for the size of the dependency list.
     */
    public static final int BASIC_MESSAGE_SIZE = 6;

    /**
     * The size of each dependency, used by the StubbornLink to estimate the size of
     * a packet.
     */
    public static final int SIZE_OF_DEPENDENCY = Integer.BYTES;

    private static final int MAX_NUM_OF_OTHER_PROCESSES = 127;

    /**
     * The maximum "safe" size for a packet, before one cannot be sure if the next
     * waiting message retrieved by the stubborn layer will break the size limit of
     * the packet.
     */
    public static final int SAFE_MAX_PAYLOAD_SIZE = MAX_PAYLOAD_SIZE
            - (BASIC_MESSAGE_SIZE + MAX_NUM_OF_OTHER_PROCESSES * SIZE_OF_DEPENDENCY);

    // Byte offsets used for the byte datagram.
    private static final int NB_MESSAGES_OFFSET = 0;
    private static final int PACKET_NUMBER_OFFSET = 4;
    private static final int TIMESTAMP_OFFSET = 8;
    private static final int LAST_HOP_OFFSET = 12;
    private static final int ACK_OFFSET = 13;
    public static final int CONTENTS_OFFSET = 14;

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
        byte[] data = new byte[MAX_PAYLOAD_SIZE];
        ByteOp.intToByte(nbMessage, data, NB_MESSAGES_OFFSET);
        ByteOp.intToByte(packetNumber, data, PACKET_NUMBER_OFFSET);
        ByteOp.intToByte(timestamp, data, TIMESTAMP_OFFSET);
        data[LAST_HOP_OFFSET] = lastHop;
        data[ACK_OFFSET] = (byte) (ack ? 1 : 0);

        int pointer = CONTENTS_OFFSET;
        for (Message m : messages) {
            // Information about the message is stored sequentially.
            data[pointer] = (byte) m.getOriginId();
            pointer += 1;
            ByteOp.intToByte(m.getMessageId(), data, pointer);
            pointer += 4;
            data[pointer] = (byte) m.getDependencies().size();
            pointer += 1;
            for (Integer e : m.getDependencies()) {
                ByteOp.intToByte(e, data, pointer);
                pointer += 4;
            }
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
        int pointer = CONTENTS_OFFSET;
        for (int i = 0; i < nbMessages; ++i) {
            byte originId = datagram[pointer];
            pointer += 1;
            int messageId = ByteOp.byteToInt(datagram, pointer);
            pointer += 4;
            int nbDependencies = (int) datagram[pointer] & 0xFF;
            pointer += 1;
            List<Integer> dependencies = new LinkedList<>();
            for (int j = 0; j < nbDependencies; ++j) {
                dependencies.add(ByteOp.byteToInt(datagram, pointer));
                pointer += 4;
            }
            toExecute.apply(Message.createMessage(originId, messageId, getLastHop(), dependencies));
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
        return new Packet(datagram, nbMessages, packetNumber, lastHop, ack, timestamp);
    }
}
