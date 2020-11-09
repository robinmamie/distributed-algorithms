package cs451.broadcast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import cs451.message.Message;
import cs451.parser.Coordinator;
import cs451.parser.Parser;

/**
 * Concrete broadcast handler doing all the initial work.
 */
public class BroadcastHandler {

    /**
     * The main broadcaster to be used (either FIFO or LCausal).
     */
    private static Broadcast broadcast;

    /**
     * The number of messages to be broadcast.
     */
    private static int nbMessagesToBroadcast;

    /**
     * The buffered writer used to write the required information to disk.
     */
    private static BufferedWriter writer;

    /**
     * The coordinator of the program.
     */
    private static Coordinator coordinator;

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
     * @param isFifo      Whether the broadcast is FIFO or LCausal.
     * @param parser      The original parser of the program.
     * @param coordinator The original coordinator of the program.
     */
    public static void create(boolean isFifo, Parser parser, Coordinator coordinator) {
        try {
            writer = new BufferedWriter(new FileWriter(parser.output()));
        } catch (IOException e1) {
            throw new RuntimeException("Cannot create output file.");
        }

        BroadcastHandler.coordinator = coordinator;

        if (isFifo) {
            nbMessagesToBroadcast = readFifoConfig(parser.config());
            int myPort = parser.hosts().get(parser.myId() - 1).getPort();
            broadcast = new FIFOBroadcast(myPort, parser.hosts(), parser.myId(), BroadcastHandler::writeDeliver,
                    BroadcastHandler::writeBroadcast);
        } else {
            // TODO create LCausalBroadcast
        }
    }

    /**
     * Write to the log when a message is delivered.
     *
     * @param message The delivered message.
     */
    private static void writeDeliver(Message message) {
        try {
            writer.write(String.format("d %d %d", message.getOriginId(), message.getMessageId()));
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write to the log when a local message is broadcast.
     *
     * @param messageId The ID of the local message.
     */
    private static void writeBroadcast(int messageId) {
        try {
            writer.write(String.format("b %d", messageId));
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (messageId == nbMessagesToBroadcast) {
            System.out.println("Signaling end of broadcasting messages");
            coordinator.finishedBroadcasting();
            try {
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Start broadcasting messages.
     *
     * @param isFifo Whether the broadcast is FIFO or LCausal.
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
        broadcast.broadcastRange(nbMessagesToBroadcast);
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
     * @param path The path to the config file.
     * @return The number of messages to be broadcast.
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
