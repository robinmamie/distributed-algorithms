package cs451.link;

import java.net.InetAddress;

import cs451.listener.LinkListener;
import cs451.message.Message;

abstract class AbstractLink implements Link {

    //private final List<LinkListener> listeners = new CopyOnWriteArrayList<>();
    private LinkListener listener;
    //private final Logger logger = new Logger(this);

    @Override
    abstract public void send(Message message, InetAddress address, int port);

    @Override
    public void addListener(LinkListener listener) {
        //listeners.add(listener);
        this.listener = listener;
    }

    @Override
    public void removeListener(LinkListener listener) {
        //listeners.remove(listener);
        this.listener = null;
    }

    protected void handleListeners(Message m, InetAddress a, int p) {
        /*if (m != null) {
            int size = listeners.size();
            for (LinkListener listener: listeners) {
                listener.apply(m, a, p);
            }
            logger.log("end cycle listeners ("+ size +")");
        }*/
        listener.apply(m, a, p);
    }
    
}
