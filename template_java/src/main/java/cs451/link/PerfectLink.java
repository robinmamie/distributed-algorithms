package cs451.link;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cs451.Message;

class PerfectLink extends AbstractLink {

    private static class IntPair {
        private final int a;
        private final int b;
        public IntPair(int a, int b) {
            this.a = a;
            this.b = b;
        }
        @Override
        public boolean equals(Object that) {
            return that instanceof IntPair
                && this.a == ((IntPair)that).a
                && this.b == ((IntPair)that).b;
        }
        @Override
        public int hashCode() {
            return (a * 7) + (b * 13);
        }
    }

    private final StubbornLink sLink;
    // TODO low performance synchronization
    private final Set<IntPair> delivered = Collections.synchronizedSet(new HashSet<>());;

    public PerfectLink(int port) {
        this.sLink = new StubbornLink(port);
        sLink.addListener((m, a, p) -> {
            IntPair ip = new IntPair(m.getOriginId(), m.getMessageId());
            if (!delivered.contains(ip)) {
                delivered.add(ip);
                handleListeners(m, a, p);
            }
        });
    }

    @Override
    public boolean send(Message message, InetAddress address, int port) {
        return sLink.send(message, address, port);
    }

}
