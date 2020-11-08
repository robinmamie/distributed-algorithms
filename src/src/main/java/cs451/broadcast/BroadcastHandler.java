package cs451.broadcast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import cs451.parser.Coordinator;
import cs451.parser.Parser;

/**
 * Concrete broadcast handler doing all the initial work.
 */
public class BroadcastHandler {

    /**
     * The main broadcaster to be used (either FIFO or LCausal).
     */
    private static Broadcast b;

    /**
     * Number of messages to be broadcast.
     */
    private static int nbMessagesToBroadcast;

    /**
     * Buffered writer used to write the required information to disk.
     */
    private static BufferedWriter writer;

    private BroadcastHandler() {
    }

    /**
     * Flush the log (to be used at the end of the program's lifetime).
     */
    public static void flushLog() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Cannot close output file.");
        }
    }

    /**
     * Prepare the broadcasters and links, to be done before accessing the barrier.
     *
     * @param isFifo      whether the broadcast is FIFO or LCausal.
     * @param parser      the original parser of the program.
     * @param coordinator the original coordinator of the program.
     */
    public static void create(boolean isFifo, Parser parser, Coordinator coordinator) {
        try {
            writer = new BufferedWriter(new FileWriter(parser.output()));
        } catch (IOException e1) {
            throw new RuntimeException("Cannot create output file.");
        }

        if (isFifo) {
            nbMessagesToBroadcast = readFifoConfig(parser.config());
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

    /**
     * Start broadcasting messages.
     *
     * @param isFifo whether the broadcast is FIFO or LCausal.
     */
    public static void start(boolean isFifo) {
        if (isFifo) {
            startFifo();
        } else {
            startLCausal();
        }
    }

    /**
     * Start FIFO-broadcasting messages.
     */
    private static void startFifo() {
        b.broadcastRange(nbMessagesToBroadcast);
    }

    /**
     * Start LCausal-broadcasting messages.
     */
    private static void startLCausal() {
        throw new RuntimeException("LCausal-Broadcast not implemented!");
    }

    /**
     * Read the number of messages to be broadcast from the FIFO config file.
     *
     * @param path the path to the config file.
     * @return the number of messages to be broadcast.
     */
    private static int readFifoConfig(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            return Integer.parseInt(line);
        } catch (IOException e) {
            throw new RuntimeException("Problem with the config file!");
        }
    }
}
