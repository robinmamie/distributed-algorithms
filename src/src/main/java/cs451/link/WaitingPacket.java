package cs451.link;

import cs451.message.Message;

class WaitingPacket {

    private final HostInfo host;
    private final Message message;
    private final long timestamp;
    private final long timeout;

    public WaitingPacket(Message message, HostInfo host) {
        this.message = message;
        this.host = host;

        timestamp = System.currentTimeMillis();
        timeout = host.getTimeout();
    }

    public Message getMessage() {
        return message;
    }

    public WaitingPacket resendIfTimedOut(Runnable toExecute) {
        if (System.currentTimeMillis() - timestamp < timeout) {
            return this;
        }
        host.testAndDouble(timeout);
        toExecute.run();
        return new WaitingPacket(message, host);
    }
}
