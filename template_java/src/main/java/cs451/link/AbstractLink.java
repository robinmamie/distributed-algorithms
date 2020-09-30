package cs451.link;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cs451.Message;
import cs451.listener.Listener;

abstract class AbstractLink implements Link {

    private final List<Listener> listeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    abstract public boolean send(Message message, InetAddress address, int port);

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    protected void handleListeners(Message m, InetAddress a, int p) {
        if (m != null) {
            synchronized (listeners) {
                for (Listener listener: listeners) {
                    listener.apply(m, a, p);
                }
            }
        }
    }
    
}
