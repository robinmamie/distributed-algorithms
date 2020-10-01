package cs451.broadcast;

import java.util.List;

import cs451.message.Message;
import cs451.parser.Parser;

public interface Broadcast {
    void broadcast(Message m);

    static Broadcast handle(boolean isFifo, Parser parser, List<String> toOutput) {
        return BroadcastHandler.start(isFifo, parser, toOutput);
    }
}
