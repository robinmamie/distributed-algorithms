package cs451.broadcast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import cs451.Message;
import cs451.link.Link;
import cs451.listener.Listener;
import cs451.parser.Host;

public class BestEffortBroadcast implements Broadcast {

    private final Link link;
    private final List<Host> hosts;

    public BestEffortBroadcast(int port, Listener deliver, List<Host> hosts) {
        this.link = Link.getLink(port);
        link.addListener(deliver);
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
