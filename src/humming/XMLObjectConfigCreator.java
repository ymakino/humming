package humming;

import echowand.common.ClassEOJ;
import echowand.common.EPC;
import echowand.info.DeviceObjectInfo;
import echowand.service.LocalObjectConfig;
import echowand.service.PropertyDelegate;
import echowand.util.ConstraintAny;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class XMLObjectConfigCreator {
    private static final Logger logger = Logger.getLogger(XMLObjectConfigCreator.class.getName());
    private static final String className = XMLObjectConfigCreator.class.getName();
    
    private LocalObjectConfig config;
    private DeviceObjectInfo info;
    private LinkedList<PropertyDelegate> delegates;
    
    private void parseClassEOJ(Node ceojNode) {
        ClassEOJ ceoj = new ClassEOJ(ceojNode.getTextContent());
        info.setClassEOJ(ceoj);
        
        logger.logp(Level.INFO, className, "parseClassEOJ", "ClassEOJ: " + ceoj);
    }
    
    private boolean parseProperty(Node propNode) {
        NodeList propInfoList = propNode.getChildNodes();
        EPC epc = null;
        
        Node epcNode = propNode.getAttributes().getNamedItem("epc");
        byte b = (byte)Integer.parseInt(epcNode.getTextContent(), 16);
        epc = EPC.fromByte(b);
        
        boolean getEnabled = true;
        boolean setEnabled = false;
        boolean annoEnabled = false;
        
        Node setNode = propNode.getAttributes().getNamedItem("set");
        if (setNode != null) {
            setEnabled = setNode.getTextContent().equals("enabled");
        }
        
        Node getNode = propNode.getAttributes().getNamedItem("get");
        if (getNode != null) {
            getEnabled = getNode.getTextContent().equals("enabled");
        }
        
        Node annoNode = propNode.getAttributes().getNamedItem("anno");
        if (annoNode != null) {
            annoEnabled = annoNode.getTextContent().equals("enabled");
        }
        
        for (int i=0; i < propInfoList.getLength(); i++) {
            Node propInfo = propInfoList.item(i);
            String infoName = propInfo.getNodeName().toLowerCase();
            
            if (propInfo.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            if (infoName.equals("data")) {
                Node typeNode = propInfo.getAttributes().getNamedItem("type");
                if (typeNode != null) {
                    String typeName = typeNode.getTextContent();
                    PropertyDelegateFactory factory = PropertyDelegateFactory.getInstance();
                    PropertyDelegate delegate = factory.newPropertyDelegate(typeName, epc, propInfo);
                    delegates.add(delegate);
                    
                    logger.logp(Level.INFO, className, "parseProperty", "delegate: " + delegate + ", type: " + typeName + ", ClassEOJ: " + info.getClassEOJ() + ", EPC: " + epc + ", info: " + propInfo);
                } else {
                    logger.logp(Level.WARNING, className, "parseProperty", "no type: " + propInfo);
                }
                
            } else {
                logger.logp(Level.WARNING, className, "parseProperty", "invalid property: " + propInfo);
            }
        }
        
        if (epc == null) {
            return false;
        }
        
        info.add(epc, getEnabled, setEnabled, annoEnabled, 1, new ConstraintAny());
        
        return true;
    }
    
    private void parseDevice(Node deviceNode) {
        
        Node ceojNode = deviceNode.getAttributes().getNamedItem("ceoj");
        parseClassEOJ(ceojNode);
        
        NodeList nodes = deviceNode.getChildNodes();
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String nodeName = node.getNodeName().toLowerCase();
            
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            if (nodeName.equals("property")) {
                parseProperty(node);
            } else {
                logger.logp(Level.WARNING, className, "parseDevice", "invalid node: " + nodeName);
            }
        }
    }

    public XMLObjectConfigCreator(Node node) {
        info = new DeviceObjectInfo();
        delegates = new LinkedList<PropertyDelegate>();
        
        parseDevice(node);
        
        config = new LocalObjectConfig(info);
        
        for (PropertyDelegate delegate : delegates) {
            config.addPropertyDelegate(delegate);
        }
    }
    
    public LocalObjectConfig getConfig() {
        return config;
    }
}