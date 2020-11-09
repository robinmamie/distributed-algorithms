package cs451.vectorclock;

import java.util.Map;
import java.util.TreeMap;

import cs451.link.HostInfo;
import cs451.message.Message;

/**
 * Implements a map of all already BEB-delivered messages. It share information
 * with the Link layer, so that there is no duplication of information between
 * the layers.
 *
 * Disclaimer: the Link layer does not care about the order of the messages.
 */
public class BroadcastAcks {

    /**
     * The map of all already received messages, built as follows:
     *
     * lastHop -> originId -> messageId (in a MessageRange)
     */
    private final Map<Integer, Map<Integer, MessageRange>> ackClock = new TreeMap<>();

    /**
     * The ID of the local host.
     */
    private final int myId;

    /**
     * Create a new BroadcastAcks.
     *
     * @param numHosts The total number of hosts in the topology.
     * @param myId     The ID of the local host.
     * @param hostInfo The information about all hosts, shared with the Link layer.
     */
    public BroadcastAcks(int numHosts, int myId, Map<Integer, HostInfo> hostInfo) {
        this.myId = myId;

        // For each "lastHop"...
        for (int i = 1; i <= numHosts; ++i) {
            Map<Integer, MessageRange> hostMap;
            if (i == myId) {
                // If lastHop is local automatically "ack" locally created messages by default.
                hostMap = new TreeMap<>();
                for (int j = 1; j <= numHosts; ++j) {
                    MessageRange mr = new MessageRange();
                    if (j == myId) {
                        mr.setRange(1, Integer.MAX_VALUE);
                    }
                    hostMap.put(j, mr);
                }
            } else {
                // Otherwise, just share the information with the Link layer.
                hostMap = hostInfo.get(i).getDelivered();
            }
            ackClock.put(i, hostMap);
        }
    }

    /**
     * Check whether the message was already broadcast, i.e. it is already saved in
     * the local host's last hop.
     *
     * @param m The message to check.
     * @return Whether it was already broadcast or not.
     */
    public boolean wasAlreadyBroadcast(Message m) {
        return ackClock.get(myId).get(m.getOriginId()).contains(m.getMessageId());
    }

    /**
     * Add a given message to the local host's last hop table.
     *
     * @param m The message to add.
     */
    public void add(Message m) {
        ackClock.get(myId).get(m.getOriginId()).add(m.getMessageId());
    }

    /**
     * Count the number of hosts which have already BEB-delivered the message.
     *
     * @param m The message to check.
     * @return The number of hosts which have already BEB-delivered the message.
     */
    public int ackCount(Message m) {
        int count = 0;
        for (Map<Integer, MessageRange> ack : ackClock.values()) {
            if (ack.get(m.getOriginId()).contains(m.getMessageId())) {
                count += 1;
            }
        }
        return count;
    }
}
