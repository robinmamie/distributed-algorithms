package cs451.broadcast;

import java.util.List;

import cs451.message.Message;
import cs451.parser.Parser;

public interface Broadcast {
    void broadcast(Message m);

    static void prepare(boolean isFifo, Parser parser, List<String> toOutput) {
        BroadcastHandler.create(isFifo, parser, toOutput);
    }

    static void handle(boolean isFifo, Parser parser, List<String> toOutput) {
        BroadcastHandler.start(isFifo, parser, toOutput);
    }
}
