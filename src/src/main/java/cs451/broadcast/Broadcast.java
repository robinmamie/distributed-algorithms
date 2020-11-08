package cs451.broadcast;

import cs451.message.Message;

/**
 * Defines the global Broadcast instance, i.e. what the main part of the program
 * can see.
 */
interface Broadcast {

    /**
     * Broadcast a single message.
     *
     * @param m the message to be broadcast.
     */
    void broadcast(Message message);

    /**
     * Broadcast an entire range of messages at once.
     *
     * @param numberOfMessages number of messages to be broadcast: effectively put
     *                         message 1 to numberOfMessages in a buffer.
     */
    void broadcastRange(int numberOfMessages);
}
