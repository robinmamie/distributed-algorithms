package cs451.link;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cs451.Message;

class PerfectLink extends AbstractLink {

    private final StubbornLink sLink;
    // TODO low performance synchronization (?), could use timestamp-based messages (p.40)
    private final Set<Message.IntTriple> delivered = Collections.synchronizedSet(new HashSet<>());

    public PerfectLink(int port) {
        this.sLink = new StubbornLink(port);
        sLink.addListener((m, a, p) -> {
            Message.IntTriple id = m.getFullId();
            if (!delivered.contains(id)) {
                delivered.add(id);
                handleListeners(m, a, p);
            }
        });
    }

    @Override
    public boolean send(Message message, InetAddress address, int port) {
        new Thread(() -> sLink.send(message, address, port)).start();
        return true;
    }

}
