package cs451.message;

public class Message {

    private final int originId;
    private final int messageId;
    private final int lastHop;
    private final boolean ack;

    private Message(int originId, int messageId, int lastHop, boolean ack) {
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
        return originId;
    }

    public int getMessageId() {
        return messageId;
    }

    public int getLastHop() {
        return lastHop;
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
        byte[] datagram = new byte[7];
        ByteOp.byteIntToByte(originId, datagram, 0);
        ByteOp.intToByte(messageId, datagram, 1);
        ByteOp.byteIntToByte(lastHop, datagram, 5);
        datagram[6] = ack ? (byte) 0xFF : 0;
        return datagram;
    }

    public static Message deserialize(byte[] datagram) {
        int originId = ByteOp.byteToByteInt(datagram, 0);
        int messageId = ByteOp.byteToInt(datagram, 1);
        int lastHop = ByteOp.byteToByteInt(datagram, 5);
        boolean ack = datagram[6] != 0;
        return new Message(originId, messageId, lastHop, ack);
    }
}
