package cs451.message;

public class Message {

    public static final class IntPair {
        private final int a;
        private final int b;

        public IntPair(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object that) {
            return that instanceof IntPair && this.a == ((IntPair) that).a && this.b == ((IntPair) that).b;
        }

        @Override
        public int hashCode() {
            return (a * 31) + (b * 17);
        }

        @Override
        public String toString() {
            return "(" + a + "," + b + ")";
        }
    }

    private final int originId;
    private final int messageId;
    private final int lastHop;
    private final boolean ack;
    private final long seqNumber;
    private final boolean alreadyHandled;

    private Message(int originId, int messageId, int lastHop, long seqNumber, boolean ack, boolean alreadyHandled) {
        this.originId = originId;
        this.messageId = messageId;
        this.lastHop = lastHop;
        this.ack = ack;
        this.seqNumber = seqNumber;
        this.alreadyHandled = alreadyHandled;
    }

    public static Message createMessage(int originId, int messageId) {
        return new Message(originId, messageId, originId, -1, false, false);
    }

    public Message toAck(int myId) {
        return new Message(originId, messageId, myId, seqNumber, true, alreadyHandled);
    }

    public Message changeLastHop(int myId) {
        return new Message(originId, messageId, myId, seqNumber, ack, alreadyHandled);
    }

    public Message changeSeqNumber(long seqNumber) {
        return new Message(originId, messageId, lastHop, seqNumber, ack, alreadyHandled);
    }

    public Message setFlagAlreadyHandled(boolean alreadyHandled) {
        return new Message(originId, messageId, lastHop, seqNumber, ack, alreadyHandled);
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

    public long getSeqNumber() {
        return seqNumber;
    }

    public boolean isAck() {
        return ack;
    }

    public boolean isAlreadyHandled() {
        return alreadyHandled;
    }

    public IntPair getId() {
        return new IntPair(originId, messageId);
    }

    public boolean isCorrectAck(Message that) {
        return ack && this.getOriginId() == that.getOriginId() && this.getMessageId() == that.getMessageId();
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Message && this.getId().equals(((Message) that).getId());
    }

    @Override
    public int hashCode() {
        return messageId * 17 + originId * 31;
    }

    @Override
    public String toString() {
        return (ack ? "Ack" : "Message") + " #" + messageId + " from " + originId + " | last hop " + lastHop + ", seq "
                + seqNumber;
    }

    public byte[] serialize() {
        byte[] datagram = new byte[15];
        ByteOp.byteIntToByte(originId, datagram, 0);
        ByteOp.intToByte(messageId, datagram, 1);
        ByteOp.byteIntToByte(lastHop, datagram, 5);
        ByteOp.longToByte(seqNumber, datagram, 6);
        datagram[14] = ack ? (byte) 0xFF : 0;
        return datagram;
    }

    public static Message deserialize(byte[] datagram) {
        int originId = ByteOp.byteToByteInt(datagram, 0);
        int messageId = ByteOp.byteToInt(datagram, 1);
        int lastHop = ByteOp.byteToByteInt(datagram, 5);
        long seqNumber = ByteOp.byteToLong(datagram, 6);
        boolean ack = datagram[14] != 0;
        return new Message(originId, messageId, lastHop, seqNumber, ack, false);
    }
}
