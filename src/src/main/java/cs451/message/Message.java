package cs451.message;

public class Message {

    private final byte originId;
    private final int messageId;
    private final byte lastHop;
    private final boolean ack;

    private Message(int originId, int messageId, int lastHop, boolean ack) {
        this.originId = (byte) originId;
        this.messageId = messageId;
        this.lastHop = (byte) lastHop;
        this.ack = ack;
    }

    private Message(byte originId, int messageId, byte lastHop, boolean ack) {
        this.originId = originId;
        this.messageId = messageId;
        this.lastHop = lastHop;
        this.ack = ack;
    }

    public static Message createMessage(int originId, int messageId) {
        return new Message(originId, messageId, originId, false);
    }

    public Message toAck(int myId) {
        return new Message(originId, messageId, myId, true);
    }

    public Message changeLastHop(int myId) {
        return new Message(originId, messageId, myId, ack);
    }

    public int getOriginId() {
        return (int)originId & 0xFF;
    }

    public int getMessageId() {
        return messageId;
    }

    public int getLastHop() {
        return (int)lastHop & 0xFF;
    }

    public boolean isAck() {
        return ack;
    }

    public boolean isCorrectAck(Message that) {
        return ack && this.getOriginId() == that.getOriginId() && this.getMessageId() == that.getMessageId();
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Message
            && this.getMessageId() == (((Message) that).getMessageId())
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

    public byte[] serialize() {
        byte[] datagram = new byte[6];
        datagram[0] = lastHop;
        datagram[1] = originId;
        ByteOp.intToByte(messageId, datagram, 2);
        if (ack) {
            datagram[2] |= (byte) 0x80;
        }
        return datagram;
    }

    public static Message deserialize(byte[] datagram) {
        int lastHop = datagram[0];
        int originId = datagram[1];
        boolean ack = (datagram[2] & (byte)0x80) != 0;
        datagram[2] &= (byte) 0x7F;
        int messageId = ByteOp.byteToInt(datagram, 2);
        return new Message(originId, messageId, lastHop, ack);
    }
}
