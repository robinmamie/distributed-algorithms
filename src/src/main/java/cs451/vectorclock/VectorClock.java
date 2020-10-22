package cs451.vectorclock;

import java.util.Set;
import java.util.TreeSet;

public class VectorClock {

    private long vectorClock = 0L;
    private final Set<Long> pendingElements = new TreeSet<>();

    public void addMember(long e) {
        synchronized (pendingElements) {
            if (e == vectorClock + 1) {
                do {
                    vectorClock += 1;
                    pendingElements.remove(vectorClock);
                } while (pendingElements.contains(vectorClock + 1));
            } else {
                pendingElements.add(e);
            }
        }
    }

    public boolean isPast(long e) {
        return e <= vectorClock || pendingElements.contains(e);
    }

    public long getStateOfVc() {
        return vectorClock;
    }
}
