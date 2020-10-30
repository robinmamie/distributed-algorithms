package cs451.link;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

class StubbornLink extends AbstractLink {

    private final FairLossLink fLink;
    private final IntConsumer broadcastListener;

    public StubbornLink(int port, List<Host> hosts, BListener listener, int myId, IntConsumer broadcastListener) {
        super(listener, myId);
        this.fLink = new FairLossLink(port, hosts, this::deliver, myId);
        this.broadcastListener = broadcastListener;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(this::stubbornSend);
    }

    @Override
    public void send(Message message, int hostId) {
        HostInfo host = fLink.getHostInfo(hostId);
        try {
            host.addMessageInWaitingList(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void deliver(Message m) {
        int hostId = m.getLastHop();
        HostInfo host = fLink.getHostInfo(hostId);
        host.resetTimeout();

        long mSeqNumber = m.getSeqNumber();
        boolean alreadyHandled;
        if (m.isAck()) {
            alreadyHandled = host.hasAckedPacket(mSeqNumber);
            host.updateReceiveVectorClock(mSeqNumber);
        } else {
            fLink.send(m.toAck(getMyId()), hostId);
            alreadyHandled = host.hasReceivedMessage(mSeqNumber);
            host.updateLocalReceiveVectorClock(mSeqNumber);
        }
        handleListener(m.setFlagAlreadyHandled(alreadyHandled));
    }

    private void stubbornSend() {
        while (true) {
            fLink.getHostInfo().forEach(this::checkNextPacketToConfirm);
            fLink.getHostInfo().forEach(this::emptyWaitingQueue);
        }
    }

    private void checkNextPacketToConfirm(int hostId, HostInfo host) {
        final WaitingPacket wp = host.getNextStubborn();
        if (wp != null && !host.hasAckedPacket(wp.getMessage().getSeqNumber())) {
            try {
                WaitingPacket newWp = wp.resendIfTimedOut(() -> {
                    fLink.send(wp.getMessage(), hostId);
                });
                host.addPacketToConfirm(newWp);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void emptyWaitingQueue(int hostId, HostInfo host) {
        while (host.canSendWaitingMessages()) {
            Message m = host.getNextWaitingMessage();
            if (m != null) {
                m.changeLastHop(getMyId());
                long seqNumber = host.getNextSeqNumber();
                Message seqMessage = m.changeSeqNumber(seqNumber);
                fLink.send(seqMessage, hostId);
                WaitingPacket wpa = new WaitingPacket(seqMessage, host);
                try {
                    host.addPacketToConfirm(wpa);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (m.getOriginId() == getMyId()) {
                    int min = Integer.MAX_VALUE;
                    for (HostInfo h: fLink.getHostInfo().values()) {
                        int candidate = h.peekNextToSend(getMyId());
                        if (candidate < min) min = candidate;
                    }
                    broadcastListener.accept(min-1);
                }
            }
        }
    }

    @Override
    public void sendRange(int hostId, int originId, int mId) {
        HostInfo hostInfo = fLink.getHostInfo(hostId);
        hostInfo.sendRange(originId, 1, mId);
    }
}
