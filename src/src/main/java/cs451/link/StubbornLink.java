package cs451.link;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

class StubbornLink extends AbstractLink {

    public static final int WINDOW_SIZE = 1 << 16;
    private final FairLossLink fLink;

    public StubbornLink(int port, List<Host> hosts, BListener listener, int myId) {
        super(listener, myId);
        fLink = new FairLossLink(port, hosts, this::deliver, myId);
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
        if (host.canSendWaitingMessages()) {
            Message m = host.getNextWaitingMessage();
            if (m != null) {
                long seqNumber = host.getNextSeqNumber();
                Message seqMessage = m.changeSeqNumber(seqNumber);
                fLink.send(seqMessage, hostId);
                WaitingPacket wpa = new WaitingPacket(seqMessage.resetSignalBroadcast(), host);
                try {
                    host.addPacketToConfirm(wpa);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
