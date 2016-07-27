package humming;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ProxyDelegate implements LocalObjectServiceDelegate {
    private static final Logger LOGGER = Logger.getLogger(ProxyDelegate.class.getName());
    private static final String CLASS_NAME = ProxyDelegate.class.getName();
    
    private LocalObject localObject;
    private Service proxyService;
    private Core proxyCore;
    private NodeInfo proxyNode;
    private EOJ proxyEOJ;
    
    private class ProxyDelegateRemoteObjectObserver implements RemoteObjectObserver {
        @Override
        public void notifyData(RemoteObject object, EPC epc, ObjectData data) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "ProxyDelegateRemoteObjectObserver.notifyData", object + ", EPC: " + epc + ", data: " + data + " -> " + localObject);
            localObject.notifyDataChanged(epc, data, null);
        }
    }
    
    public RemoteObject getRemoteObject() throws SubnetException {
        if (!proxyService.getCore().isInitialized()) {
            return null;
        }
        
        return proxyService.getRemoteObject(proxyNode, proxyEOJ);
    }
    
    public ProxyDelegate(Core proxyCore, NodeInfo proxyNode, EOJ proxyEOJ) {
        
        this.proxyCore = proxyCore;
        this.proxyNode = proxyNode;
        this.proxyEOJ = proxyEOJ;
    }

    @Override
    public void notifyCreation(LocalObject object, Core core) {
        localObject = object;
        proxyService = new Service(proxyCore);
        
        try {
            proxyService.registerRemoteEOJ(proxyNode, proxyEOJ);
            RemoteObject remoteObject = proxyService.getRemoteObject(proxyNode, proxyEOJ);

            if (remoteObject == null) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyPropertyDelegateCoreListener.initialized", "cannot register: " + proxyNode + " " + proxyEOJ);
                return;
            }

            remoteObject.addObserver(new ProxyDelegateRemoteObjectObserver());
        } catch (SubnetException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyPropertyDelegateCoreListener.initialized", "cannot register: " + proxyNode + " " + proxyEOJ, ex);
        }
    }

    @Override
    public void getData(GetState result, LocalObject object, EPC epc) {
        LOGGER.entering(CLASS_NAME, "getData", new Object[]{result, object, epc});
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getData", "begin: " + object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + epc);
            ObjectData data = proxyService.getRemoteData(proxyNode, proxyEOJ, epc);
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
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "begin: " + object + ", EPC: " + epc + " -> " + proxyNode + ", EPC: " + epc + ", data: " + newData);
            boolean success = proxyService.setRemoteData(proxyNode, proxyEOJ, epc, curData);
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "end: " + object + ", EPC: " + epc + " -> " + proxyNode + ", EPC: " + epc + ", data: " + newData + ", result: " + success);
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
        
        result.setDone();
        
        LOGGER.exiting(CLASS_NAME, "notifyDataChanged");
    }
}
