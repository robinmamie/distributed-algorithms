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
            return (a * 7) + (b * 13);
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
            return (a * 7) + (b * 13) + (c * 2);
        }
    }

    // note: ID is maximum 1 byte!
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

    public Message(int originId, int messageId) {
        this(originId, messageId, originId, false);
    }

    public Message(int originId, int messageId, boolean ack) {
        this(originId, messageId, originId, ack);
    }

    private Message(Message that, boolean ack) {
        this.originId = that.originId;
        this.messageId = that.messageId;
        this.lastHop = that.lastHop;
        this.ack = true;
    }

    public Message(Message that, int lastHop) {
        this.originId = that.originId;
        this.messageId = that.messageId;
        this.lastHop = lastHop;
        this.ack = false;
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

    public IntPair getId() {
        return new IntPair(getOriginId(), getMessageId());
    }

    public IntTriple getFullId() {
        return new IntTriple(getOriginId(), getMessageId(), getLastHop());
    }

    public boolean isAck() {
        return ack;
    }

    public boolean isAck(Message that) {
        return ack
            && this.getOriginId() == that.getOriginId()
            && this.getMessageId() == that.getMessageId();
    }

    public Message toAck() {
        return new Message(this, true);
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Message
            && this.getFullId().equals(((Message)that).getFullId());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = 1;
        hash = prime * hash + originId;
        hash = prime * hash + messageId;
        return hash;
    }

    @Override
    public String toString() {
        return (ack ? "Ack" : "Message") + " #" + messageId + " from " + originId + ", last hop " + lastHop;
    }

    private static final byte fullMask = (byte) 0xff;

    private void intToByte(int integer, byte[] array, int offset) {
        array[offset+0] = (byte)((integer >> 24) & fullMask);
        array[offset+1] = (byte)((integer >> 16) & fullMask);
        array[offset+2] = (byte)((integer >> 8) & fullMask);
        array[offset+3] = (byte)((integer) & fullMask);
    }

    public byte[] serialize() {
        final int parameterSize = Integer.BYTES;
        byte[] datagram = new byte[parameterSize*3 + 1];
        intToByte(getOriginId(), datagram, 0);
        intToByte(getMessageId(), datagram, parameterSize);
        intToByte(getLastHop(), datagram, parameterSize*2);
        datagram[parameterSize*3] = isAck() ? fullMask : 0;
        return datagram;
    }

    private static int byteToInt(byte[] array, int offset) {
        int ret = 0;
        for (int i=offset; i<Integer.BYTES+offset; i++) {
            ret <<= 8;
            ret |= (int)array[i] & 0xFF;
        }
        return ret;
        /*int integer = 0;
        integer += array[offset+0] << 24;
        integer += array[offset+1] << 16;
        integer += array[offset+2] << 8;
        integer += array[offset+3];
        return integer;*/
    }

    public static Message deserialize(byte[] datagram) {
        final int parameterSize = Integer.BYTES;
        int originId = byteToInt(datagram, 0);
        int messageId = byteToInt(datagram, parameterSize);
        int lastHop = byteToInt(datagram, parameterSize*2);
        boolean ack = datagram[parameterSize*3] != 0;
        return new Message(originId, messageId, lastHop, ack);
    }
}
