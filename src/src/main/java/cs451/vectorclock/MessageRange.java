package cs451.vectorclock;

/**
 * An abstraction serving the purpose of compressing message IDs, which allows
 * for a good memory-scalability.
 *
 * It creates a list of sub-ranges in ascending order.
 */
public class MessageRange {

    /**
     * A token value used when the range is empty.
     */
    public static final int EMPTY = Integer.MIN_VALUE;

    /**
     * The "linked list" of sub-ranges.
     */
    private Range ranges = null;

    /**
     * Lock used for the whole abstraction. Cannot directly use "ranges", because it
     * can change/be deleted.
     */
    private final Object lock = new Object();

    /**
     * Set a range as the initial value (used to broadcast/save the local messages).
     *
     * @param a The lower bound of the range.
     * @param b The upper bound of the range.
     */
    public void setRange(int a, int b) {
        synchronized (lock) {
            ranges = new Range(a, b);
        }
    }

    /**
     * Add a range to the MessageRange. Merges and overrides ranges to keep the
     * contents logical.
     *
     * @param a The lower bound of the new range.
     * @param b The upper bound of the new range.
     */
    public void addRange(int a, int b) {
        synchronized (lock) {
            Range previous = null;
            Range current = ranges;
            int lowerBound = EMPTY;

            while (current != null) {
                if (lowerBound == EMPTY) {
                    if (a < current.getStart()) {
                        lowerBound = a;
                    } else if (current.contains(a) || a == current.getEnd() + 1) {
                        lowerBound = current.getStart();
                    }
                }

                if (lowerBound != EMPTY) {
                    Range newRange = null;
                    if (current.contains(b) || current.getStart() - 1 == b) {
                        newRange = new Range(lowerBound, current.getEnd(), current.next());
                    } else if (b < current.getStart()) {
                        newRange = new Range(lowerBound, b, current);
                    }
                    if (newRange != null) {
                        if (previous != null) {
                            previous.setNext(newRange);
                        } else {
                            ranges = newRange;
                        }
                        return;
                    }
                }

                if (lowerBound == EMPTY) {
                    previous = current;
                }
                current = current.next();
            }

            a = lowerBound == EMPTY ? a : lowerBound;
            if (previous == null) {
                ranges = new Range(a, b);
            } else {
                previous.setNext(new Range(a, b));
            }
        }
    }

    /**
     * Add an element to the range and return whether the element was absent or not.
     *
     * @param e The new element to add
     * @return True if the element was absent, false otherwise.
     */
    public boolean add(int e) {
        synchronized (lock) {
            Range previous = null;
            Range current = ranges;

            while (current != null) {
                if (current.canBeExtendedBy(e)) {
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
        }
        return true;
    }

    /**
     * Take and remove the first element of the range.
     *
     * @return The first element of the range.
     */
    public int poll() {
        int firstElement;
        synchronized (lock) {
            if (ranges == null) {
                return EMPTY;
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

    /**
     * Check whether a given element is in the range.
     *
     * @param e The element to check.
     * @return Whether the given element is present in the range or not.
     */
    public boolean contains(int e) {
        synchronized (lock) {
            Range current = ranges;
            while (current != null) {
                if (e < current.getStart()) {
                    return false;
                }
                if (e <= current.getEnd()) {
                    return true;
                }
                current = current.next();
            }
        }
        return false;
    }

    /**
     * Get the last element of the first sub-range (used for the VectorClock
     * implementation).
     *
     * @return The last element of the first sub-range.
     */
    public int endOfFirstRange() {
        synchronized (lock) {
            if (ranges == null) {
                return EMPTY;
            }
            return ranges.getEnd();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Range current = ranges;
        while (current != null) {
            sb.append('(').append(current.getStart()).append(',').append(current.getEnd()).append(')');
            current = current.next();
        }
        return sb.toString();
    }

    /**
     * Sub-abstraction defining a simple range. The overall abstraction combines
     * these sub-ranges to create a collection of ranges. Implements a linked list.
     */
    private static class Range {
        private int start;
        private int end;
        private Range next;

        /**
         * Create a new sub-range.
         *
         * @param start The start value of the sub-range (inclusive).
         * @param end   The end value of the sub-range (inclusive).
         * @param next  The sub-range following, or null if none.
         */
        public Range(int start, int end, Range next) {
            this.start = start;
            this.end = end;
            this.next = next;
        }

        /**
         * Create a new sub-range, with no following sub-range.
         *
         * @param start The start value of the sub-range (inclusive).
         * @param end   The end value of the sub-range (inclusive).
         */
        public Range(int start, int end) {
            this(start, end, null);
        }

        /**
         * Check whether the sub-range is not directly after a given value, which would
         * mean the sub-range could be extended.
         *
         * @param e The value to check.
         * @return Whether the sub-range is not directly after the given value.
         */
        public boolean isWellAfter(int e) {
            return e < start - 1;
        }

        /**
         * Check whether the sub-range contains the value.
         *
         * @param e The value to check.
         * @return Whether the sub-range contains the value.
         */
        public boolean contains(int e) {
            return start <= e && e <= end;
        }

        /**
         * Merge two sub-ranges if it is possible.
         *
         * @param previous The previous sub-range (in the list).
         */
        private void mergeIfPossible(Range previous) {
            if (previous != null && previous.end + 1 == start) {
                previous.end = end;
                previous.next = next;
            } else if (next != null && end + 1 == next.start) {
                end = next.end;
                next = next.next;
            }
        }

        /**
         * Check wether the sub-range can be extended by a given value, and extend it.
         *
         * @param e The value to check.
         * @return Whether the sub-range was successfully extended or not.
         */
        public boolean canBeExtendedBy(int e) {
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

        /**
         * Get the next sub-range.
         *
         * @return The next sub-range.
         */
        public Range next() {
            return next;
        }

        /**
         * Set the next sub-range to the given value.
         *
         * @param next The sub-range to set as next in the list.
         */
        public void setNext(Range next) {
            this.next = next;
        }

        /**
         * Get the start value of the sub-range.
         *
         * @return The start value of the sub-range.
         */
        public int getStart() {
            return start;
        }

        /**
         * Get the end value of the sub-range.
         *
         * @return The end value of the sub-range.
         */
        public int getEnd() {
            return end;
        }

        /**
         * Increment the start value.
         */
        public void incrementStart() {
            start += 1;
        }
    }
}
