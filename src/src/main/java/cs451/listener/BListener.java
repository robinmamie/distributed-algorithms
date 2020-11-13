package cs451.listener;

import cs451.message.Message;

/**
 * Functional interface serving the general purpose of delivering messages.
 */
@FunctionalInterface
public interface BListener {

    /**
     * Deliver, or apply the message to the given function.
     *
     * @param message The message.
     */
    void apply(Message message);
}
