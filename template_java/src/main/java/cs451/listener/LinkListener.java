package cs451.listener;

import java.net.InetAddress;

import cs451.message.Message;

@FunctionalInterface
public interface LinkListener {
    void apply(Message message, InetAddress address, int port);
}
