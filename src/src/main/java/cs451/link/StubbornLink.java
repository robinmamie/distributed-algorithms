package cs451.link;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.listener.LinkListener;
import cs451.message.Message;

public class StubbornLink extends AbstractLink {

    public static final long TIMEOUT_MS = 100;
    // TODO should the number of threads be equal to the number of hosts?
    private static final int STUBBORN_THREADS = 3;

    private final FairLossLink flLink;
    private final BlockingQueue<CustomPacket> sendQueue = new LinkedBlockingQueue<>();
    private final Set<CustomPacket> ackSet = ConcurrentHashMap.newKeySet();

    private class CustomPacket {
        public Message message;
        public InetAddress address;
        public int port;
        public long timestamp;
        public long timeout;
        public LinkListener listener;

        public CustomPacket(Message m, InetAddress a, int p, LinkListener l) {
            message = m;
            address = a;
            port = p;
            timestamp = System.currentTimeMillis();
            timeout = TIMEOUT_MS;
            listener = l;
        }

        private void resetTimestamp() {
            timestamp = System.currentTimeMillis();
        }

        public boolean shouldResend() {
            if (System.currentTimeMillis() - timestamp < timeout) {
                return false;
            }
            if (timeout < 32 * TIMEOUT_MS) {
                timeout *= 2;
            }
            resetTimestamp();
            return true;
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
        try {
            CustomPacket cp = new CustomPacket(message, address, port, null);
            LinkListener confirmAck = (m, a, p) -> {
                if (m.isAck(message) && a.equals(address) && p == port) {
                    ackSet.add(cp);
                }
            };
            cp.listener = confirmAck;
            flLink.addListener(confirmAck);
            flLink.send(message, address, port);
            sendQueue.put(cp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void stubbornSend() {
        while (true) {
            CustomPacket cp;
            try {
                cp = sendQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }

            if (ackSet.contains(cp)) {
                ackSet.remove(cp);
                flLink.removeListener(cp.listener);
            } else {
                if (cp.shouldResend()) {
                    flLink.send(cp.message, cp.address, cp.port);
                }
                try {
                    sendQueue.put(cp);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
