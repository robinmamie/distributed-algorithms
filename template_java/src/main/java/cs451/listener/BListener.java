package cs451.listener;

import cs451.message.Message;

@FunctionalInterface
public interface BListener {
    void apply(Message message);
}
