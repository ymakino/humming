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
    private static final Logger LOGGER = Logger.getLogger(ProxyPropertyDelegateCreator.class.getName());
    private static final String CLASS_NAME = ProxyPropertyDelegateCreator.class.getName();
    
    public static final String SUBNET_TAG = "subnet";
    public static final String NODE_TAG = "node";
    public static final String EOJ_TAG = "eoj";
    public static final String EPC_TAG = "epc";
    public static final String INSTANCE_TAG = "instance";
    
    private Core defaultCore;
    private HashMap<String, Core> coreMap;
    
    public ProxyPropertyDelegateCreator(Core core) {
        defaultCore = core;
        coreMap = new HashMap<String, Core>();
    }
    
    public Core addCore(String name, Core core) {
        return coreMap.put(name, core);
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
    public PropertyDelegate newPropertyDelegate(ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) throws HummingException {
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
            
                if (proxyInfo.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
            
                String infoName = proxyInfo.getNodeName();
                if (infoName.equals(SUBNET_TAG)) {
                    proxySubnetInfo = proxyInfo;
                } else if (infoName.equals(NODE_TAG)) {
                    proxyNodeInfo = proxyInfo;
                } else if (infoName.equals(EOJ_TAG)) {
                    proxyEOJInfo = proxyInfo;
                } else if (infoName.equals(INSTANCE_TAG)) {
                    proxyInstanceInfo = proxyInfo;
                } else if (infoName.equals(EPC_TAG)) {
                    proxyEPCInfo = proxyInfo;
                } else {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "newPropertyDelegate", "invalid element: " + infoName);
                }
            }
            
            proxyCore = defaultCore;
            if (proxySubnetInfo != null) {
                String subnetName = proxySubnetInfo.getTextContent();
                proxyCore = coreMap.get(subnetName);
         
                if (proxyCore == null) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "newPropertyDelegate", "invalid subnet: " + subnetName);
                    throw new HummingException("invalid subnet: " + subnetName);
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
            throw new HummingException("failed", ex);
        }
        
        if (proxyEPC == null) {
            proxyEPC = epc;
        }
        
        if (proxyNode == null || proxyEOJ == null || proxyEPC == null) {
            String errorMessage = "invalid proxy information: Node: " + proxyNode + " EOJ: " + proxyEOJ + " EPC: " + proxyEPC;
            LOGGER.logp(Level.WARNING, CLASS_NAME, "newPropertyDelegate", errorMessage);
            throw new HummingException(errorMessage);
        }
        
        return new ProxyPropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, proxyCore, proxyNode, proxyEOJ, proxyEPC);
    }
}
