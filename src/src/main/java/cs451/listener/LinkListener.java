package cs451.listener;

import java.net.InetAddress;

import cs451.message.Message;

// TODO return true if listener completed! Remove must happen where they are handled (not requeue if done)

@FunctionalInterface
public interface LinkListener {
    void apply(Message message, InetAddress address, int port);
}
