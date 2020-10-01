package cs451.link;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cs451.listener.LinkListener;
import cs451.message.Message;

abstract class AbstractLink implements Link {

    // TODO look for better data structure
    private final List<LinkListener> listeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    abstract public boolean send(Message message, InetAddress address, int port);

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
            synchronized (listeners) {
                for (LinkListener listener: listeners) {
                    listener.apply(m, a, p);
                }
            }
        }
    }
    
}
