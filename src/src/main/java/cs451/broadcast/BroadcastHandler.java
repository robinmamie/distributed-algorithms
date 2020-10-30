package cs451.broadcast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.parser.Parser;

public class BroadcastHandler {

    private static Broadcast b;
    private static BlockingQueue<Integer> bq = new LinkedBlockingQueue<>();

    private BroadcastHandler() {
    }

    static void create(boolean isFifo, Parser parser, BlockingQueue<String> toOutput) {
        if (isFifo) {
            int myPort = parser.hosts().get(parser.myId() - 1).getPort();
            b = new FIFOBroadcast(myPort, parser.hosts(), parser.myId(), m -> {
                try {
                    toOutput.put("d " + m.getOriginId() + " " + m.getMessageId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }, value -> {
                try {
                    bq.put(value);
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
        final int nbMessages = readConfig(parser.config());

        b.broadcastRange(parser.myId(), nbMessages);

        // Broadcast
        for (int i = 1; i <= nbMessages; ++i) {
            try {
                int reported = 0;
                while (reported < i) {
                    reported = bq.take();
                }
                toOutput.put("b " + i);
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
