package humming;

import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.net.NodeInfo;
import echowand.net.SubnetException;
import echowand.object.EchonetObjectException;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.object.RemoteObject;
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
    private Core core;
    private NodeInfo proxyNode;
    private EOJ proxyEOJ;
    private EPC proxyEPC;
    
    public ProxyPropertyDelegate(Core core, EPC epc, NodeInfo proxyNode, EOJ proxyEOJ, EPC proxyEPC) {
        super(epc, true, false, false);
        this.core = core;
        this.proxyNode = proxyNode;
        this.proxyEOJ = proxyEOJ;
        this.proxyEPC = proxyEPC;
    }
    
    private Service getService() {
        return new Service(core);
    }
    
    private RemoteObject getRemoteObject() throws SubnetException {
        RemoteObject remoteObject = getService().getRemoteObject(proxyNode, proxyEOJ);
        if (remoteObject == null) {
            remoteObject = getService().registerRemoteEOJ(proxyNode, proxyEOJ);
        }
        return remoteObject;
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        try {
            return getRemoteObject().getData(epc);
        } catch (SubnetException ex) {
            Logger.getLogger(ProxyPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EchonetObjectException ex) {
            Logger.getLogger(ProxyPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }

    @Override
    public boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        try {
            return getRemoteObject().setData(epc, data);
        } catch (SubnetException ex) {
            Logger.getLogger(ProxyPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EchonetObjectException ex) {
            Logger.getLogger(ProxyPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
}
