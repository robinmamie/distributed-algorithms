package cs451.link;

import cs451.message.Packet;

/**
 * Implements an abstraction above the simple message. It mainly gives
 * information about the timeout.
 */
class WaitingPacket {

    /**
     * The information about the distant host.
     */
    private final HostInfo host;

    /**
     * The underlying packet.
     */
    private final Packet packet;

    /**
     * The timestamp of the message at its creation, in milliseconds.
     */
    private final long timestamp;

    /**
     * The host's timeout at he creation of this packet.
     */
    private final long timeout;

    /**
     * Create a "waiting" packet, giving information about when to resend the
     * message if it was not yet acked.
     *
     * @param message The underlying message.
     * @param host    The information about the distant host.
     */
    public WaitingPacket(Packet packet, HostInfo host) {
        this.packet = packet;
        this.host = host;
        this.timeout = host.getTimeout();
        timestamp = System.currentTimeMillis();
    }

    /**
     * Get the underlying packet.
     *
     * @return The underlying packet of this waiting packet.
     */
    public Packet getPacket() {
        return packet;
    }

    /**
     * Check if the message has timed out, if yes, execute the given function and
     * adapt the timeout of the host.
     *
     * @param toExecute Function to run if the message has timed out (should trigger
     *                  a resending).
     * @return The same or a new WaitingPacket.
     */
    public WaitingPacket resendIfTimedOut(Runnable toExecute) {
        if (System.currentTimeMillis() - timestamp < timeout) {
            return this;
        }
        host.exponentialBackOff();
        toExecute.run();
        return new WaitingPacket(packet, host);
    }
}
