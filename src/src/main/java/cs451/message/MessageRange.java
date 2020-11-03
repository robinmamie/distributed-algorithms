package cs451.message;

public class MessageRange {

    private final static int TOKEN_EMPTY = Integer.MIN_VALUE;

    private static class Range {
        private int start;
        private int end;
        private Range next;
        public Range(int start, int end, Range next) {
            this.start = start;
            this.end = end;
            this.next = next;
        }
        public Range(int start, int end) {
            this(start, end, null);
        }
        public boolean isWellAfter(int e) {
            return e < start-1;
        }
        public boolean contains(int e) {
            return start <= e && e <= end;
        }
        private void mergeIfPossible(Range previous) {
            if (previous != null && previous.end + 1 == start) {
                previous.end = end;
                previous.next = next;
            } else if (next != null && end + 1 == next.start) {
                end = next.end;
                next = next.next;
            }
        }
        public boolean canExtend(int e) {
            if (end + 1 == e) {
                end = e;
                return true;
            }
            if (start - 1 == e) {
                start = e;
                return true;
            }
            return false;
        }
        public Range next() {
            return next;
        }
        public void setNext(Range next) {
            this.next = next;
        }
        public int getStart() {
            return start;
        }
        public int getEnd() {
            return end;
        }
        public void incrementStart() {
            start += 1;
        }
    }

    private Range ranges = null;
    private final Object lock = new Object();

    public void setRange(int a, int b) {
        synchronized (lock) {
            ranges = new Range(a, b);
        }
    }

    /**
     * 
     * @param e
     * @return true if element was not yet present, false otherwise
     */
    public boolean add(int e) {
        Range previous = null;
        Range current = ranges;

        while (current != null) {
            if (current.canExtend(e)) {
                current.mergeIfPossible(previous);
                return true;
            }
            if (current.contains(e)) {
                return false;
            }
            if (current.isWellAfter(e)) {
                Range newRange = new Range(e, e, current);
                if (previous == null) {
                    ranges = newRange;
                } else {
                    previous.setNext(newRange);
                }
                return true;
            }
            previous = current;
            current = current.next();
        }

        if (previous == null) {
            ranges = new Range(e, e);
        } else {
            previous.setNext(new Range(e, e));
        }
        return true;
    }

    public boolean addSync(int e) {
        synchronized (lock) {
            return add(e);
        }
    }

    public int poll() {
        int firstElement;
        synchronized (lock) {
            if (ranges == null) {
                return TOKEN_EMPTY;
            }
            firstElement = ranges.getStart();
            if (firstElement == ranges.getEnd()) {
                ranges = ranges.next();
            } else {
                ranges.incrementStart();
            }
        }
        return firstElement;
    }

    public boolean contains(int e) {
        Range current = ranges;
        while (current != null) {
            if (e < current.getStart()) {
                return false;
            } if (e <= current.getEnd()) {
                return true;
            }
            current = current.next();
        }
        return false;
    }

    public int endOfFirstRange() {
        if (ranges == null) {
            return TOKEN_EMPTY;
        }
        return ranges.getEnd();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Range current = ranges;
        while (current != null) {
            sb.append('(')
                .append(current.getStart())
                .append(',')
                .append(current.getEnd())
                .append(')');
            current = current.next();
        }
        return sb.toString();
    }
    
}
