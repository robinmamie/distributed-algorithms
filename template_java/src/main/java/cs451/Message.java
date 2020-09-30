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

    private final int originId;
    private final int messageId;
    private final String content;
    private final boolean ack;

    public Message(int originId, int messageId, String content) {
        this.originId = originId;
        this.messageId = messageId;
        this.content = content;
        this.ack = false;
    }

    public Message(int originId, int messageId) {
        this.originId = originId;
        this.messageId = messageId;
        this.content = "";
        this.ack = true;
    }

    private Message(Message that) {
        this.originId = that.originId;
        this.messageId = that.messageId;
        this.content = "";
        this.ack = true;
    }

    public int getOriginId() {
        return originId;
    }

    public int getMessageId() {
        return messageId;
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
        hash = prime * hash + ((content == null) ? 0 : content.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return (ack ? "Ack" : "Message") + " #" + messageId + " from " + originId + ": " + content;
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
