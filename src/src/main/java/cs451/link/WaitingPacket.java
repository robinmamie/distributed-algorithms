package cs451.link;

import cs451.message.Message;

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
     * The underlying message.
     */
    private final Message message;

    /**
     * The timestamp of the message at its creation, in milliseconds.
     */
    private final long timestamp;

    /**
     * The numeric value of the timeout, fixed for the lifetime of this wrapper
     * packet.
     */
    private final long timeout;

    /**
     * Create a "waiting" packet, giving information about when to resend the
     * message if it was not yet acked.
     *
     * @param message The underlying message.
     * @param host    The information about the distant host.
     */
    public WaitingPacket(Message message, HostInfo host) {
        this.message = message;
        this.host = host;

        timestamp = System.currentTimeMillis();
        timeout = host.getTimeout();
    }

    /**
     * Get the underlying message.
     *
     * @return The underlying message of this waiting packet.
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Check if the message has timed out, if yes, execute the given function and
     * double the timeout of the host.
     *
     * @param toExecute Function to run if the message has timed out (should trigger
     *                  a resending).
     * @return The same or a new Waiting packet.
     */
    public WaitingPacket resendIfTimedOut(Runnable toExecute) {
        if (System.currentTimeMillis() - timestamp < timeout) {
            return this;
        }
        host.testAndDouble((int) timeout);
        toExecute.run();
        return new WaitingPacket(message, host);
    }
}
