package cs451.link;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cs451.message.Message;

class PerfectLink extends AbstractLink {

    private final StubbornLink sLink;
    private final Set<Message.IntTriple> delivered = ConcurrentHashMap.newKeySet();

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
