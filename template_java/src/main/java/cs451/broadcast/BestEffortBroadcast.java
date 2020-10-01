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

    public BestEffortBroadcast(int port, List<Host> hosts, BListener deliver) {
        this.link = Link.getLink(port);
        link.addListener((m, a, p) -> deliver.apply(m));
        this.hosts = hosts;
    }

    @Override
    public void broadcast(Message m) {
        for (Host host: hosts) {
            try {
                link.send(m, InetAddress.getByName(host.getIp()), host.getPort());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }
}
