package cs451.listener;

import java.net.InetAddress;

import cs451.Message;

@FunctionalInterface
public interface Listener {
    void apply(Message message, InetAddress address, int port);
}
