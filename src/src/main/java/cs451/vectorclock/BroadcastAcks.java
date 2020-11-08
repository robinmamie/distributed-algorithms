package cs451.vectorclock;

import java.util.Map;
import java.util.TreeMap;

import cs451.link.HostInfo;
import cs451.message.Message;

public class BroadcastAcks {

    private final Map<Integer, Map<Integer, MessageRange>> ackClock = new TreeMap<>();
    private final int numHosts;
    private final int myId;

    public BroadcastAcks(int numHosts, int myId, Map<Integer, HostInfo> hostInfo) {
        this.numHosts = numHosts;
        this.myId = myId;

        for (int i = 1; i <= numHosts; ++i) {
            Map<Integer, MessageRange> hostMap;
            if (i == myId) {
                hostMap = new TreeMap<>();
                for (int j = 1; j <= numHosts; ++j) {
                    MessageRange mr = new MessageRange();
                    if (j == myId) {
                        mr.setRange(1, Integer.MAX_VALUE);
                    }
                    hostMap.put(j, mr);
                }
            } else {
                hostMap = hostInfo.get(i).getDelivered();
            }
            ackClock.put(i, hostMap);
        }
    }

    public boolean wasAlreadyBroadcast(Message m) {
        return ackClock.get(myId).get(m.getOriginId()).contains(m.getMessageId());
    }

    public void add(Message m) {
        ackClock.get(myId).get(m.getOriginId()).add(m.getMessageId());
    }

    public int ackCount(Message m) {
        int count = 0;
        for (int i = 1; i <= numHosts; i++) {
            if (ackClock.get(i).get(m.getOriginId()).contains(m.getMessageId())) {
                count += 1;
            }
        }
        return count;
    }
}
