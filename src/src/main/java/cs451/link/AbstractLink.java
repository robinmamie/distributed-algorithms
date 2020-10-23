package cs451.link;

import java.net.InetAddress;

import cs451.listener.BListener;
import cs451.message.Message;

abstract class AbstractLink implements Link {

    private BListener listener;

    @Override
    abstract public void send(Message message, int hostId, InetAddress address, int port);

    @Override
    public void addListener(BListener listener) {
        this.listener = listener;
    }

    @Override
    public void removeListener(BListener listener) {
        this.listener = null;
    }

    protected void handleListeners(Message m, InetAddress a, int p) {
        listener.apply(m);
    }
    
}
