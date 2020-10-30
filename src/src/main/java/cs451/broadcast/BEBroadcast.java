package cs451.broadcast;

import java.util.List;
import java.util.function.IntConsumer;

import cs451.link.Link;
import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

class BEBroadcast implements Broadcast {

    private final List<Host> hosts;
    private final int myId;
    private final BListener deliver;

    private final Link link;

    public BEBroadcast(int port, List<Host> hosts, int myId, BListener deliver, IntConsumer broadcastListener) {
        this.hosts = hosts;
        this.myId = myId;
        this.deliver = deliver;

        this.link = Link.getLink(port, hosts, deliver, myId, broadcastListener);
    }

    @Override
    public void broadcast(Message m) {
        // Message from distant host already acked, so no need to resend the message
        int sentFrom = m.getLastHop();
        for (Host host : hosts) {
            if (host.getId() != sentFrom || sentFrom == myId) {
                if (myId != host.getId()) {
                    link.send(m, host.getId());
                } else {
                    deliver.apply(m);
                }
            }
        }
    }

    public long getLocallyLastDeliveredMessage() {
        return -1;
    }

    @Override
    public void broadcastRange(int originId, int mId) {
        if (originId != myId) {
            throw new RuntimeException("Message ranges can only be broadcast from origin!");
        }
        for (Host host : hosts) {
            if (myId != host.getId()) {
                link.sendRange(host.getId(), originId, mId);
            }
        }
    }
}
