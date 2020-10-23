package cs451.link;

import java.net.InetAddress;

import cs451.listener.BListener;
import cs451.message.Message;

abstract class AbstractLink implements Link {

    private BListener listener;

    @Override
    public void addListener(BListener listener) {
        this.listener = listener;
    }

    protected void handleListeners(Message m, InetAddress a, int p) {
        listener.apply(m);
    }
}
