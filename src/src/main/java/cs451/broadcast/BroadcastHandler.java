package cs451.broadcast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import cs451.parser.Coordinator;
import cs451.parser.Parser;

public class BroadcastHandler {

    private static Broadcast b;
    private static int nbMessagesToBroadcast;

    private static BufferedWriter writer;

    private BroadcastHandler() {
    }

    static void flushLog() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Cannot close output file.");
        }
    }

    static void create(boolean isFifo, Parser parser, Coordinator coordinator) {
        try {
            writer = new BufferedWriter(new FileWriter(parser.output()));
        } catch (IOException e1) {
            throw new RuntimeException("Cannot create output file.");
        }
        nbMessagesToBroadcast = readConfig(parser.config());

        if (isFifo) {
            int myPort = parser.hosts().get(parser.myId() - 1).getPort();
            b = new FIFOBroadcast(myPort, parser.hosts(), parser.myId(), m -> {
                try {
                    writer.write(String.format("d %d %d", m.getOriginId(), m.getMessageId()));
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, id -> {
                try {
                    writer.write(String.format("b %d", id));
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (id == nbMessagesToBroadcast) {
                    System.out.println("Signaling end of broadcasting messages");
                    coordinator.finishedBroadcasting();
                }
            });
        } else {
            // TODO create LCausalBroadcast
        }
    }

    static void start(boolean isFifo, Parser parser) {
        if (isFifo) {
            startFifo(parser);
        } else {
            startLCausal(parser);
        }
    }

    private static void startFifo(Parser parser) {
        b.broadcastRange(parser.myId(), nbMessagesToBroadcast);
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
