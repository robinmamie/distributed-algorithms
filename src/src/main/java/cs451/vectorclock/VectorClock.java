package cs451.vectorclock;

/**
 * Simpler abstraction to the MessageRange.
 */
public class VectorClock {

    /**
     * The underlying MessageRange.
     */
    private final MessageRange range = new MessageRange();

    /**
     * Create a VectorClock starting at 0.
     */
    public VectorClock() {
        range.add(0);
    }

    /**
     * Add an element to the vector clock or the set of elements waiting to be added
     * to the VectorClock.
     *
     * @param e The new element to be added.
     * @return True if the element was absent, false otherwise.
     */
    public boolean addMember(int e) {
        return range.add(e);
    }

    /**
     * Check whether a given element is contained in the vector clock, or the set of
     * elements waiting to be added to the VectorClock.
     *
     * @param e The element to check.
     * @return Whether the given element is present in the range or not.
     */
    public boolean contains(int e) {
        return range.contains(e);
    }

    /**
     * Get the current value of the VectorClock.
     *
     * @return The current value of the VectorClock.
     */
    public int getStateOfVc() {
        return range.endOfFirstRange();
    }

    public MessageRange getRange() {
        return range;
    }
}
