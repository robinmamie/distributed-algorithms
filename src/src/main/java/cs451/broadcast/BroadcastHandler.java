package cs451.broadcast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import cs451.message.Message;
import cs451.parser.Parser;

public class BroadcastHandler {

    private static Broadcast b;

    static void create(boolean isFifo, Parser parser, BlockingQueue<String> toOutput) {
        if (isFifo) {
            int myPort = parser.hosts().get(parser.myId() - 1).getPort();
            b = new UrbFifo(myPort, parser.hosts(), parser.myId(), m -> {
                try {
                    toOutput.put("d " + m.getOriginId() + " " + m.getMessageId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            });
        } else {
            // TODO create LCausalBroadcast
        }
    }

    static void start(boolean isFifo, Parser parser, BlockingQueue<String> toOutput) {
        if (isFifo) {
            startFifo(parser, toOutput);
        } else {
            startLCausal(parser);
        }
    }

    private static void startFifo(Parser parser, BlockingQueue<String> toOutput) {
        int nbMessages = readConfig(parser.config());
        final int N = nbMessages;

        // Broadcast
        for (int i = 1; i <= N; ++i) {
            final Message m = Message.createMessage(parser.myId(), i);
            b.broadcast(m);
            try {
                toOutput.put("b " + m.getMessageId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void startLCausal(Parser parser) {
        throw new RuntimeException("LCausal-Broadcast not implemented!");
    }

    private static int readConfig(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            return Integer.parseInt(line);
        } catch (IOException e) {
            System.err.println("Problem with the config file!");
            return 0;
        }
    }
}
