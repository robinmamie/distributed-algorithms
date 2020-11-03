package cs451.broadcast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import cs451.parser.Coordinator;
import cs451.parser.Parser;

public class BroadcastHandler {

    private static Broadcast b;
    private static int nbMessagesToBroadcast;

    private static StringBuilder output = null;
    private static Writer writer;
    private static final int SB_LIMIT = 100000;
    private static final int SB_SIZE = SB_LIMIT + 100;

    private BroadcastHandler() {
    }

    static void flushLog(boolean force) {
        if (output.length() > SB_LIMIT || force) {
            try {
                writer.write(output.toString());
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException("Cannot write to output file.");
            }
        }
        if (force) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Cannot close output file.");
            }
        }
    }

    static void create(boolean isFifo, Parser parser, Coordinator coordinator) {
        try {
            writer = new BufferedWriter(new FileWriter(parser.output()));
        } catch (IOException e1) {
            throw new RuntimeException("Cannot create output file.");
        }
        output = new StringBuilder(SB_SIZE);
        nbMessagesToBroadcast = readConfig(parser.config());

        if (isFifo) {
            int myPort = parser.hosts().get(parser.myId() - 1).getPort();
            b = new FIFOBroadcast(myPort, parser.hosts(), parser.myId(), m -> {
                output
                    .append("d ")
                    .append(m.getOriginId())
                    .append(" ")
                    .append(m.getMessageId())
                    .append(System.lineSeparator());
            }, id -> {
                output
                    .append("b ")
                    .append(id)
                    .append(System.lineSeparator());
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
