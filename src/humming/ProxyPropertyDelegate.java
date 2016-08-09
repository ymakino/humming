package humming;

import echowand.common.Data;
import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.net.NodeInfo;
import echowand.net.SubnetException;
import echowand.object.EchonetObjectException;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.object.RemoteObject;
import echowand.object.RemoteObjectObserver;
import echowand.service.Core;
import echowand.service.PropertyDelegate;
import echowand.service.Service;
import echowand.util.Pair;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ProxyPropertyDelegate extends PropertyDelegate {
    private static final Logger LOGGER = Logger.getLogger(ProxyPropertyDelegate.class.getName());
    private static final String CLASS_NAME = ProxyPropertyDelegate.class.getName();
    
    private Service remoteService;
    private Core remoteCore;
    private NodeInfo remoteNode;
    private EOJ remoteEOJ;
    private EPC remoteEPC;
    
    private class ProxyPropertyRemoteObjectObserver implements RemoteObjectObserver {
        @Override
        public void notifyData(RemoteObject object, EPC epc, ObjectData data) {
            if (remoteEPC != epc || !isNotifyEnabled()) {
                return;
            }
            
            LocalObject proxyObject = getLocalObject();
            
            if (proxyObject != null) {
                Service service = new Service(getCore());
                
                LinkedList<Pair<EPC, Data>> properties = new LinkedList<Pair<EPC, Data>>();
                properties.add(new Pair<EPC, Data>(getEPC(), data.getData()));
                
                for (int i=0; i<data.getExtraSize(); i++) {
                    properties.add(new Pair<EPC, Data>(getEPC(), data.getExtraDataAt(i)));
                }
                
                try {
                    LOGGER.logp(Level.INFO, CLASS_NAME, "ProxyPropertyRemoteObjectObserver.notifyData", object + ", EPC: " + remoteEPC + ", data: " + data + " -> " + proxyObject + ", EPC: " + getEPC());
                    service.doNotify(proxyObject.getEOJ(), properties);
                } catch (SubnetException ex) {
                    LOGGER.logp(Level.INFO, CLASS_NAME, "ProxyPropertyRemoteObjectObserver.notifyData", "failed: " + object + ", EPC: " + remoteEPC + ", data: " + data + " -> " + proxyObject + ", EPC: " + getEPC(), ex);
                }
            }
        }
    }
    
    public ProxyPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Core remoteCore, NodeInfo remoteNode, EOJ remoteEOJ, EPC remoteEPC) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        
        this.remoteCore = remoteCore;
        this.remoteNode = remoteNode;
        this.remoteEOJ = remoteEOJ;
        this.remoteEPC = remoteEPC;
                
        LOGGER.logp(Level.INFO, CLASS_NAME, "ProxyPropertyDelegate", "epc: " + epc + " -> proxyNode: " + remoteNode + ", proxyEOJ: " + remoteEOJ + ", proxyEPC: " + remoteEPC);
    }
    
    public RemoteObject getRemoteObject() throws SubnetException {
        if (!remoteService.getCore().isInitialized()) {
            throw new SubnetException("not initialized: " + remoteService.getCore());
        }
        
        return remoteService.getRemoteObject(remoteNode, remoteEOJ);
    }
    
    @Override
    public void notifyCreation(LocalObject object, Core core) {
        remoteService = new Service(remoteCore);
        
        try {
            remoteService.registerRemoteEOJ(remoteNode, remoteEOJ);
            RemoteObject remoteObject = remoteService.getRemoteObject(remoteNode, remoteEOJ);

            if (remoteObject == null) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyPropertyDelegateCoreListener.initialized", "cannot register: " + remoteNode + " " + remoteEOJ);
                return;
            }

            remoteObject.addObserver(new ProxyPropertyRemoteObjectObserver());
        } catch (SubnetException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyPropertyDelegateCoreListener.initialized", "cannot register: " + remoteNode + " " + remoteEOJ, ex);
        }
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        LOGGER.entering(CLASS_NAME, "getUserData", new Object[]{object, epc});
        
        ObjectData data = null;
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "begin: " + object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + remoteEPC);
            data = getRemoteObject().getData(remoteEPC);
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "end: " + object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + remoteEPC + ", data: " + data);
        } catch (SubnetException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed", ex);
        } catch (EchonetObjectException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed", ex);
        }
        
        LOGGER.exiting(CLASS_NAME, "getUserData", data);
        return data;
    }

    @Override
    public boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        LOGGER.entering(CLASS_NAME, "setUserData", new Object[]{object, epc, data});
        
        boolean result = false;
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "begin: " + object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + remoteEPC + ", data: " + data);
            result = getRemoteObject().setData(remoteEPC, data);
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "end: " + object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + remoteEPC + ", data: " + data + ", result: " + result);
        } catch (SubnetException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed", ex);
        } catch (EchonetObjectException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed", ex);
        }
        
        LOGGER.exiting(CLASS_NAME, "setUserData", result);
        return result;
    }
}
