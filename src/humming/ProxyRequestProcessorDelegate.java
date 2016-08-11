package humming;

import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.net.NodeInfo;
import echowand.net.SubnetException;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.Core;
import echowand.service.LocalObjectServiceDelegate;
import echowand.service.Service;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ProxyRequestProcessorDelegate implements LocalObjectServiceDelegate {
    private static final Logger LOGGER = Logger.getLogger(ProxyRequestProcessorDelegate.class.getName());
    private static final String CLASS_NAME = ProxyRequestProcessorDelegate.class.getName();
    
    private LocalObject proxyObject;
    
    private Service remoteService;
    private Core remoteCore;
    private NodeInfo remoteNode;
    private EOJ remoteEOJ;
    
    public ProxyRequestProcessorDelegate(Core remoteCore, NodeInfo remoteNode, EOJ remoteEOJ) {
        this.remoteCore = remoteCore;
        this.remoteNode = remoteNode;
        this.remoteEOJ = remoteEOJ;
    }

    @Override
    public void notifyCreation(LocalObject object, Core core) {
        LOGGER.entering(CLASS_NAME, "notifyCreation", new Object[]{object, core});
        
        proxyObject = object;
        remoteService = new Service(remoteCore);
        
        ProxyRequestProcessor processor = new ProxyRequestProcessor();
        try {
            processor.registerProxyObject(core, proxyObject.getEOJ(), remoteCore, remoteService.getRemoteNode(remoteNode), remoteEOJ);
            core.getRequestDispatcher().addRequestProcessor(0, processor);
            remoteCore.getRequestDispatcher().addRequestProcessor(0, processor);
        } catch (SubnetException ex) {
            Logger.getLogger(ProxyRequestProcessorDelegate.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void getData(GetState result, LocalObject object, EPC epc) {
    }

    @Override
    public void setData(SetState result, LocalObject object, EPC epc, ObjectData newData, ObjectData curData) {
    }

    @Override
    public void notifyDataChanged(NotifyState result, LocalObject object, EPC epc, ObjectData curData, ObjectData oldData) {
    }
    
}
