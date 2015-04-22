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
    private static final Logger logger = Logger.getLogger(ProxyPropertyDelegate.class.getName());
    private static final String className = ProxyPropertyDelegate.class.getName();
    
    private Core core;
    private Service service;
    private NodeInfo proxyNode;
    private EOJ proxyEOJ;
    private EPC proxyEPC;
    private LocalObject localObject = null;
    
    private class ProxyRemoteObjectObserver implements RemoteObjectObserver {
        @Override
        public void notifyData(RemoteObject object, EPC epc, ObjectData data) {
            if (localObject == null || !isNotifyEnabled() || proxyEPC != epc) {
                return;
            }
            
            logger.logp(Level.INFO, className, "ProxyRemoteObjectObserver.notifyData", object + ", EPC: " + proxyEPC + ", data: " + data + " -> " + localObject + ", EPC: " + getEPC());
            localObject.notifyDataChanged(getEPC(), data, null);
        }
    }
    
    public ProxyPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Core core, NodeInfo proxyNode, EOJ proxyEOJ, EPC proxyEPC) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        this.core = core;
        service = new Service(core);
        this.proxyNode = proxyNode;
        this.proxyEOJ = proxyEOJ;
        this.proxyEPC = proxyEPC;
        logger.logp(Level.INFO, className, "ProxyPropertyDelegate", "epc: " + epc + " -> proxyNode: " + proxyNode + ", proxyEOJ: " + proxyEOJ + ", proxyEPC: " + proxyEPC);
    }
    
    private RemoteObject getRemoteObject() throws SubnetException {
        RemoteObject remoteObject = service.getRemoteObject(proxyNode, proxyEOJ);
        if (remoteObject == null) {
            remoteObject = service.registerRemoteEOJ(proxyNode, proxyEOJ);
            remoteObject.addObserver(new ProxyRemoteObjectObserver());
        }
        return remoteObject;
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        logger.entering(className, "getUserData", new Object[]{object, epc});
        
        localObject = object;
        ObjectData data = null;
        
        try {
            data = getRemoteObject().getData(proxyEPC);
            logger.logp(Level.INFO, className, "getUserData", object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + proxyEPC + ", data: " + data);
        } catch (SubnetException ex) {
            Logger.getLogger(ProxyPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EchonetObjectException ex) {
            Logger.getLogger(ProxyPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        logger.exiting(className, "getUserData", data);
        return data;
    }

    @Override
    public boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        logger.entering(className, "setUserData", new Object[]{object, epc, data});
        
        localObject = object;
        boolean result = false;
        
        try {
            result = getRemoteObject().setData(proxyEPC, data);
            logger.logp(Level.INFO, className, "setUserData", object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + proxyEPC + ", data: " + data + ", result: " + result);
        } catch (SubnetException ex) {
            Logger.getLogger(ProxyPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EchonetObjectException ex) {
            Logger.getLogger(ProxyPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        logger.exiting(className, "setUserData", result);
        return result;
    }
}
