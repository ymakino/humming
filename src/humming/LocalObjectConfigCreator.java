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
public class LocalObjectConfigCreator {
    private static final Logger logger = Logger.getLogger(LocalObjectConfigCreator.class.getName());
    private static final String className = LocalObjectConfigCreator.class.getName();
    
    private PropertyDelegateFactory delegateFactory;
    private LocalObjectConfig config;
    private DeviceObjectInfo info;
    private LinkedList<PropertyDelegate> delegates;
    
    private void parseClassEOJ(Node ceojNode) {
        ClassEOJ ceoj = new ClassEOJ(ceojNode.getTextContent());
        info.setClassEOJ(ceoj);
        
        logger.logp(Level.INFO, className, "parseClassEOJ", "ClassEOJ: " + ceoj);
    }
    
    private String toNodeString(Node node, boolean enableAttributes) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            return node.getTextContent().trim();
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append("<" + node.getNodeName());
        
        if (enableAttributes) {
            for (int i=0; i<node.getAttributes().getLength(); i++) {
                Node attr = node.getAttributes().item(i);
                builder.append(" " + attr.getNodeName() + "=\"" + attr.getTextContent() + "\"");
            }
        }
        
        StringBuilder childrenBuilder = new StringBuilder();
        for (int i=0; i<node.getChildNodes().getLength(); i++) {
            Node child = node.getChildNodes().item(i);
            childrenBuilder.append(toNodeString(child, true));
        }
        
        if (childrenBuilder.length() == 0) {
            builder.append("/>");
        } else {
            builder.append(">");
            builder.append(childrenBuilder);
            builder.append("</" + node.getNodeName() + ">");
        }
        
        return builder.toString();
    }
    
    private String toInfoString(Node node) {
        if (!node.getNodeName().equals("data")) {
            return toNodeString(node, true);
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        
        for (int i=0; i<node.getChildNodes().getLength(); i++) {
            Node child = node.getChildNodes().item(i);
            builder.append(toNodeString(child, true));
        }
        
        builder.append("]");
        return builder.toString();
    }
    
    private boolean parseProperty(Node propNode) throws HummingException {
        NodeList propInfoList = propNode.getChildNodes();
        EPC epc = null;
        
        Node epcNode = propNode.getAttributes().getNamedItem("epc");
        String epcStr = epcNode.getTextContent().toLowerCase();
        if (epcStr.startsWith("0x")) {
            epcStr = epcStr.substring(2);
        }
        byte b = (byte)Integer.parseInt(epcStr, 16);
        epc = EPC.fromByte(b);
        
        boolean getEnabled = true;
        boolean setEnabled = false;
        boolean notifyEnabled = false;
        
        Node setNode = propNode.getAttributes().getNamedItem("set");
        if (setNode != null) {
            setEnabled = setNode.getTextContent().equals("enabled");
        }
        
        Node getNode = propNode.getAttributes().getNamedItem("get");
        if (getNode != null) {
            getEnabled = getNode.getTextContent().equals("enabled");
        }
        
        Node notifyNode = propNode.getAttributes().getNamedItem("notify");
        if (notifyNode != null) {
            notifyEnabled = notifyNode.getTextContent().equals("enabled");
        }
        
        for (int i=0; i < propInfoList.getLength(); i++) {
            Node propInfo = propInfoList.item(i);
            String infoName = propInfo.getNodeName();
            
            if (propInfo.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            if (!infoName.equals("data")) {
                logger.logp(Level.WARNING, className, "parseProperty", "invalid property: " + toNodeString(propInfo, true));
                continue;
            }
            
            Node typeNode = propInfo.getAttributes().getNamedItem("type");
            if (typeNode != null) {
                String typeName = typeNode.getTextContent();

                PropertyDelegate delegate = delegateFactory.newPropertyDelegate(typeName, epc, getEnabled, setEnabled, notifyEnabled, propInfo);
                delegates.add(delegate);
                logger.logp(Level.INFO, className, "parseProperty", "delegate: " + delegate + ", type: " + typeName + ", ClassEOJ: " + info.getClassEOJ() + ", EPC: " + epc + ", GET: " + getEnabled + ", SET: " + setEnabled + ", Notify: " + notifyEnabled + ", info: " + toInfoString(propInfo));

            } else {
                logger.logp(Level.WARNING, className, "parseProperty", "invalid property: " + toInfoString(propInfo));
            }
        }
        
        if (epc == null) {
            return false;
        }
        
        info.add(epc, getEnabled, setEnabled, notifyEnabled, 1, new ConstraintAny());
        
        return true;
    }
    
    private void parseObject(Node objectNode) throws HummingException {
        Node ceojNode = objectNode.getAttributes().getNamedItem("ceoj");
        
        if (ceojNode == null) {
            logger.logp(Level.WARNING, className, "parseObject", "invalid ClassEOJ: " + ceojNode);
            throw new HummingException("invalid ClassEOJ: " + ceojNode);
        }
        
        parseClassEOJ(ceojNode);
        
        NodeList nodes = objectNode.getChildNodes();
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String nodeName = node.getNodeName();
            
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            if (nodeName.equals("property")) {
                parseProperty(node);
            } else {
                logger.logp(Level.WARNING, className, "parseObject", "invalid property: " + nodeName);
            }
        }
    }

    public LocalObjectConfigCreator(Node objectNode, PropertyDelegateFactory factory) throws HummingException {
        delegateFactory = factory;
        
        info = new DeviceObjectInfo();
        delegates = new LinkedList<PropertyDelegate>();
        
        parseObject(objectNode);
        
        config = new LocalObjectConfig(info);
        
        for (PropertyDelegate delegate : delegates) {
            config.addPropertyDelegate(delegate);
        }
    }
    
    public LocalObjectConfig getConfig() {
        return config;
    }
}