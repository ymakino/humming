package humming;

import echowand.common.ClassEOJ;
import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.net.NodeInfo;
import echowand.net.SubnetException;
import echowand.service.Core;
import echowand.service.PropertyDelegate;
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
    
    private Humming humming;
    
    public ProxyPropertyDelegateCreator(Humming humming) {
        this.humming = humming;
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
        Core remoteCore;
        NodeInfo remoteNode = null;
        EOJ remoteEOJ = null;
        EPC remoteEPC = null;
        
        try {
            Node remoteSubnetInfo = null;
            Node remoteNodeInfo = null;
            Node remoteEOJInfo = null;
            Node remoteInstanceInfo = null;
            Node remoteEPCInfo = null;
        
            NodeList nodeList = node.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node remoteInfo = nodeList.item(i);
            
                if (remoteInfo.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
            
                String infoName = remoteInfo.getNodeName();
                if (infoName.equals(SUBNET_TAG)) {
                    remoteSubnetInfo = remoteInfo;
                } else if (infoName.equals(NODE_TAG)) {
                    remoteNodeInfo = remoteInfo;
                } else if (infoName.equals(EOJ_TAG)) {
                    remoteEOJInfo = remoteInfo;
                } else if (infoName.equals(INSTANCE_TAG)) {
                    remoteInstanceInfo = remoteInfo;
                } else if (infoName.equals(EPC_TAG)) {
                    remoteEPCInfo = remoteInfo;
                } else {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "newPropertyDelegate", "invalid element: " + infoName);
                }
            }
            
            remoteCore = humming.getCore();
            if (remoteSubnetInfo != null) {
                String subnetName = remoteSubnetInfo.getTextContent();
                remoteCore = humming.getCore(subnetName);
         
                if (remoteCore == null) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "newPropertyDelegate", "invalid subnet: " + subnetName);
                    throw new HummingException("invalid subnet: " + subnetName);
                }
            }
            
            if (remoteNodeInfo != null) {
                remoteNode = parseNodeInfo(remoteCore, remoteNodeInfo);
            }
            
            if (remoteInstanceInfo != null) {
                remoteEOJ = parseInstanceInfo(remoteInstanceInfo, ceoj);
            }
            
            if (remoteEOJInfo != null) {
                remoteEOJ = parseEOJInfo(remoteEOJInfo);
            }
            
            if (remoteEPCInfo != null) {
                remoteEPC = parseEPCInfo(remoteEPCInfo);
            }
            
            if (remoteEPC == null) {
                remoteEPC = epc;
            }
        } catch (SubnetException ex) {
            Logger.getLogger(ProxyPropertyDelegateCreator.class.getName()).log(Level.SEVERE, null, ex);
            throw new HummingException("failed", ex);
        }
        
        if (remoteEPC == null) {
            remoteEPC = epc;
        }
        
        if (remoteNode == null || remoteEOJ == null || remoteEPC == null) {
            String errorMessage = "invalid remote information: Node: " + remoteNode + " EOJ: " + remoteEOJ + " EPC: " + remoteEPC;
            LOGGER.logp(Level.WARNING, CLASS_NAME, "newPropertyDelegate", errorMessage);
            throw new HummingException(errorMessage);
        }
        
        return new ProxyPropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, remoteCore, remoteNode, remoteEOJ, remoteEPC);
    }
}
