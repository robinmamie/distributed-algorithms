package cs451;

import cs451.broadcast.BroadcastHandler;
import cs451.parser.Coordinator;
import cs451.parser.Host;
import cs451.parser.Parser;

public class Main {

    private static void handleSignal() {
        // immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        // write/flush output file if necessary
        BroadcastHandler.closeLog();
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
        System.out
                .println("Use 'kill -SIGINT " + pid + " ' or 'kill -SIGTERM " + pid + " ' to stop processing packets.");

        System.out.println("My id is " + parser.myId() + ".");
        System.out.println("List of hosts is:");
        for (Host host : parser.hosts()) {
            System.out.println(host.getId() + ", " + host.getIp() + ", " + host.getPort());
        }

        System.out.println("Barrier: " + parser.barrierIp() + ":" + parser.barrierPort());
        System.out.println("Signal: " + parser.signalIp() + ":" + parser.signalPort());
        System.out.println("Output: " + parser.output());
        // if config is defined; always check before parser.config()
        if (parser.hasConfig()) {
            System.out.println("Config: " + parser.config());
        }

        Coordinator coordinator = new Coordinator(parser.myId(), parser.barrierIp(), parser.barrierPort(),
                parser.signalIp(), parser.signalPort());

        final boolean lcausal = false;

        BroadcastHandler.create(lcausal, parser, coordinator);

        System.out.println("Waiting for all processes for finish initialization");
        coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");

        BroadcastHandler.start(lcausal);

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60L * 60 * 1000);
        }
    }
}
