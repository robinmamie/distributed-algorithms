package cs451;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 2865064940223588810L;

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

    private final int originId;
    private final int messageId;
    private final int lastHop;
    private final boolean ack;

    public Message(int originId, int messageId) {
        this.originId = originId;
        this.messageId = messageId;
        this.lastHop = originId;
        this.ack = false;
    }

    public Message(int originId, int messageId, boolean ack) {
        this.originId = originId;
        this.messageId = messageId;
        this.lastHop = originId;
        this.ack = ack;
    }

    public Message(Message that, int lastHop) {
        this.originId = that.originId;
        this.messageId = that.messageId;
        this.lastHop = lastHop;
        this.ack = false;
    }

    private Message(Message that) {
        this.originId = that.originId;
        this.messageId = that.messageId;
        this.lastHop = that.originId;
        this.ack = true;
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
        return new Message(this);
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
        return (ack ? "Ack" : "Message") + " #" + messageId + " from " + originId;
    }

    public byte[] serialize() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        byte[] datagram = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            datagram = bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                // Ignore close exception
            }
        }
        return datagram;
    }

    public static Message unserialize(byte[] datagram) {
        // TODO corrupted packets? Should we crash?
        ByteArrayInputStream bis = new ByteArrayInputStream(datagram);
        ObjectInput in = null;
        Message message = null;
        try {
            in = new ObjectInputStream(bis);
            Object o = in.readObject();
            message = (Message) o;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // Ignore close exception
            }
        }
        return message;
    }
}
