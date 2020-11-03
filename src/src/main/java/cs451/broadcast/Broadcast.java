package cs451.broadcast;

import cs451.message.Message;
import cs451.parser.Coordinator;
import cs451.parser.Parser;

public interface Broadcast {
    void broadcast(Message m);
    void broadcastRange(int originId, int mId);

    static void prepare(boolean isFifo, Parser parser, Coordinator coordinator) {
        BroadcastHandler.create(isFifo, parser, coordinator);
    }

    static void handle(boolean isFifo, Parser parser) {
        BroadcastHandler.start(isFifo, parser);
    }

    static void flushLog() {
        BroadcastHandler.flushLog(true);
    }

    long getLocallyLastDeliveredMessage();
}
