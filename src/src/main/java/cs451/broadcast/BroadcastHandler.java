package cs451.broadcast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.message.Message;
import cs451.parser.Parser;

public class BroadcastHandler {

    static void start(boolean isFifo, Parser parser, List<String> toOutput) {
        if (isFifo) {
            startFifo(parser, toOutput);
        } else {
            startLCausal(parser);
        }
    }

    private static void startFifo(Parser parser, List<String> toOutput) {
        int nbMessages = readConfig(parser.config());
        final int N = nbMessages;

        BlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();
        new Thread(() -> {
            int myPort = parser.hosts().get(parser.myId() - 1).getPort();
            Broadcast b = new FifoBroadcast(myPort, parser.hosts(), parser.myId(), m -> {
                toOutput.add("d " + m.getOriginId() + " " + m.getMessageId());
                try {
                    queue.put(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            });

            // Broadcast
            for (int i = 1; i <= N; ++i) {
                final Message m = new Message(parser.myId(), i);
                b.broadcast(m);
                toOutput.add("b " + m.getMessageId());
            }
        }).start();
        try {
            while (nbMessages > 0) {
                queue.take();
                nbMessages -= 1;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
