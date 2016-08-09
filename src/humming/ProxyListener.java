package humming;

import echowand.common.EOJ;
import echowand.common.ESV;
import echowand.logic.Listener;
import echowand.logic.SetGetTransactionConfig;
import echowand.logic.Transaction;
import echowand.logic.TransactionListener;
import echowand.net.CommonFrame;
import echowand.net.Frame;
import echowand.net.Node;
import echowand.net.Property;
import echowand.net.StandardPayload;
import echowand.net.Subnet;
import echowand.net.SubnetException;
import echowand.service.Core;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ProxyListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger(ProxyListener.class.getName());
    private static final String CLASS_NAME = ProxyListener.class.getName();
    
    private static final int TIMEOUT = 10000;
    
    private HashMap<Subnet, Core> coreMap;
    private HashMap<ObjectInfo, ObjectInfo> proxyMap;
    
    private class ObjectInfo {
        public final Core core;
        public final Node node;
        public final EOJ eoj;
        
        public ObjectInfo(Core core, Node node, EOJ eoj) {
            this.core = core;
            this.node = node;
            this.eoj = eoj;
        }
        
        private boolean equals(Object o1, Object o2) {
            if (o1 == null) {
                if (o2 != null) {
                    return false;
                }
            } else if (!o1.equals(o2)) {
                return false;
            }
            
            return true;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            
            if (!(o instanceof ObjectInfo)) {
                return false;
            }
            
            ObjectInfo other = (ObjectInfo)o;
            
            return equals(core, other.core) && equals(node, other.node) && equals(eoj, other.eoj);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + Objects.hashCode(this.core);
            hash = 59 * hash + Objects.hashCode(this.node);
            hash = 59 * hash + Objects.hashCode(this.eoj);
            return hash;
        }
    }
    
    private class ProxyTransactionListener implements TransactionListener {
        private Core proxyCore;
        private Frame requestFrame;
        
        public ProxyTransactionListener(Core proxyCore, Frame requestFrame) {
            this.proxyCore = proxyCore;
            this.requestFrame = requestFrame;
        }

        @Override
        public void begin(Transaction t) {
        }

        @Override
        public void send(Transaction t, Subnet subnet, Frame frame, boolean success) {
        }
        
        private StandardPayload createResponsePayload(StandardPayload payload) {
            StandardPayload requestPayload = requestFrame.getCommonFrame().getEDATA(StandardPayload.class);
            
            StandardPayload newPayload = new StandardPayload();
            newPayload.setESV(payload.getESV());
            newPayload.setDEOJ(requestPayload.getSEOJ());
            newPayload.setSEOJ(requestPayload.getDEOJ());
            
            for (int i=0; i<payload.getFirstOPC(); i++) {
                newPayload.addFirstProperty(payload.getFirstPropertyAt(i));
            }
            
            for (int i=0; i<payload.getSecondOPC(); i++) {
                newPayload.addSecondProperty(payload.getSecondPropertyAt(i));
            }
            
            return newPayload;
        }

        @Override
        public void receive(Transaction t, Subnet subnet, Frame frame) {
            StandardPayload proxyPayload = frame.getCommonFrame().getEDATA(StandardPayload.class);
            
            if (proxyPayload == null) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyTransactionListener.receive", "invalid frame: " + frame);
            }
            
            CommonFrame commonFrame = new CommonFrame();
            commonFrame.setEDATA(createResponsePayload(proxyPayload));
            commonFrame.setTID(requestFrame.getCommonFrame().getTID());
            
            Frame newFrame = new Frame(subnet.getLocalNode(), requestFrame.getSender(), commonFrame, requestFrame.getConnection());
            
            try {
                boolean result = proxyCore.getSubnet().send(newFrame);
                
                if (result == false) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyTransactionListener.receive", "cannot send frame: " + newFrame);
                }
            } catch (SubnetException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyTransactionListener.receive", "cannot send frame: " + newFrame, ex);
            }
            
        }

        @Override
        public void finish(Transaction t) {
        }
        
    }
    
    public ProxyListener() {
        coreMap = new HashMap<Subnet, Core>();
        proxyMap = new HashMap<ObjectInfo, ObjectInfo>();
    }
    
    public synchronized void registerProxyObject(Core proxyCore, EOJ proxyEOJ, Core remoteCore, Node remoteNode, EOJ remoteEOJ) {
        ObjectInfo proxyObjectInfo = new ObjectInfo(proxyCore, proxyCore.getSubnet().getLocalNode(), proxyEOJ);
        ObjectInfo removeObjectInfo = new ObjectInfo(remoteCore, remoteNode, remoteEOJ);
        
        coreMap.put(proxyCore.getSubnet(), proxyCore);
        coreMap.put(remoteCore.getSubnet(), remoteCore);
        
        proxyMap.put(proxyObjectInfo, removeObjectInfo);
    }
    
    private synchronized Core getCoreOfSubet(Subnet subnet) {
        return coreMap.get(subnet);
    }
    
    private synchronized ObjectInfo getRemoteInfo(ObjectInfo proxyInfo) {
        return proxyMap.get(proxyInfo);
    }
    
    private boolean isSetGetESV(ESV esv) {
        return esv == ESV.Get || esv == ESV.SetI || esv == ESV.SetC || esv == ESV.SetGet || esv == ESV.INF_REQ; 
    }
    
    private boolean isNotifyESV(ESV esv) {
        return esv == ESV.INF || esv == ESV.INFC; 
    }
    
    private boolean processSetGet(Subnet subnet, Frame frame, StandardPayload payload) {
        Core proxyCore = getCoreOfSubet(subnet);
        
        if (proxyCore == null) {
            LOGGER.exiting(CLASS_NAME, "processSetGet", false);
            return false;
        }
        
        ObjectInfo proxyInfo = new ObjectInfo(proxyCore, subnet.getLocalNode(), payload.getDEOJ());
        
        ObjectInfo remoteInfo = getRemoteInfo(proxyInfo);
        
        if (remoteInfo == null) {
            LOGGER.exiting(CLASS_NAME, "processSetGet", false);
            return false;
        }
        
        SetGetTransactionConfig transactionConfig = new SetGetTransactionConfig();
        int firstOPC = 0x000000ff & payload.getFirstOPC();
        int secondOPC = 0x000000ff & payload.getSecondOPC();
        
        transactionConfig.setSenderNode(remoteInfo.core.getSubnet().getLocalNode());
        transactionConfig.setReceiverNode(remoteInfo.node);
        transactionConfig.setSourceEOJ(proxyInfo.eoj);
        transactionConfig.setDestinationEOJ(remoteInfo.eoj);
        
        switch (payload.getESV()) {
            case INF_REQ:
                transactionConfig.setAnnouncePreferred(true);
            case Get:
                for (int i=0; i<firstOPC; i++) {
                    transactionConfig.addGet(payload.getFirstPropertyAt(i).getEPC());
                }
                break;
            case SetGet:
                for (int i=0; i<secondOPC; i++) {
                    transactionConfig.addGet(payload.getSecondPropertyAt(i).getEPC());
                }
            case SetI:
                transactionConfig.setResponseRequired(false);
            case SetC:
                for (int i=0; i<firstOPC; i++) {
                    Property property = payload.getFirstPropertyAt(i);
                    transactionConfig.addSet(property.getEPC(), property.getEDT());
                }
                break;
            default:
                LOGGER.exiting(CLASS_NAME, "processSetGet", false);
                return false;
        }
        
        Transaction transaction = new Transaction(remoteInfo.core.getSubnet(), remoteInfo.core.getTransactionManager(), transactionConfig);
        
        transaction.addTransactionListener(new ProxyTransactionListener(proxyCore, frame));
        
        transaction.setTimeout(TIMEOUT);
        
        try {
            transaction.execute();
            LOGGER.exiting(CLASS_NAME, "processSetGet", false);
            return true;
        } catch (SubnetException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "processSetGet", "cannot execute transaction: " + transaction, ex);
            LOGGER.exiting(CLASS_NAME, "processSetGet", false);
            return false;
        }
    }
    
    private boolean processNotify(Subnet subnet, Frame frame, StandardPayload payload) {
        LOGGER.exiting(CLASS_NAME, "processNotify", false);
        return false;
    }

    @Override
    public boolean process(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "process", new Object[]{subnet, frame, processed});
        
        if (processed) {
            LOGGER.exiting(CLASS_NAME, "process", false);
            return false;
        }
        
        StandardPayload payload = frame.getCommonFrame().getEDATA(StandardPayload.class);
        
        if (payload == null) {
            LOGGER.exiting(CLASS_NAME, "process", false);
            return false;
        }
        
        if (isSetGetESV(payload.getESV())) {
            boolean result = processSetGet(subnet, frame, payload);
            LOGGER.exiting(CLASS_NAME, "process", result);
            return result;
        }
        
        if (isNotifyESV(payload.getESV())) {
            boolean result = processNotify(subnet, frame, payload);
            LOGGER.exiting(CLASS_NAME, "process", result);
            return result;
        }
        
        LOGGER.exiting(CLASS_NAME, "process", false);
        return false;
    }
}
