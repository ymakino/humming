package humming.dropper;

import echowand.net.Frame;
import echowand.net.Node;
import echowand.net.NodeInfo;
import echowand.net.Subnet;
import echowand.net.SubnetException;
import echowand.service.ExtendedSubnet;
import echowand.util.Pair;
import echowand.util.Selector;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class FrameDropSubnet implements ExtendedSubnet {
    private static final Logger LOGGER = Logger.getLogger(FrameDropSubnet.class.getName());
    private static final String CLASS_NAME = FrameDropSubnet.class.getName();
    
    private Subnet internalSubnet;
    private FrameDropper defaultDropper;
    private LinkedList<Pair<Selector<? super Frame>, FrameDropper>> sendDroppers;
    private LinkedList<Pair<Selector<? super Frame>, FrameDropper>> receiveDroppers;
    
    private long totalSendCount = 0;
    private long dropSendCount = 0;
    private long totalReceiveCount = 0;
    private long dropReceiveCount = 0;
    
    public FrameDropSubnet(Subnet subnet, FrameDropper dropper) {
        LOGGER.entering(CLASS_NAME, "FrameDropSubnet", new Object[]{subnet, dropper});
        
        this.internalSubnet = subnet;
        defaultDropper = dropper;
        sendDroppers = new LinkedList<Pair<Selector<? super Frame>, FrameDropper>>();
        receiveDroppers = new LinkedList<Pair<Selector<? super Frame>, FrameDropper>>();
        
        LOGGER.exiting(CLASS_NAME, "FrameDropSubnet");
    }
    
    public FrameDropSubnet(Subnet subnet) {
        LOGGER.entering(CLASS_NAME, "FrameDropSubnet", subnet);
        
        this.internalSubnet = subnet;
        defaultDropper = new RandomFrameDropper(0);
        sendDroppers = new LinkedList<Pair<Selector<? super Frame>, FrameDropper>>();
        receiveDroppers = new LinkedList<Pair<Selector<? super Frame>, FrameDropper>>();
        
        LOGGER.exiting(CLASS_NAME, "FrameDropSubnet");
    }
    
    @Override
    public <S extends Subnet> S getSubnet(Class<S> cls) {
        LOGGER.entering(CLASS_NAME, "getSubnet", cls);
        
        S subnet;
        
        if (cls.isInstance(this)) {
            subnet = cls.cast(this);
        } else if (cls.isInstance(getInternalSubnet())) {
            subnet = cls.cast(getInternalSubnet());
        } else if (getInternalSubnet() instanceof ExtendedSubnet) {
            subnet = ((ExtendedSubnet)getInternalSubnet()).getSubnet(cls);
        } else {
            subnet = null;
        }
        
        LOGGER.exiting(CLASS_NAME, "getSubnet", subnet);
        return subnet;
    }
    
    @Override
    public Subnet getInternalSubnet() {
        LOGGER.entering(CLASS_NAME, "getInternalSubnet");
        
        Subnet result = internalSubnet;
        
        LOGGER.exiting(CLASS_NAME, "getInternalSubnet", result);
        return result;
    }
    
    public synchronized void setDefaultDropper(FrameDropper dropper) {
        LOGGER.entering(CLASS_NAME, "setDefaultDropper", dropper);
        
        this.defaultDropper = dropper;
        
        LOGGER.exiting(CLASS_NAME, "setDefaultDropper");
    }
    
    public synchronized FrameDropper getDefaultDropper() {
        LOGGER.entering(CLASS_NAME, "getDefaultDropper");
        
        FrameDropper result = defaultDropper;
        
        LOGGER.exiting(CLASS_NAME, "getDefaultDropper", result);
        return result;
    }
    
    public synchronized int countSendDroppers() {
        LOGGER.entering(CLASS_NAME, "countSendDroppers");
        
        int result = sendDroppers.size();
        
        LOGGER.exiting(CLASS_NAME, "countSendDroppers", result);
        return result;
    }
    
    public synchronized int countReceiveDroppers() {
        LOGGER.entering(CLASS_NAME, "countReceiveDroppers");
        
        int result = receiveDroppers.size();
        
        LOGGER.exiting(CLASS_NAME, "countReceiveDroppers", result);
        return result;
    }
    
    public synchronized void addSendDropper(Selector<? super Frame> selector, FrameDropper dropper) {
        LOGGER.entering(CLASS_NAME, "addSendDropper", new Object[]{selector, dropper});
        
        sendDroppers.add(new Pair<Selector<? super Frame>, FrameDropper>(selector, dropper));
        
        LOGGER.exiting(CLASS_NAME, "addSendDropper");
    }
    
    public synchronized void addReceiveDropper(Selector<? super Frame> selector, FrameDropper dropper) {
        LOGGER.entering(CLASS_NAME, "addReceiveDropper", new Object[]{selector, dropper});
        
        receiveDroppers.add(new Pair<Selector<? super Frame>, FrameDropper>(selector, dropper));
        
        LOGGER.exiting(CLASS_NAME, "addReceiveDropper");
    }
    
    public synchronized Pair<Selector<? super Frame>, FrameDropper> getSendDropper(int index) {
        LOGGER.entering(CLASS_NAME, "getSendDropper", index);
        
        Pair<Selector<? super Frame>, FrameDropper> result = sendDroppers.get(index);
        
        LOGGER.exiting(CLASS_NAME, "getSendDropper", result);
        return result;
    }
    
    public synchronized Pair<Selector<? super Frame>, FrameDropper> getReceiveDropper(int index) {
        LOGGER.entering(CLASS_NAME, "getReceiveDropper", index);
        
        Pair<Selector<? super Frame>, FrameDropper> result = receiveDroppers.get(index);
        
        LOGGER.exiting(CLASS_NAME, "getReceiveDropper", result);
        
        return result;
    }
    
    public synchronized FrameDropper getSendDropper(Frame frame) {
        LOGGER.entering(CLASS_NAME, "getSendDropper", frame);
        
        FrameDropper result = defaultDropper;
        
        for (Pair<Selector<? super Frame>, FrameDropper> p : sendDroppers) {
            if (p.first.match(frame)) {
                result = p.second;
                break;
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "getSendDropper", result);
        return result;
    }
    
    public synchronized FrameDropper getReceiveDropper(Frame frame) {
        LOGGER.entering(CLASS_NAME, "getReceiveDropper", frame);
        
        FrameDropper result = defaultDropper;
        
        for (Pair<Selector<? super Frame>, FrameDropper> p : receiveDroppers) {
            if (p.first.match(frame)) {
                result = p.second;
                break;
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "getReceiveDropper", result);
        return result;
    }
    
    public synchronized Pair<Selector<? super Frame>, FrameDropper> removeSendDropper(int index) {
        LOGGER.entering(CLASS_NAME, "removeSendDropper", index);
        
        Pair<Selector<? super Frame>, FrameDropper> result = sendDroppers.remove(index);
        
        LOGGER.exiting(CLASS_NAME, "removeSendDropper", result);
        return result;
    }
    
    public synchronized Pair<Selector<? super Frame>, FrameDropper> removeReceiveDropper(int index) {
        LOGGER.entering(CLASS_NAME, "removeReceiveDropper", index);
        
        Pair<Selector<? super Frame>, FrameDropper> result = receiveDroppers.remove(index);
        
        LOGGER.exiting(CLASS_NAME, "removeReceiveDropper", result);
        return result;
    }

    @Override
    public boolean send(Frame frame) throws SubnetException {
        LOGGER.entering(CLASS_NAME, "send", frame);
        
        if (dropSendCount > 0) {
            String msg = String.format("%.5f", dropSendCount / (double)totalSendCount);
            LOGGER.logp(Level.INFO, CLASS_NAME, "send", "send drop rate: " + msg);
        }
        
        totalSendCount++;
        
        boolean result;
        
        if (getSendDropper(frame).shouldDropSend(frame)) {
            dropSendCount++;
            LOGGER.logp(Level.INFO, CLASS_NAME, "send", "drop frame: " + frame);
            result = true;
        } else {
            result = internalSubnet.send(frame);
        }
        
        LOGGER.exiting(CLASS_NAME, "send", result);
        return result;
    }

    @Override
    public Frame receive() throws SubnetException {
        LOGGER.entering(CLASS_NAME, "receive");
        
        if (dropReceiveCount > 0) {
            String msg = String.format("%.5f", dropReceiveCount / (double)totalReceiveCount);
            LOGGER.logp(Level.INFO, CLASS_NAME, "receive", "receive drop rate: " + msg);
        }
        
        for (;;) {
            Frame frame = internalSubnet.receive();
            totalReceiveCount++;
            
            if (getReceiveDropper(frame).shouldDropReceive(frame)) {
                dropReceiveCount++;
                LOGGER.logp(Level.INFO, CLASS_NAME, "receive", "drop frame: " + frame);
            } else {
                LOGGER.exiting(CLASS_NAME, "receive", frame);
                return frame;
            }
        }
    }

    @Override
    public Node getLocalNode() {
        LOGGER.entering(CLASS_NAME, "getLocalNode");
        
        Node result = internalSubnet.getLocalNode();
        
        LOGGER.exiting(CLASS_NAME, "getLocalNode", result);
        return result;
    }

    @Override
    public Node getRemoteNode(String name) throws SubnetException {
        LOGGER.entering(CLASS_NAME, "getRemoteNode", name);
        
        Node result = internalSubnet.getRemoteNode(name);
        
        LOGGER.exiting(CLASS_NAME, "getRemoteNode", result);
        return result;
    }

    @Override
    public Node getRemoteNode(NodeInfo nodeInfo) throws SubnetException {
        LOGGER.entering(CLASS_NAME, "getRemoteNode", nodeInfo);
        
        Node result = internalSubnet.getRemoteNode(nodeInfo);
        
        LOGGER.exiting(CLASS_NAME, "getRemoteNode", result);
        return result;
    }

    @Override
    public Node getGroupNode() {
        LOGGER.entering(CLASS_NAME, "getGroupNode");
        
        Node result = internalSubnet.getGroupNode();
        
        LOGGER.exiting(CLASS_NAME, "getGroupNode", result);
        return result;
    }
    
}
