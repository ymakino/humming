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
import echowand.service.PropertyDelegate;
import echowand.service.Service;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ProxyPropertyDelegate extends PropertyDelegate {
    private static final Logger LOGGER = Logger.getLogger(ProxyPropertyDelegate.class.getName());
    private static final String CLASS_NAME = ProxyPropertyDelegate.class.getName();
    
    private Service proxyService;
    private Core proxyCore;
    private NodeInfo proxyNode;
    private EOJ proxyEOJ;
    private EPC proxyEPC;
    
    private class ProxyRemoteObjectObserver implements RemoteObjectObserver {
        @Override
        public void notifyData(RemoteObject object, EPC epc, ObjectData data) {
            if (proxyEPC != epc || !isNotifyEnabled()) {
                return;
            }
            
            LocalObject localObject = getLocalObject();
            
            if (localObject != null) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "ProxyRemoteObjectObserver.notifyData", object + ", EPC: " + proxyEPC + ", data: " + data + " -> " + localObject + ", EPC: " + getEPC());
                localObject.notifyDataChanged(getEPC(), data, null);
            }
        }
    }
    
    public ProxyPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Core proxyCore, NodeInfo proxyNode, EOJ proxyEOJ, EPC proxyEPC) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        
        this.proxyCore = proxyCore;
        this.proxyNode = proxyNode;
        this.proxyEOJ = proxyEOJ;
        this.proxyEPC = proxyEPC;
                
        LOGGER.logp(Level.INFO, CLASS_NAME, "ProxyPropertyDelegate", "epc: " + epc + " -> proxyNode: " + proxyNode + ", proxyEOJ: " + proxyEOJ + ", proxyEPC: " + proxyEPC);
    }
    
    public RemoteObject getRemoteObject() throws SubnetException {
        if (!proxyService.getCore().isInitialized()) {
            return null;
        }
        
        return proxyService.getRemoteObject(proxyNode, proxyEOJ);
    }
    
    @Override
    public void notifyCreation(LocalObject object, Core core) {
        proxyService = new Service(proxyCore);
        
        try {
            proxyService.registerRemoteEOJ(proxyNode, proxyEOJ);
            RemoteObject remoteObject = proxyService.getRemoteObject(proxyNode, proxyEOJ);

            if (remoteObject == null) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyPropertyDelegateCoreListener.initialized", "cannot register: " + proxyNode + " " + proxyEOJ);
                return;
            }

            remoteObject.addObserver(new ProxyRemoteObjectObserver());
        } catch (SubnetException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "ProxyPropertyDelegateCoreListener.initialized", "cannot register: " + proxyNode + " " + proxyEOJ, ex);
        }
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        LOGGER.entering(CLASS_NAME, "getUserData", new Object[]{object, epc});
        
        ObjectData data = null;
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "begin: " + object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + proxyEPC);
            data = getRemoteObject().getData(proxyEPC);
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "end: " + object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + proxyEPC + ", data: " + data);
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
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "begin: " + object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + proxyEPC + ", data: " + data);
            result = getRemoteObject().setData(proxyEPC, data);
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "end: " + object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + proxyEPC + ", data: " + data + ", result: " + result);
        } catch (SubnetException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed", ex);
        } catch (EchonetObjectException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed", ex);
        }
        
        LOGGER.exiting(CLASS_NAME, "setUserData", result);
        return result;
    }
}
