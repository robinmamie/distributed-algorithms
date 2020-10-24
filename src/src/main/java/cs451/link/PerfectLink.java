package cs451.link;

import java.util.List;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

class PerfectLink extends AbstractLink {

    private final StubbornLink sLink;

    public PerfectLink(int port, List<Host> hosts, BListener listener, int myId) {
        super(listener, myId);
        this.sLink = new StubbornLink(port, hosts, this::deliver, myId);
    }

    @Override
    public void send(Message message, int hostId) {
        sLink.send(message, hostId);
    }

    private void deliver(Message m) {
        if (!m.isAlreadyHandled()) {
            handleListener(m);
        }
    }
}
