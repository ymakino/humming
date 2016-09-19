package humming;

import echowand.common.ClassEOJ;
import echowand.common.Data;
import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.common.ESV;
import echowand.logic.RequestProcessor;
import echowand.logic.SetGetTransactionConfig;
import echowand.logic.Transaction;
import echowand.logic.TransactionConfig;
import echowand.logic.TransactionListener;
import echowand.net.CommonFrame;
import echowand.net.Frame;
import echowand.net.Node;
import echowand.net.Property;
import echowand.net.StandardPayload;
import echowand.net.Subnet;
import echowand.net.SubnetException;
import echowand.service.Core;
import echowand.service.Service;
import echowand.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ProxyRequestProcessor implements RequestProcessor {
    private static final Logger LOGGER = Logger.getLogger(ProxyRequestProcessor.class.getName());
    private static final String CLASS_NAME = ProxyRequestProcessor.class.getName();
    
    private static final int TIMEOUT = 10000;
    
    private HashMap<Subnet, Core> coreMap;
    private HashMap<ProxyObjectInfo, RemoteObjectInfo> proxyMap;
    
    private LinkedList<Transaction> transactions;
    
    private class ProxyObjectInfo {
        public final Core core;
        public final EOJ eoj;
        
        public ProxyObjectInfo(Core core, EOJ eoj) {
            this.core = core;
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
            
            if (!(o instanceof ProxyObjectInfo)) {
                return false;
            }
            
            ProxyObjectInfo other = (ProxyObjectInfo)o;
            
            return equals(core, other.core) && equals(eoj, other.eoj);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.core);
            hash = 67 * hash + Objects.hashCode(this.eoj);
            return hash;
        }
    }
    
    private class RemoteObjectInfo {
        public final Core core;
        public final Node node;
        public final EOJ eoj;
        
        public RemoteObjectInfo(Core core, Node node, EOJ eoj) {
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
            
            if (!(o instanceof RemoteObjectInfo)) {
                return false;
            }
            
            RemoteObjectInfo other = (RemoteObjectInfo)o;
            
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
            LOGGER.entering(CLASS_NAME, "ProxyTransactionListener.begin", t);
            
            addTransaction(t);
            
            LOGGER.exiting(CLASS_NAME, "ProxyTransactionListener.begin");
        }

        @Override
        public void send(Transaction t, Subnet subnet, Frame frame, boolean success) {
        }
        
        private StandardPayload createResponsePayload(StandardPayload payload) {
            LOGGER.entering(CLASS_NAME, "ProxyTransactionListener.createResponsePayload", payload);
            
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
            
            LOGGER.exiting(CLASS_NAME, "ProxyTransactionListener.createResponsePayload", newPayload);
            return newPayload;
        }

        @Override
        public void receive(Transaction t, Subnet subnet, Frame frame) {
            LOGGER.entering(CLASS_NAME, "ProxyTransactionListener.receive", frame);
                
            StandardPayload proxyPayload = frame.getCommonFrame().getEDATA(StandardPayload.class);
            
            if (proxyPayload == null) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyTransactionListener.receive", "invalid frame: " + frame);
            }
            
            CommonFrame commonFrame = new CommonFrame();
            commonFrame.setEDATA(createResponsePayload(proxyPayload));
            commonFrame.setTID(requestFrame.getCommonFrame().getTID());
            
            Subnet proxySubnet = proxyCore.getSubnet();
            Frame newFrame = new Frame(proxySubnet.getLocalNode(), requestFrame.getSender(), commonFrame, requestFrame.getConnection());
            
            try {
                proxyCore.getSubnet().send(newFrame);
            } catch (SubnetException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyTransactionListener.receive", "cannot send frame: " + newFrame, ex);
            }
            
            LOGGER.exiting(CLASS_NAME, "ProxyTransactionListener.receive");
        }

        @Override
        public void finish(Transaction t) {
            LOGGER.entering(CLASS_NAME, "ProxyTransactionListener.finish", t);
            
            removeTransaction(t);
            
            LOGGER.exiting(CLASS_NAME, "ProxyTransactionListener.finish");
        }
        
    }
    
    public ProxyRequestProcessor() {
        coreMap = new HashMap<Subnet, Core>();
        proxyMap = new HashMap<ProxyObjectInfo, RemoteObjectInfo>();
        transactions = new LinkedList<Transaction>();
    }
    
    private synchronized void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }
    
    private synchronized void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
    }
    
    private synchronized List<Transaction> cloneTransactions() {
        return new ArrayList<Transaction>(transactions);
    }
    
    
    public synchronized void registerProxyObject(Core proxyCore, EOJ proxyEOJ, Core remoteCore, Node remoteNode, EOJ remoteEOJ) {
        LOGGER.entering(CLASS_NAME, "registerProxyObject", new Object[]{proxyCore, proxyEOJ, remoteCore, remoteNode, remoteEOJ});
        
        ProxyObjectInfo proxyObjectInfo = new ProxyObjectInfo(proxyCore, proxyEOJ);
        RemoteObjectInfo remoteObjectInfo = new RemoteObjectInfo(remoteCore, remoteNode, remoteEOJ);
        
        coreMap.put(proxyCore.getSubnet(), proxyCore);
        coreMap.put(remoteCore.getSubnet(), remoteCore);
        
        proxyMap.put(proxyObjectInfo, remoteObjectInfo);
        
        LOGGER.exiting(CLASS_NAME, "registerProxyObject");
    }
    
    private synchronized Core getCoreOfSubet(Subnet subnet) {
        LOGGER.entering(CLASS_NAME, "getCoreOfSubet", subnet);
        
        Core core =  coreMap.get(subnet);
        
        LOGGER.exiting(CLASS_NAME, "getCoreOfSubet", core);
        return core;
    }
    
    private synchronized RemoteObjectInfo getRemoteObjectInfo(ProxyObjectInfo proxyObjectInfo) {
        LOGGER.entering(CLASS_NAME, "getCoreOfSubet", proxyObjectInfo);
        
        RemoteObjectInfo remoteObjectInfo =  proxyMap.get(proxyObjectInfo);
        
        LOGGER.exiting(CLASS_NAME, "getCoreOfSubet", remoteObjectInfo);
        return remoteObjectInfo;
    }
    
    private synchronized List<ProxyObjectInfo> getProxyObjectInfoList(RemoteObjectInfo remoteObjectInfo) {
        LOGGER.entering(CLASS_NAME, "getProxyObjectInfoList", remoteObjectInfo);
        
        LinkedList<ProxyObjectInfo> proxyList = new LinkedList<ProxyObjectInfo>();
        
        for (Map.Entry<ProxyObjectInfo,RemoteObjectInfo> entry : proxyMap.entrySet()) {
            if (entry.getValue().equals(remoteObjectInfo)) {
                proxyList.add(entry.getKey());
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "getProxyObjectInfoList", proxyList);
        return proxyList;
    }
    
    private boolean processPayloadSetGet(Subnet subnet, Frame frame, StandardPayload payload) {
        LOGGER.entering(CLASS_NAME, "processPayloadSetGet", new Object[]{subnet, frame, payload});
                
        Core proxyCore = getCoreOfSubet(subnet);
        
        if (proxyCore == null) {
            LOGGER.exiting(CLASS_NAME, "processPayloadSetGet", false);
            return false;
        }
        
        ProxyObjectInfo proxyInfo = new ProxyObjectInfo(proxyCore, payload.getDEOJ());
        RemoteObjectInfo remoteInfo = getRemoteObjectInfo(proxyInfo);
        
        if (remoteInfo == null) {
            LOGGER.exiting(CLASS_NAME, "processPayloadSetGet", false);
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
                LOGGER.exiting(CLASS_NAME, "processPayloadSetGet", false);
                return false;
        }
        
        Transaction transaction = new Transaction(remoteInfo.core.getSubnet(), remoteInfo.core.getTransactionManager(), transactionConfig);
        
        transaction.addTransactionListener(new ProxyTransactionListener(proxyCore, frame));
        
        transaction.setTimeout(TIMEOUT);
        
        try {
            transaction.execute();
            LOGGER.exiting(CLASS_NAME, "processPayloadSetGet", false);
            return true;
        } catch (SubnetException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "processPayloadSetGet", "cannot execute transaction: " + transaction, ex);
            LOGGER.exiting(CLASS_NAME, "processPayloadSetGet", false);
            return false;
        }
    }
    
    private List<Pair<EPC, Data>> toPairs(StandardPayload payload) {
        LinkedList<Pair<EPC, Data>> pairs = new LinkedList<Pair<EPC, Data>>();
        int size = 0x00ff & payload.getFirstOPC();
        
        for (int i=0; i<size; i++) {
            Property property = payload.getFirstPropertyAt(i);
            pairs.add(new Pair<EPC, Data>(property.getEPC(), property.getEDT()));
        }
        
        return pairs;
    }
    
    private boolean isTransactionOfINF(Transaction transaction, Subnet subnet, Frame frame, StandardPayload payload) {
        LOGGER.entering(CLASS_NAME, "isTransactionOfINF", new Object[]{transaction, subnet, frame, payload});
        
        if (transaction.getTID() != frame.getCommonFrame().getTID()) {
            LOGGER.exiting(CLASS_NAME, "isTransactionOfINF", false);
            return false;
        }
        
        TransactionConfig transactionConfig = transaction.getTransactionConfig();

        if (transactionConfig.getESV() != ESV.INF_REQ) {
            LOGGER.exiting(CLASS_NAME, "isTransactionOfINF", false);
            return false;
        }

        if (!transactionConfig.getReceiverNode().equals(subnet.getGroupNode())) {
            if (!transactionConfig.getReceiverNode().equals(frame.getSender())) {
                LOGGER.exiting(CLASS_NAME, "isTransactionOfINF", false);
                return false;
            }
        }

        if (transactionConfig.getDestinationEOJ().isAllInstance()) {
            ClassEOJ ceoj = transactionConfig.getDestinationEOJ().getClassEOJ();
            if (!payload.getSEOJ().isMemberOf(ceoj)) {
                LOGGER.exiting(CLASS_NAME, "isTransactionOfINF", false);
                return false;
            }
        } else {
            EOJ eoj = transactionConfig.getDestinationEOJ();
            if (!payload.getSEOJ().equals(eoj)) {
                LOGGER.exiting(CLASS_NAME, "isTransactionOfINF", false);
                return false;
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "isTransactionOfINF", true);
        return true;
    }
    
    private Transaction getTransactionOfINF(Subnet subnet, Frame frame, StandardPayload payload) {
        LOGGER.entering(CLASS_NAME, "getTransactionOfINF", new Object[]{subnet, frame, payload});
        
        for (Transaction transaction : cloneTransactions()) {
            if (isTransactionOfINF(transaction, subnet, frame, payload)) {
                LOGGER.exiting(CLASS_NAME, "getTransactionOfINF", transaction);
                return transaction;
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "getTransactionOfINF", null);
        return null;
    }
    
    private boolean processPayloadINF(Subnet subnet, Frame frame, StandardPayload payload) {
        LOGGER.entering(CLASS_NAME, "processPayloadINF", new Object[]{subnet, frame, payload});
        
        Core remoteCore = getCoreOfSubet(subnet);
        
        if (remoteCore == null) {
            LOGGER.exiting(CLASS_NAME, "processPayloadINF", false);
            return false;
        }
        
        
        /********** Transaction support begin **********/
        /*
         * Transaction supporsed to receive INF frame(s) as a reply of an INF_REQ frame.
         * However there is a bug in MainLoop, Listener and RequestProcessor, which prevents Transaction to receive INF frame.
         *
         * The lines belows are quick hack to avoid this bug.
         * These lines must be deleted when the bug is fixed.
         */
        
        Transaction transaction = getTransactionOfINF(subnet, frame, payload);
        
        if (transaction != null) {
            transaction.receiveResponse(frame);
            return true;
        }
        
        /********** Transaction support end **********/
        
        RemoteObjectInfo remoteInfo = new RemoteObjectInfo(remoteCore, frame.getSender(), payload.getSEOJ());
        
        List<ProxyObjectInfo> proxyInfoList = getProxyObjectInfoList(remoteInfo);
        
        List<Pair<EPC, Data>> properties = toPairs(payload);
        
        for (ProxyObjectInfo proxyInfo : proxyInfoList) {
            Service service = new Service(proxyInfo.core);
            Node node = service.getGroupNode();
            EOJ eoj = proxyInfo.eoj;
            
            try {
                service.doNotify(node, eoj, properties);
            } catch (SubnetException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "processPayloadINF", "cannot send notification: " + proxyInfo.core + " " + node + " " + eoj);
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "processPayloadINF", false);
        return false;
    }
    
    public boolean processFrameSetGet(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processFrameSetGet", new Object[]{subnet, frame, processed});
        
        if (processed) {
            LOGGER.exiting(CLASS_NAME, "processFrameSetGet", false);
            return false;
        }
        
        StandardPayload payload = frame.getCommonFrame().getEDATA(StandardPayload.class);
        
        if (payload == null) {
            LOGGER.exiting(CLASS_NAME, "processFrameSetGet", false);
            return false;
        }
        
        boolean result = processPayloadSetGet(subnet, frame, payload);
        LOGGER.exiting(CLASS_NAME, "processFrameSetGet", result);
        return result;
    }

    public boolean processFrameINF(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processFrameINF", new Object[]{subnet, frame, processed});
        
        if (processed) {
            LOGGER.exiting(CLASS_NAME, "processFrameINF", false);
            return false;
        }
        
        StandardPayload payload = frame.getCommonFrame().getEDATA(StandardPayload.class);
        
        if (payload == null) {
            LOGGER.exiting(CLASS_NAME, "processFrameINF", false);
            return false;
        }
        
        
        boolean result = processPayloadINF(subnet, frame, payload);
        LOGGER.exiting(CLASS_NAME, "processFrameINF", result);
        return result;
    }

    @Override
    public boolean processSetI(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processINF_REQ", new Object[]{subnet, frame, processed});
        
        boolean result = processFrameSetGet(subnet, frame, processed);
        
        LOGGER.entering(CLASS_NAME, "processINF_REQ", result);
        return result;
    }

    @Override
    public boolean processSetC(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processINF_REQ", new Object[]{subnet, frame, processed});
        
        boolean result = processFrameSetGet(subnet, frame, processed);
        
        LOGGER.entering(CLASS_NAME, "processINF_REQ", result);
        return result;
    }

    @Override
    public boolean processGet(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processINF_REQ", new Object[]{subnet, frame, processed});
        
        boolean result = processFrameSetGet(subnet, frame, processed);
        LOGGER.entering(CLASS_NAME, "processINF_REQ", result);
        return result;
    }
    
    @Override
    public boolean processSetGet(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processINF_REQ", new Object[]{subnet, frame, processed});
        
        boolean result = processFrameSetGet(subnet, frame, processed);
        
        LOGGER.entering(CLASS_NAME, "processINF_REQ", result);
        return result;
    }

    @Override
    public boolean processINF_REQ(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processINF_REQ", new Object[]{subnet, frame, processed});
        
        boolean result = processFrameSetGet(subnet, frame, processed);
        
        LOGGER.entering(CLASS_NAME, "processINF_REQ", result);
        return result;
    }

    @Override
    public boolean processINF(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processINF", new Object[]{subnet, frame, processed});
        
        boolean result = processFrameINF(subnet, frame, processed);
        
        LOGGER.entering(CLASS_NAME, "processINF", result);
        return result;
    }

    @Override
    public boolean processINFC(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processINFC", new Object[]{subnet, frame, processed});
        
        boolean result = processFrameINF(subnet, frame, processed);
        
        LOGGER.entering(CLASS_NAME, "processINFC", result);
        return result;
    }
}
