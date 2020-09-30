package cs451;

import java.net.InetAddress;
import java.net.UnknownHostException;

import cs451.broadcast.UniformReliableBroadcast;
import cs451.link.Link;
import cs451.parser.Coordinator;
import cs451.parser.Host;
import cs451.parser.Parser;

public class Main {

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //write/flush output file if necessary
        System.out.println("Writing output.");
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        long pid = parser.pid();
        System.out.println("My PID is " + pid + ".");
        System.out.println("Use 'kill -SIGINT " + pid + " ' or 'kill -SIGTERM " + pid + " ' to stop processing packets.");

        System.out.println("My id is " + parser.myId() + ".");
        System.out.println("List of hosts is:");
        for (Host host: parser.hosts()) {
            System.out.println(host.getId() + ", " + host.getIp() + ", " + host.getPort());
        }

        System.out.println("Barrier: " + parser.barrierIp() + ":" + parser.barrierPort());
        System.out.println("Signal: " + parser.signalIp() + ":" + parser.signalPort());
        System.out.println("Output: " + parser.output());
        // if config is defined; always check before parser.config()
        if (parser.hasConfig()) {
            System.out.println("Config: " + parser.config());
        }

        Coordinator coordinator = new Coordinator(parser.myId(), parser.barrierIp(), parser.barrierPort(), parser.signalIp(), parser.signalPort());

        System.out.println("Waiting for all processes for finish initialization");
        coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");
        
        // ---------------------------------------------------------------------
        // Read config file, integer contained in it indicates the number of messages to broadcast
        // TODO perfect links
        // TODO URB
        // TODO FIFO-broadcast
        // TODO L-Causal broadcast

        //Link link = Link.getLink(parser.hosts().get(parser.myId()-1).getPort());
        //link.addListener((m, a, p) -> System.out.println(m));

        Message message = new Message(parser.myId(), 1);
        int myPort = parser.hosts().get(parser.myId()-1).getPort();
        UniformReliableBroadcast urb = new UniformReliableBroadcast(myPort, (m, a, d) -> {
            System.out.println(m + " has been uniformly, reliably broadcast.");
        }, parser.hosts(), parser.myId());
        urb.broadcast(message);

        // ---------------------------------------------------------------------

        System.out.println("Signaling end of broadcasting messages");
        coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
