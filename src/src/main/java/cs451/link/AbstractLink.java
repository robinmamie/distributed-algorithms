package cs451.link;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cs451.listener.LinkListener;
import cs451.message.Message;

abstract class AbstractLink implements Link {

    // TODO look for better data structure
    private final List<LinkListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    abstract public void send(Message message, InetAddress address, int port);

    @Override
    public void addListener(LinkListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(LinkListener listener) {
        listeners.remove(listener);
    }

    protected void handleListeners(Message m, InetAddress a, int p) {
        if (m != null) {
            for (LinkListener listener: listeners) {
                listener.apply(m, a, p);
            }
        }
    }
    
}
