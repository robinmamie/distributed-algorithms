package cs451.link;

import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cs451.listener.LinkListener;
import cs451.message.Message;

public class StubbornLink extends AbstractLink {

    public static final long TIMEOUT_MS = 100;
    private static final int STUBBORN_THREADS = 5;

    private final FairLossLink flLink;
    private final BlockingQueue<CustomPacket> sendQueue = new LinkedBlockingQueue<>();

    private class CustomPacket {
        public Message message;
        public InetAddress address;
        public int port;

        public CustomPacket(Message m, InetAddress a, int p) {
            message = m;
            address = a;
            port = p;
        }
    }

    public StubbornLink(int port) {
        this.flLink = new FairLossLink(port);
        flLink.addListener((m, a, p) -> {
            if (!m.isAck()) {
                flLink.send(m.toAck(), a, p);
                handleListeners(m, a, p);
            }
        });
        for (int i = 0; i < STUBBORN_THREADS; ++i) {
            new Thread(() -> stubbornSend()).start();
        }
    }

    @Override
    public void send(Message message, InetAddress address, int port) {
        while (true) {
            try {
                sendQueue.put(new CustomPacket(message, address, port));
            } catch (InterruptedException e) {
                System.err.println("INTERRUPTED");
                continue;
            }
            break;
        }
    }

    private void stubbornSend() {
        BlockingQueue<Boolean> communicationQueue = new LinkedBlockingQueue<>();
        while (true) {
            boolean noAck = true;
            CustomPacket cp;
            try {
                cp = sendQueue.take();
            } catch (InterruptedException e1) {
                System.err.println("INTERRUPTED");
                continue;
            }

            Message message = cp.message;
            InetAddress address = cp.address;
            int port = cp.port;

            LinkListener confirmAck = (m, a, p) -> {
                if (m.isAck(message) && a.equals(address) && p == port) {
                    while (true) {
                        try {
                            communicationQueue.put(true);
                        } catch (InterruptedException e) {
                            System.err.println("INTERRUPTED");
                            continue;
                        }
                        break;
                    }
                }
            };

            flLink.addListener(confirmAck);
            while (noAck) {
                flLink.send(message, address, port);
                try {
                    noAck = (communicationQueue.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS) == null);
                } catch (InterruptedException e) {
                    // Ignore
                    System.err.println("INTERRUPTED");
                }
            }
            flLink.removeListener(confirmAck);
        }
    }
}
