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
import echowand.service.CoreDefaultListener;
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
    
    private Service proxyService;
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
            logger.logp(Level.INFO, className, "ProxyRemoteObjectObserver.notifyData", object + ", EPC: " + proxyEPC + ", data: " + data + " -> " + localObject + ", EPC: " + getEPC());
            localObject.notifyDataChanged(getEPC(), data, null);
        }
    }
    
    private class ProxyPropertyDelegateCoreListener extends CoreDefaultListener {
        @Override
        public void initialized(Core core) {
            try {
                RemoteObject remoteObject = proxyService.getRemoteObject(proxyNode, proxyEOJ);

                if (remoteObject == null) {
                    proxyService.registerRemoteEOJ(proxyNode, proxyEOJ);
                    remoteObject = proxyService.getRemoteObject(proxyNode, proxyEOJ);
                }
                
                if (remoteObject == null) {
                    logger.logp(Level.WARNING, className, "ProxyPropertyDelegateCoreListener.started", "cannot register: " + proxyNode + " " + proxyEOJ);
                    return;
                }
                
                remoteObject.addObserver(new ProxyRemoteObjectObserver());
            } catch (SubnetException ex) {
                Logger.getLogger(ProxyPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public ProxyPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Core proxyCore, NodeInfo proxyNode, EOJ proxyEOJ, EPC proxyEPC) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        
        proxyService = new Service(proxyCore);
        this.proxyNode = proxyNode;
        this.proxyEOJ = proxyEOJ;
        this.proxyEPC = proxyEPC;
        
        if (proxyCore.isInitialized()) {
            new ProxyPropertyDelegateCoreListener().initialized(proxyCore);
        } else {
            proxyCore.addListener(new ProxyPropertyDelegateCoreListener());
        }
                
        logger.logp(Level.INFO, className, "ProxyPropertyDelegate", "epc: " + epc + " -> proxyNode: " + proxyNode + ", proxyEOJ: " + proxyEOJ + ", proxyEPC: " + proxyEPC);
    }
    
    public RemoteObject getRemoteObject() throws SubnetException {
        if (!proxyService.getCore().isInitialized()) {
            return null;
        }
        
        return proxyService.getRemoteObject(proxyNode, proxyEOJ);
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        logger.entering(className, "getUserData", new Object[]{object, epc});
        
        ObjectData data = null;
        
        try {
            logger.logp(Level.INFO, className, "getUserData begin", object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + proxyEPC);
            data = getRemoteObject().getData(proxyEPC);
            logger.logp(Level.INFO, className, "getUserData end", object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + proxyEPC + ", data: " + data);
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
        
        boolean result = false;
        
        try {
            logger.logp(Level.INFO, className, "setUserData begin", object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + proxyEPC + ", data: " + data);
            result = getRemoteObject().setData(proxyEPC, data);
            logger.logp(Level.INFO, className, "setUserData end", object + ", EPC: " + epc + " -> " + getRemoteObject() + ", EPC: " + proxyEPC + ", data: " + data + ", result: " + result);
        } catch (SubnetException ex) {
            Logger.getLogger(ProxyPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EchonetObjectException ex) {
            Logger.getLogger(ProxyPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        logger.exiting(className, "setUserData", result);
        return result;
    }
}
