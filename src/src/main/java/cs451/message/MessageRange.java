package cs451.message;

import java.util.LinkedList;

public class MessageRange {

    private final LinkedList<Long> ranges = new LinkedList<>();

    public void setRange(long a, long b) {
        synchronized (ranges) {
            ranges.clear();
            ranges.add(a);
            ranges.add(b);
        }
    }

    /**
     * 
     * @param e
     * @return true if element was not yet present, false otherwise
     */
    public boolean add(long e) {
        synchronized (ranges) {
            for (int i = 0; i < ranges.size(); i += 2) {
                long startOfSubRange = ranges.get(i);
                long endOfSubRange = ranges.get(i+1);
                if (e < startOfSubRange) {
                    if (e == startOfSubRange - 1) {
                        ranges.set(i, e);
                    } else {
                        ranges.add(i, e);
                        ranges.add(i, e);
                    }
                    return true;
                } else if (e <= endOfSubRange) {
                    return false;
                } else if (e == endOfSubRange+1) {
                    if (i+2 < ranges.size() && ranges.get(i+2) == e+1) {
                        ranges.remove(i+2);
                        ranges.remove(i+1);
                    } else {
                        ranges.set(i+1, e);
                    }
                    return true;
                }
            }
            ranges.add(e);
            ranges.add(e);
            return true;
        }
    }

    public long poll() {
        synchronized (ranges) {
            if (ranges.isEmpty()) {
                return Integer.MIN_VALUE;
            }
            long firstElement = ranges.get(0);
            if (firstElement == ranges.get(1)) {
                ranges.remove(1);
                ranges.remove(0);
            } else {
                ranges.set(0, firstElement+1);
            }
            return firstElement;
        }
    }

    public long peek() {
        synchronized (ranges) {
            if (ranges.isEmpty()) {
                return Integer.MIN_VALUE;
            }
            return ranges.get(0);
        }
    }

    public boolean contains(long e) {
        synchronized (ranges) {
            for (int i = 0; i < ranges.size(); i+=2) {
                if (e < ranges.get(i)) {
                    return false;
                } if (e <= ranges.get(i+1)) {
                    return true;
                }
            }
        }
        return false;
    }

    public long endOfFirstRange() {
        synchronized (ranges) {
            if (ranges.isEmpty()) {
                return -1;
            }
            return ranges.get(1);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ranges.size(); i +=2) {
            sb.append('(')
                .append(ranges.get(i))
                .append(',')
                .append(ranges.get(i+1))
                .append(')');
        }
        return sb.toString();
    }
    
}
