package humming;

import echowand.common.ClassEOJ;
import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.net.InetNodeInfo;
import echowand.net.NodeInfo;
import echowand.net.SubnetException;
import echowand.service.Core;
import echowand.service.PropertyDelegate;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class ProxyPropertyDelegateCreator implements PropertyDelegateCreator {
    private static final Logger logger = Logger.getLogger(ProxyPropertyDelegateCreator.class.getName());
    private static final String className = ProxyPropertyDelegateCreator.class.getName();
    
    private Core core;
    
    public ProxyPropertyDelegateCreator(Core core) {
        this.core = core;
    }
    
    private NodeInfo parseNodeInfo(Node node) throws SubnetException {
        return core.getSubnet().getRemoteNode(node.getTextContent()).getNodeInfo();
    }
    
    private EOJ parseEOJInfo(Node node) throws SubnetException {
        return new EOJ(node.getTextContent());
    }
    
    private EPC parseEPCInfo(Node node) throws SubnetException {
        String epcName = node.getTextContent();
        if (epcName.toLowerCase().startsWith("0x")) {
            epcName = epcName.substring(2);
        }

        byte b = (byte)Integer.parseInt(epcName, 16);
        return EPC.fromByte(b);
    }
    
    private EOJ parseInstanceInfo(Node node, ClassEOJ ceoj) throws SubnetException {
        String instanceStr = node.getTextContent();
        byte instanceCode = (byte)Integer.parseInt(instanceStr);
        return ceoj.getEOJWithInstanceCode(instanceCode);
    }
    
    @Override
    public PropertyDelegate newPropertyDelegate(ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) {
        NodeInfo proxyNode = null;
        EOJ proxyEOJ = null;
        EPC proxyEPC = null;
        
        try {
            NodeList nodeList = node.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node proxyInfo = nodeList.item(i);
                String infoName = proxyInfo.getNodeName();
                if (infoName.equals("node")) {
                    proxyNode = parseNodeInfo(proxyInfo);
                } else if (infoName.equals("eoj")) {
                    proxyEOJ = parseEOJInfo(proxyInfo);
                } else if (infoName.equals("instance")) {
                    proxyEOJ = parseInstanceInfo(proxyInfo, ceoj);
                } else if (infoName.equals("epc")) {
                    proxyEPC = parseEPCInfo(proxyInfo);
                }
            }
        } catch (SubnetException ex) {
            Logger.getLogger(ProxyPropertyDelegateCreator.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        if (proxyEPC == null) {
            proxyEPC = epc;
        }
        
        if (proxyNode != null && proxyEOJ != null && proxyEPC != null) {
            return new ProxyPropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, core, proxyNode, proxyEOJ, proxyEPC);
        }
        
        return null;
    }
}
