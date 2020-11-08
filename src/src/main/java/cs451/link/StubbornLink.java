package cs451.link;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cs451.listener.BListener;
import cs451.message.Message;
import cs451.parser.Host;

class StubbornLink extends AbstractLink {

    private final FairLossLink fLink;

    public StubbornLink(int port, List<Host> hosts, BListener listener, int myId) {
        super(listener, myId, hosts);
        this.fLink = new FairLossLink(port, hosts, this::deliver, myId);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(this::stubbornSend);
    }

    @Override
    public void send(Message message, int hostId) {
        HostInfo host = getHostInfo(hostId);
        host.addMessageInWaitingList(message);
    }

    private void deliver(Message m) {
        int hostId = m.getLastHop();
        HostInfo host = getHostInfo(hostId);
        host.resetTimeout();

        if (!m.isAck()) {
            fLink.send(m.toAck(getMyId()), hostId);
        }
        handleListener(m);
    }

    private void stubbornSend() {
        while (true) {
            getHostInfo().forEach(this::checkNextPacketToConfirm);
        }
    }

    private void checkNextPacketToConfirm(int hostId, HostInfo host) {
        final WaitingPacket wp = host.getNextStubborn();
        if (wp != null && !host.isDelivered(wp.getMessage())) {
            try {
                WaitingPacket newWp = wp.resendIfTimedOut(() -> {
                    fLink.send(wp.getMessage(), hostId);
                });
                host.addPacketToConfirm(newWp);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        emptyWaitingQueue(hostId, host);
    }

    private void emptyWaitingQueue(int hostId, HostInfo host) {
        while (host.canSendWaitingMessages()) {
            Message m = host.getNextWaitingMessage();
            if (m == null) {
                return;
            }

            fLink.send(m, hostId);
            WaitingPacket wpa = new WaitingPacket(m, host);
            try {
                host.addPacketToConfirm(wpa);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void sendRange(int hostId, int originId, int mId) {
        HostInfo hostInfo = getHostInfo(hostId);
        hostInfo.sendRange(originId, 1, mId);
    }
}
