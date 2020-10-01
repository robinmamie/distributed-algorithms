package cs451.link;

import java.net.InetAddress;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cs451.listener.LinkListener;
import cs451.message.Message;

public class StubbornLink extends AbstractLink {

    public static final long TIMEOUT = 100_000;

    private final FairLossLink flLink;

    public StubbornLink(int port) {
        this.flLink = new FairLossLink(port);
        flLink.addListener((m, a, p) -> {
            if (!m.isAck()) {
                flLink.send(m.toAck(), a, p);
                handleListeners(m, a, p);
            }
        });
    }

    @Override
    public boolean send(Message message, InetAddress address, int port) {
        final Lock lock = new ReentrantLock();
        Condition acked = lock.newCondition();
        boolean noAck = true;

        LinkListener confirmAck = (m, a, p) -> {
            if (m.isAck(message) && a.equals(address) && p == port) {
                synchronized (lock) {
                    acked.signalAll();
                }
            }
        };

        flLink.addListener(confirmAck);
        while (noAck) {
            if (flLink.send(message, address, port)) {
                try {
                    lock.lock();
                    noAck = (acked.awaitNanos(TIMEOUT) == 0);
                    lock.unlock();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
        flLink.removeListener(confirmAck);

        return true;
    }    
}
