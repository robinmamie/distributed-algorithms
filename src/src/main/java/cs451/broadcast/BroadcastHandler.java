package cs451.broadcast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import cs451.message.Message;
import cs451.parser.Parser;

public class BroadcastHandler {

    private static final int TIMEOUT = 50;
    private static Broadcast b;

    static void create(boolean isFifo, Parser parser, List<String> toOutput) {
        if (isFifo) {
            int myPort = parser.hosts().get(parser.myId() - 1).getPort();
            b = new UrbFifo(myPort, parser.hosts(), parser.myId(), m -> {
                toOutput.add("d " + m.getOriginId() + " " + m.getMessageId());
            });
        } else {
            // TODO create LCausalBroadcast
        }
    }

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

        // Broadcast
        for (int i = 1; i <= N; ++i) {
            final Message m = Message.createMessage(parser.myId(), i);
            b.broadcast(m);
            toOutput.add("b " + m.getMessageId());
        }
        try {
            int timeout = TIMEOUT;
            int memory = 0;
            while (timeout > 0 && toOutput.size() < nbMessages * (parser.hosts().size() + 1)) {
                Thread.sleep(100);
                if (memory == b.status()) {
                    timeout -= 1;
                } else {
                    memory = b.status();
                    timeout = TIMEOUT;
                }
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
