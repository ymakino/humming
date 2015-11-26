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
        this.internalSubnet = subnet;
        defaultDropper = dropper;
        sendDroppers = new LinkedList<Pair<Selector<? super Frame>, FrameDropper>>();
        receiveDroppers = new LinkedList<Pair<Selector<? super Frame>, FrameDropper>>();
    }
    
    public FrameDropSubnet(Subnet subnet) {
        this.internalSubnet = subnet;
        defaultDropper = new RandomFrameDropper(0);
        sendDroppers = new LinkedList<Pair<Selector<? super Frame>, FrameDropper>>();
        receiveDroppers = new LinkedList<Pair<Selector<? super Frame>, FrameDropper>>();
    }
    
    @Override
    public <S extends Subnet> S getSubnet(Class<S> cls) {
        if (cls.isInstance(this)) {
            return cls.cast(this);
        } else if (cls.isInstance(getInternalSubnet())) {
            return cls.cast(getInternalSubnet());
        } else if (getInternalSubnet() instanceof ExtendedSubnet) {
            return ((ExtendedSubnet)getInternalSubnet()).getSubnet(cls);
        } else {
            return null;
        }
    }
    
    @Override
    public Subnet getInternalSubnet() {
        return internalSubnet;
    }
    
    public synchronized void setDefaultDropper(FrameDropper dropper) {
        this.defaultDropper = dropper;
    }
    
    public synchronized FrameDropper getDefaultDropper() {
        return defaultDropper;
    }
    
    public synchronized int countSendDroppers() {
        return sendDroppers.size();
    }
    
    public synchronized int countReceiveDroppers() {
        return receiveDroppers.size();
    }
    
    public synchronized void addSendDropper(Selector<? super Frame> selector, FrameDropper dropper) {
        sendDroppers.add(new Pair<Selector<? super Frame>, FrameDropper>(selector, dropper));
    }
    
    public synchronized void addReceiveDropper(Selector<? super Frame> selector, FrameDropper dropper) {
        receiveDroppers.add(new Pair<Selector<? super Frame>, FrameDropper>(selector, dropper));
    }
    
    public synchronized Pair<Selector<? super Frame>, FrameDropper> getSendDropper(int index) {
        return sendDroppers.get(index);
    }
    
    public synchronized Pair<Selector<? super Frame>, FrameDropper> getReceiveDropper(int index) {
        return receiveDroppers.get(index);
    }
    
    public synchronized FrameDropper getSendDropper(Frame frame) {
        for (Pair<Selector<? super Frame>, FrameDropper> p : sendDroppers) {
            if (p.first.match(frame)) {
                return p.second;
            }
        }
        
        return defaultDropper;
    }
    
    public synchronized FrameDropper getReceiveDropper(Frame frame) {
        for (Pair<Selector<? super Frame>, FrameDropper> p : receiveDroppers) {
            if (p.first.match(frame)) {
                return p.second;
            }
        }
        
        return defaultDropper;
    }
    
    public synchronized Pair<Selector<? super Frame>, FrameDropper> removeSendDropper(int index) {
        return sendDroppers.remove(index);
    }
    
    public synchronized Pair<Selector<? super Frame>, FrameDropper> removeReceiveDropper(int index) {
        return receiveDroppers.remove(index);
    }

    @Override
    public boolean send(Frame frame) throws SubnetException {
        String msg = String.format("%.5f", dropSendCount / (double)totalSendCount);
        LOGGER.logp(Level.INFO, CLASS_NAME, "send", "send drop rate: " + msg);
        
        totalSendCount++;
        
        if (getSendDropper(frame).shouldDropSend(frame)) {
            dropSendCount++;
            return true;
        } else {
            return internalSubnet.send(frame);
        }
    }

    @Override
    public Frame receive() throws SubnetException {
        String msg = String.format("%.5f", dropReceiveCount / (double)totalReceiveCount);
        LOGGER.logp(Level.INFO, CLASS_NAME, "receive", "receive drop rate: " + msg);
        
        for (;;) {
            Frame frame = internalSubnet.receive();
            totalReceiveCount++;
            
            if (getReceiveDropper(frame).shouldDropReceive(frame)) {
                dropReceiveCount++;
            } else {
                return frame;
            }
        }
    }

    @Override
    public Node getLocalNode() {
        return internalSubnet.getLocalNode();
    }

    @Override
    public Node getRemoteNode(String name) throws SubnetException {
        return internalSubnet.getRemoteNode(name);
    }

    @Override
    public Node getRemoteNode(NodeInfo nodeInfo) throws SubnetException {
        return internalSubnet.getRemoteNode(nodeInfo);
    }

    @Override
    public Node getGroupNode() {
        return internalSubnet.getGroupNode();
    }
    
}
