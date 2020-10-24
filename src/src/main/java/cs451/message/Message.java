package cs451.message;

import cs451.listener.BListener;

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
            return that instanceof IntTriple && this.a == ((IntTriple) that).a && this.b == ((IntTriple) that).b
                    && this.c == ((IntTriple) that).c;
        }

        @Override
        public int hashCode() {
            return (a * 31) + (b * 17) + (c * 13);
        }

        @Override
        public String toString() {
            return "(" + a + "," + b + "," + c + ")";
        }
    }

    // note: ID is maximum 1 byte!
    private final int originId;
    private final int messageId;
    private final int lastHop;
    private final boolean ack;
    private final long seqNumber;
    private final BListener listener;
    private final boolean alreadyHandled;

    private Message(int originId, int messageId, int lastHop, long seqNumber, boolean ack, BListener listener,
            boolean alreadyHandled) {
        this.originId = originId;
        this.messageId = messageId;
        this.lastHop = lastHop;
        this.ack = ack;
        this.seqNumber = seqNumber;
        this.listener = listener;
        this.alreadyHandled = alreadyHandled;
    }

    public static Message createMessage(int originId, int messageId, BListener listener) {
        return new Message(originId, messageId, originId, -1, false, listener, false);
    }

    public static Message createMessage(int originId, int messageId) {
        return createMessage(originId, messageId, m -> {
        });
    }

    public Message toAck(int myId) {
        return new Message(originId, messageId, myId, seqNumber, true, listener, alreadyHandled);
    }

    public Message changeLastHop(int myId) {
        return new Message(originId, messageId, myId, seqNumber, ack, listener, alreadyHandled);
    }

    public Message changeSeqNumber(long seqNumber) {
        return new Message(originId, messageId, lastHop, seqNumber, ack, listener, alreadyHandled);
    }

    public Message setFlagAlreadyHandled(boolean alreadyHandled) {
        return new Message(originId, messageId, lastHop, seqNumber, ack, listener, alreadyHandled);
    }

    public Message resetSignalBroadcast() {
        return new Message(originId, messageId, lastHop, seqNumber, ack, m -> {
        }, alreadyHandled);
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

    public IntTriple getFullId() {
        return new IntTriple(originId, messageId, lastHop);
    }

    public boolean isCorrectAck(Message that) {
        return ack && this.getOriginId() == that.getOriginId() && this.getMessageId() == that.getMessageId();
    }

    public void signalBroadcast() {
        listener.apply(this);
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
        return new Message(originId, messageId, lastHop, seqNumber, ack, m -> {
        }, false);
    }
}
