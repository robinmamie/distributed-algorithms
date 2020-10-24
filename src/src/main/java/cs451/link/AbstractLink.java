package cs451.link;

import cs451.listener.BListener;
import cs451.message.Message;

abstract class AbstractLink implements Link {

    private final BListener listener;
    private final int myId;

    protected AbstractLink(BListener listener, int myId) {
        this.listener = listener;
        this.myId = myId;
    }

    protected void handleListener(Message m) {
        listener.apply(m);
    }

    protected int getMyId() {
        return myId;
    }
}
