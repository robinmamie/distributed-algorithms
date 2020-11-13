package cs451.link;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cs451.listener.BListener;
import cs451.listener.PListener;
import cs451.message.Message;
import cs451.message.Packet;
import cs451.parser.Host;

/**
 * Defines a common architecture for all Links, i.e. each concrete link should
 * store a listener each and handle it once a message is delivered.
 *
 * All the links also share common host information, as can be seen in the class
 * HostInfo.
 */
public abstract class AbstractLink implements Link {

    /**
     * Common information for each distant host, used for networking purposes.
     */
    private static final Map<Integer, HostInfo> hostInfo = new TreeMap<>();

    /**
     * The listener to be called once a message is delivered.
     */
    private final BListener bListener;
    
    /**
    * The listener to be called once a packet is delivered.
    */
   private final PListener pListener;

    /**
     * The ID of the local host.
     */
    private final int myId;

    /**
     * The constructor of the AbstractLink, creating empty host information if it is
     * called for the first time.
     *
     * @param listener The listener to call once a message is delivered.
     * @param myId     The ID of the local host.
     * @param hosts    The complete list of hosts of the network.
     */
    protected AbstractLink(BListener listener, int myId, List<Host> hosts) {
        this.bListener = listener;
        this.pListener = null;
        this.myId = myId;

        // Only create host information once.
        if (hostInfo.isEmpty()) {
            for (Host host : hosts) {
                int i = host.getId();
                if (i != getMyId()) {
                    HostInfo hostI;
                    try {
                        hostI = new HostInfo(InetAddress.getByName(host.getIp()), host.getPort(), hosts.size());
                    } catch (UnknownHostException e) {
                        throw new RuntimeException("Invalid IP address given!");
                    }
                    hostInfo.put(i, hostI);
                }
            }
        }
    }

    /**
     * The constructor of the AbstractLink, creating empty host information if it is
     * called for the first time.
     *
     * @param listener The listener to call once a packet is delivered.
     * @param myId     The ID of the local host.
     * @param hosts    The complete list of hosts of the network.
     */
    protected AbstractLink(PListener listener, int myId, List<Host> hosts) {
        this.bListener = null;
        this.pListener = listener;
        this.myId = myId;

        // Only create host information once.
        if (hostInfo.isEmpty()) {
            for (Host host : hosts) {
                int i = host.getId();
                if (i != getMyId()) {
                    HostInfo hostI;
                    try {
                        hostI = new HostInfo(InetAddress.getByName(host.getIp()), host.getPort(), hosts.size());
                    } catch (UnknownHostException e) {
                        throw new RuntimeException("Invalid IP address given!");
                    }
                    hostInfo.put(i, hostI);
                }
            }
        }
    }

    /**
     * Deliver the given message.
     *
     * @param m The message to deliver.
     */
    protected void handleListener(Message message) {
        bListener.apply(message);
    }

    /**
     * Deliver the given packet.
     *
     * @param m The message to deliver.
     */
    protected void handleListener(Packet packet) {
        pListener.apply(packet);
    }

    /**
     * Get the ID of the local host.
     *
     * @return The ID of the local host.
     */
    protected int getMyId() {
        return myId;
    }

    /**
     * Statically get a particular host info saved by this network.
     *
     * @param hostId The particular ID of the necessary information.
     * @return The particular instance of host information of this network.
     */
    public static HostInfo getHostInfo(int hostId) {
        return hostInfo.get(hostId);
    }

    /**
     * Statically get all host info saved by this network.
     *
     * @return All instances of host information of this network.
     */
    public static Map<Integer, HostInfo> getHostInfo() {
        return hostInfo;
    }
}
