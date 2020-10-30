package cs451.link;

import java.util.List;
import java.util.function.IntConsumer;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

class PerfectLink extends AbstractLink {

    private final StubbornLink sLink;

    public PerfectLink(int port, List<Host> hosts, BListener listener, int myId, IntConsumer broadcastListener) {
        super(listener, myId);
        this.sLink = new StubbornLink(port, hosts, this::deliver, myId, broadcastListener);
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

    @Override
    public void sendRange(int hostId, int originId, int mId) {
        sLink.sendRange(hostId, originId, mId);
    }
}
