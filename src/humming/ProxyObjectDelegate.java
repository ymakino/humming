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
import echowand.service.LocalObjectServiceDelegate;
import echowand.service.ObjectNotFoundException;
import echowand.service.Service;
import echowand.util.Pair;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ProxyObjectDelegate implements LocalObjectServiceDelegate {
    private static final Logger LOGGER = Logger.getLogger(ProxyObjectDelegate.class.getName());
    private static final String CLASS_NAME = ProxyObjectDelegate.class.getName();
    
    private Service service;
    private LocalObject proxyObject;
    
    private Service remoteService;
    private Core remoteCore;
    private NodeInfo remoteNode;
    private EOJ remoteEOJ;
    
    private class ProxyDelegateRemoteObjectObserver implements RemoteObjectObserver {
        @Override
        public void notifyData(RemoteObject object, EPC epc, ObjectData data) {
            try {
                LOGGER.logp(Level.INFO, CLASS_NAME, "ProxyDelegateRemoteObjectObserver.notifyData", "begin: " + object + ", EPC: " + epc + ", data: " + data + " -> " + proxyObject);
            
                LinkedList<Pair<EPC, Data>> properties = new LinkedList<Pair<EPC, Data>>();
                properties.add(new Pair<EPC, Data>(epc, data.getData()));
                
                for (int i=0; i<data.getExtraSize(); i++) {
                    properties.add(new Pair<EPC, Data>(epc, data.getExtraDataAt(i)));
                }
                
                service.doNotify(proxyObject.getEOJ(), properties);
                
            LOGGER.logp(Level.INFO, CLASS_NAME, "ProxyDelegateRemoteObjectObserver.notifyData", "end: " + object + ", EPC: " + epc + ", data: " + data + " -> " + proxyObject);
            } catch (SubnetException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyDelegateRemoteObjectObserver.notifyData", "failed: " + object + ", EPC: " + epc + ", data: " + data + " -> " + proxyObject, ex);
            }
        }
    }
    
    public RemoteObject getRemoteObject() throws SubnetException {
        if (!remoteService.getCore().isInitialized()) {
            return null;
        }
        
        return remoteService.getRemoteObject(remoteNode, remoteEOJ);
    }
    
    public ProxyObjectDelegate(Core remoteCore, NodeInfo remoteNode, EOJ remoteEOJ) {
        this.remoteCore = remoteCore;
        this.remoteNode = remoteNode;
        this.remoteEOJ = remoteEOJ;
    }

    @Override
    public void notifyCreation(LocalObject object, Core core) {
        LOGGER.entering(CLASS_NAME, "notifyCreation", new Object[]{object, core});
        
        proxyObject = object;
        service = new Service(core);
        remoteService = new Service(remoteCore);
        
        try {
            remoteService.registerRemoteEOJ(remoteNode, remoteEOJ);
            RemoteObject remoteObject = remoteService.getRemoteObject(remoteNode, remoteEOJ);

            if (remoteObject == null) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyPropertyDelegateCoreListener.initialized", "cannot register: " + remoteNode + " " + remoteEOJ);
                LOGGER.exiting(CLASS_NAME, "notifyCreation");
                return;
            }

            remoteObject.addObserver(new ProxyDelegateRemoteObjectObserver());
        } catch (SubnetException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyPropertyDelegateCoreListener.initialized", "cannot register: " + remoteNode + " " + remoteEOJ, ex);
        }
        
        LOGGER.exiting(CLASS_NAME, "notifyCreation");
        return;
    }

    @Override
    public void getData(GetState result, LocalObject object, EPC epc) {
        LOGGER.entering(CLASS_NAME, "getData", new Object[]{result, object, epc});
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getData", "begin: " + object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + epc);
            ObjectData data = remoteService.getRemoteData(remoteNode, remoteEOJ, epc);
            result.setGetData(data);
            LOGGER.logp(Level.INFO, CLASS_NAME, "getData", "end: " + object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + epc + ", data: " + data);
        } catch (ObjectNotFoundException ex) {
            result.setFail();
        } catch (SubnetException ex) {
            result.setFail();
        } catch (EchonetObjectException ex) {
            result.setFail();
        }
        
        LOGGER.exiting(CLASS_NAME, "getData");
    }

    @Override
    public void setData(SetState result, LocalObject object, EPC epc, ObjectData newData, ObjectData curData) {
        LOGGER.entering(CLASS_NAME, "setData", new Object[]{result, object, epc, newData, curData});
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "begin: " + object + ", EPC: " + epc + " -> " + remoteNode + ", EPC: " + epc + ", data: " + newData);
            boolean success = remoteService.setRemoteData(remoteNode, remoteEOJ, epc, newData);
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "end: " + object + ", EPC: " + epc + " -> " + remoteNode + ", EPC: " + epc + ", data: " + newData + ", result: " + success);
            if (success) {
                result.setDone();
            } else {
                result.setFail();
            }
        } catch (ObjectNotFoundException ex) {
            result.setFail();
        } catch (SubnetException ex) {
            result.setFail();
        } catch (EchonetObjectException ex) {
            result.setFail();
        }
        
        LOGGER.exiting(CLASS_NAME, "setData");
    }

    @Override
    public void notifyDataChanged(NotifyState result, LocalObject object, EPC epc, ObjectData curData, ObjectData oldData) {
        LOGGER.entering(CLASS_NAME, "notifyDataChanged", new Object[]{result, object, epc, curData, oldData});
        
        LOGGER.exiting(CLASS_NAME, "notifyDataChanged");
    }
}
