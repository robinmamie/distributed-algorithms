package cs451.listener;

import cs451.message.Packet;

/**
 * Functional interface serving the general purpose of delivering packets.
 */
@FunctionalInterface
public interface PListener {
    /**
     * Deliver, or apply the list of messages to the given function.
     *
     * @param packet The packet to deliver.
     */
    void apply(Packet packet);
}
