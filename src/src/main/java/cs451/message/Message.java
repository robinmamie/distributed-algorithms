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
            return that instanceof IntPair
                && this.a == ((IntPair)that).a
                && this.b == ((IntPair)that).b;
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

    public static final class IntTriple {
        private final int a;
        private final int b;
        private final int c;
        public IntTriple(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
        @Override
        public boolean equals(Object that) {
            return that instanceof IntTriple
                && this.a == ((IntTriple)that).a
                && this.b == ((IntTriple)that).b
                && this.c == ((IntTriple)that).c;
        }
        @Override
        public int hashCode() {
            return (a * 31) + (b * 17) + (c * 13);
        }
    }

    // note: ID is maximum 1 byte!
    private final int originId;
    private final int messageId;
    private final int lastHop;
    private final boolean ack;
    private final long seqNumber;

    private Message(int originId, int messageId, int lastHop, long seqNumber, boolean ack) {
        this.originId = originId;
        this.messageId = messageId;
        this.lastHop = lastHop;
        this.ack = ack;
        this.seqNumber = seqNumber;
    }

    public static Message createMessage(int originId, int messageId) {
        return new Message(originId, messageId, originId, -1, false);
    }

    public Message toAck(int myId) {
        return new Message(originId, messageId, myId, seqNumber, true);
    }

    public Message changeLastHop(int myId) {
        return new Message(originId, messageId, myId, seqNumber, ack);
    }

    public Message changeSeqNumber(long seqNumber) {
        return new Message(originId, messageId, lastHop, seqNumber, ack);
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

    public IntPair getId() {
        return new IntPair(originId, messageId);
    }

    public IntTriple getFullId() {
        return new IntTriple(originId, messageId, lastHop);
    }

    public boolean isCorrectAck(Message that) {
        return ack
            && this.getOriginId() == that.getOriginId()
            && this.getMessageId() == that.getMessageId();
    }   

    @Override
    public boolean equals(Object that) {
        return that instanceof Message
            && this.getId().equals(((Message)that).getId());
    }

    @Override
    public int hashCode() {
        return messageId * 17 + originId * 31;
    }

    @Override
    public String toString() {
        return (ack ? "Ack" : "Message") + " #" + messageId + " from " + originId + ", last hop " + lastHop;
    }

    public byte[] serialize() {
        byte[] datagram = new byte[21];
        ByteOp.intToByte(originId, datagram, 0);
        ByteOp.intToByte(messageId, datagram, 4);
        ByteOp.intToByte(lastHop, datagram, 8);
        ByteOp.longToByte(seqNumber, datagram, 12);
        datagram[20] = ack ? (byte) 0xFF : 0;
        return datagram;
    }

    public static Message deserialize(byte[] datagram) {
        int originId = ByteOp.byteToInt(datagram, 0);
        int messageId = ByteOp.byteToInt(datagram, 4);
        int lastHop = ByteOp.byteToInt(datagram, 8);
        long seqNumber = ByteOp.byteToLong(datagram, 12);
        boolean ack = datagram[20] != 0;
        return new Message(originId, messageId, lastHop, seqNumber, ack);
    }
}
