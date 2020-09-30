package cs451.broadcast;

import java.util.List;

import cs451.Message;
import cs451.listener.Listener;
import cs451.parser.Host;

public interface Broadcast {
    void broadcast(Message m);

    static Broadcast getBroadcast(int myPort, Listener l, List<Host> hosts, int myId) {
        return new UniformReliableBroadcast(myPort, l, hosts, myId);
    }
}
