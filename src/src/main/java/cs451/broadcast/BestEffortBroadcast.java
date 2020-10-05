package cs451.broadcast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import cs451.link.Link;
import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

class BestEffortBroadcast implements Broadcast {

    private final Link link;
    private final List<Host> hosts;
    private final int myId;
    private final BListener deliver;

    public BestEffortBroadcast(int port, List<Host> hosts, int myId, BListener deliver) {
        this.link = Link.getLink(port);
        this.myId = myId;
        link.addListener((m, a, p) -> deliver.apply(m));
        this.hosts = hosts;
        this.deliver = deliver;
    }

    @Override
    public void broadcast(Message m) {
        m = new Message(m, myId);
        for (Host host: hosts) {
            if (host.getId() == myId) {
                deliver.apply(m);
            } else {
                try {
                    link.send(m, InetAddress.getByName(host.getIp()), host.getPort());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
