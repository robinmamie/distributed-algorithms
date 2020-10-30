package cs451.vectorclock;

import cs451.message.MessageRange;

public class VectorClock {

    private final MessageRange range = new MessageRange();
    
    public VectorClock() {
        range.add(0);
    }

    public void addMember(long e) {
        range.add(e);
    }

    public boolean isPast(long e) {
        return range.contains(e);
    }

    public long getStateOfVc() {
        return range.endOfFirstRange();
    }

    public String getRange() {
        return range.toString();
    }
}
