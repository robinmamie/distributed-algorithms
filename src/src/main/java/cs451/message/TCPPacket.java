package cs451.message;

public class TCPPacket {
    private final Message message;
    private final int sender;
    private final long seqNumber;

    public TCPPacket(Message message, int sender, long seqNumber) {
        this.message = message;
        this.sender = sender;
        this.seqNumber = seqNumber;
    }
}
