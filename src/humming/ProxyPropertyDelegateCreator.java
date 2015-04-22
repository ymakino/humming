package humming;

import echowand.common.ClassEOJ;
import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.net.NodeInfo;
import echowand.net.SubnetException;
import echowand.service.Core;
import echowand.service.PropertyDelegate;
import java.util.HashMap;
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
    private HashMap<String, Core> coreMap;
    
    public ProxyPropertyDelegateCreator(Core proxyCore) {
        core = proxyCore;
        coreMap = new HashMap<String, Core>();
    }
    
    public Core addCore(String name, Core proxyCore) {
        return coreMap.put(name, proxyCore);
    }
    
    private NodeInfo parseNodeInfo(Core core, Node node) throws SubnetException {
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
        Core proxyCore;
        NodeInfo proxyNode = null;
        EOJ proxyEOJ = null;
        EPC proxyEPC = null;
        
        try {
            Node proxySubnetInfo = null;
            Node proxyNodeInfo = null;
            Node proxyEOJInfo = null;
            Node proxyInstanceInfo = null;
            Node proxyEPCInfo = null;
        
            NodeList nodeList = node.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node proxyInfo = nodeList.item(i);
                String infoName = proxyInfo.getNodeName();
                if (infoName.equals("subnet")) {
                    proxySubnetInfo = proxyInfo;
                } else if (infoName.equals("node")) {
                    proxyNodeInfo = proxyInfo;
                } else if (infoName.equals("eoj")) {
                    proxyEOJInfo = proxyInfo;
                } else if (infoName.equals("instance")) {
                    proxyInstanceInfo = proxyInfo;
                } else if (infoName.equals("epc")) {
                    proxyEPCInfo = proxyInfo;
                }
            }
            
            proxyCore = core;
            if (proxySubnetInfo != null) {
                String subnetName = proxySubnetInfo.getTextContent();
                proxyCore = coreMap.get(subnetName);
         
                if (proxyCore == null) {
                    logger.logp(Level.WARNING, className, "newPropertyDelegate", "invalid subnet: " + subnetName);
                    return null;
                }
            }
            
            if (proxyNodeInfo != null) {
                proxyNode = parseNodeInfo(proxyCore, proxyNodeInfo);
            }
            
            if (proxyInstanceInfo != null) {
                proxyEOJ = parseInstanceInfo(proxyInstanceInfo, ceoj);
            }
            
            if (proxyEOJInfo != null) {
                proxyEOJ = parseEOJInfo(proxyEOJInfo);
            }
            
            if (proxyEPCInfo != null) {
                proxyEPC = parseEPCInfo(proxyEPCInfo);
            }
            
            if (proxyEPC == null) {
                proxyEPC = epc;
            }
        } catch (SubnetException ex) {
            Logger.getLogger(ProxyPropertyDelegateCreator.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        if (proxyEPC == null) {
            proxyEPC = epc;
        }
        
        if (proxyNode == null || proxyEOJ == null || proxyEPC == null) {
            return null;
        }
        
        return new ProxyPropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, proxyCore, proxyNode, proxyEOJ, proxyEPC);
    }
}
