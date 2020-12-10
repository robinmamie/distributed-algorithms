package cs451.broadcast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import cs451.link.AbstractLink;
import cs451.link.HostInfo;
import cs451.message.Message;
import cs451.parser.Coordinator;
import cs451.parser.Parser;

/**
 * Concrete broadcast handler doing all the initial broadcasting work.
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
     * The ID of the local host.
     */
    private static int myId;

    /**
     * The buffered writer used to write the required information to disk.
     */
    private static BufferedWriter writer;

    /**
     * The coordinator of the program.
     */
    private static Coordinator coordinator;

    private BroadcastHandler() {
        // Everything is statically done in this class.
    }

    /**
     * Flush the log (to be used at the end of the program's lifetime).
     */
    public static void closeLog() {
        try {
            writer.close();
        } catch (IOException e) {
            System.err.println("Cannot close output file.");
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
            System.err.println("Cannot create output file.");
        }

        BroadcastHandler.coordinator = coordinator;
        int myPort = parser.hosts().get(parser.myId() - 1).getPort();
        myId = parser.myId();

        if (isFifo) {
            nbMessagesToBroadcast = readFifoConfig(parser.config());
            broadcast = new FIFOBroadcast(myPort, parser.hosts(), myId, BroadcastHandler::writeDeliver,
                    BroadcastHandler::writeBroadcast);
        } else {
            Map<Integer, List<Integer>> dependencies = new TreeMap<>();
            nbMessagesToBroadcast = readLCausalConfig(parser.config(), dependencies);
            broadcast = new LCausalBroadcast(myPort, parser.hosts(), myId, BroadcastHandler::writeDeliver,
                    BroadcastHandler::writeBroadcast, dependencies);
        }
    }

    /**
     * Write to the log when a message is delivered.
     *
     * @param message The delivered message.
     */
    private static void writeDeliver(Message message) {
        try {
            synchronized (writer) {
                writer.write(String.format("d %d %d", message.getOriginId(), message.getMessageId()));
                writer.newLine();
            }
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
            synchronized (writer) {
                writer.write(String.format("b %d", messageId));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (messageId == nbMessagesToBroadcast) {
            System.out.println("Signaling end of broadcasting messages");
            coordinator.finishedBroadcasting();
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
        Map<Integer, HostInfo> hostInfo = AbstractLink.getHostInfo();
        int memoryLimit = 20_000 / hostInfo.size();
        for (int i = 1; i <= nbMessagesToBroadcast; ++i) {
            broadcast.broadcast(Message.createMessage(myId, i));
            if (i % memoryLimit == 0) {
                try {
                    Thread.sleep(1000L * hostInfo.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
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
            System.err.println("Problem with the config file!");
            return 0;
        }
    }

    /**
     * Read the number of messages to be broadcast from the LCausal config file, and
     * fill the dependency list.
     *
     * @param path The path to the config file.
     * @return The number of messages to be broadcast.
     */
    private static int readLCausalConfig(String path, Map<Integer, List<Integer>> dependencies) {
        int nbMessages = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            nbMessages = Integer.parseInt(line);
            int currentId = 1;
            while (null != (line = reader.readLine())) {
                List<Integer> list = new LinkedList<Integer>();
                try (Scanner scanner = new Scanner(line)) {
                    while (scanner.hasNextInt()) {
                        int process = scanner.nextInt();
                        if (process != currentId) {
                            list.add(process);
                        }
                    }
                }
                dependencies.put(currentId, list);
                currentId += 1;
            }
        } catch (IOException e) {
            System.err.println("Problem with the config file!");
        }
        return nbMessages;
    }
}
