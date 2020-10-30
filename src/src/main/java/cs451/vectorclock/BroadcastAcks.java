package cs451.vectorclock;

import java.util.Map;
import java.util.TreeMap;

import cs451.message.Message;
import cs451.message.MessageRange;

public class BroadcastAcks {

    private final Map<Integer, Map<Integer, MessageRange>> ackClock = new TreeMap<>();
    private final int numHosts;

    public BroadcastAcks(int numHosts, int myId) {
        this.numHosts = numHosts;
        for (int i = 1; i <= numHosts; ++i) {
            Map<Integer, MessageRange> hostMap = new TreeMap<>();
            for (int j = 1; j <= numHosts; ++j) {
                MessageRange mr = new MessageRange();
                if (i == myId && j == myId) {
                    mr.setRange(1, Long.MAX_VALUE);
                }
                hostMap.put(j, mr);
            }
            ackClock.put(i, hostMap);
        }
    }

    public int addAndReturnCount(Message m) {
        ackClock.get(m.getLastHop()).get(m.getOriginId()).add(m.getMessageId());
        int count = 0;
        for (int i = 1; i <= numHosts; i++) {
            if (ackClock.get(i).get(m.getOriginId()).contains(m.getMessageId())) {
                count += 1;
            }
        }
        return count;
    }
}
