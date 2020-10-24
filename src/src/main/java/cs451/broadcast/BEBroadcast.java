package cs451.broadcast;

import java.util.List;

import cs451.link.Link;
import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

class BEBroadcast implements Broadcast {

    private final List<Host> hosts;
    private final int myId;
    private final BListener deliver;

    private final Link link;

    public BEBroadcast(int port, List<Host> hosts, int myId, BListener deliver) {
        this.hosts = hosts;
        this.myId = myId;
        this.deliver = deliver;

        this.link = Link.getLink(port, hosts, deliver, myId);
    }

    @Override
    public void broadcast(Message m) {
        // Message from distant host already acked, so no need to resend the message
        int sentFrom = m.getLastHop();
        m = m.changeLastHop(myId);
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
}
