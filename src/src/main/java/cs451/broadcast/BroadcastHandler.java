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

    static Broadcast start(boolean isFifo, Parser parser, List<String> toOutput) {
        if (isFifo) {
            return startFifo(parser, toOutput);
        }
        return startLCausal(parser);
    }

    private static Broadcast startFifo(Parser parser, List<String> toOutput) {
        int myPort = parser.hosts().get(parser.myId() - 1).getPort();

        BlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();
        Broadcast b = new FifoBroadcast(myPort, parser.hosts(), parser.myId(), m -> {
            toOutput.add("d " + m.getOriginId() + " " + m.getMessageId());
            while (true) {
                try {
                    queue.put(true);
                    break;
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        });

        // Broadcast
        int nbMessages = readConfig(parser.config());
        for (int i = 1; i <= nbMessages; ++i) {
            final Message m = new Message(parser.myId(), i);
            b.broadcast(m);
            toOutput.add("b " + m.getMessageId());
        }
        while (nbMessages > 0) {
            try {
                while (nbMessages > 0) {
                    queue.take();
                    nbMessages -= 1;
                }
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        return b;
    }

    private static Broadcast startLCausal(Parser parser) {
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
