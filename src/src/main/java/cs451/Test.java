package cs451;

import java.net.InetAddress;
import java.net.UnknownHostException;

import cs451.link.Link;
import cs451.message.Message;

public class Test {
    public static void main(String[] args) throws InterruptedException, UnknownHostException {
        final int nLinks = 2;
        Link[] links = new Link[nLinks];
        final long baseTime = System.currentTimeMillis();
        for (int i = 0; i < nLinks; ++i) {
            links[i] = Link.getLink(11000+i, 2, i+1);
            links[i].addListener((m, a, p) -> { 
                System.out.println((System.currentTimeMillis() - baseTime) + ": " + m);
            });
        }
        for (int i = 0; i < 100000; ++i) {
            links[0].send(Message.createMessage(1, i+1), InetAddress.getByName("127.0.0.1"), 11001);
            System.out.println((System.currentTimeMillis() - baseTime) + ": sent message " + i);
            links[1].send(Message.createMessage(2, i+1), InetAddress.getByName("127.0.0.1"), 11000);
        }

        Thread.sleep(60*60*1000);
    }
}
