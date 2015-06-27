package humming;

import echowand.common.ClassEOJ;
import echowand.common.Data;
import echowand.common.EPC;
import echowand.info.DeviceObjectInfo;
import echowand.service.LocalObjectConfig;
import echowand.service.PropertyDelegate;
import echowand.service.PropertyUpdater;
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
    private static final Logger LOGGER = Logger.getLogger(LocalObjectConfigCreator.class.getName());
    private static final String CLASS_NAME = LocalObjectConfigCreator.class.getName();
    
    private PropertyDelegateFactory delegateFactory;
    private LocalObjectConfig config;
    private DeviceObjectInfo info;
    private LinkedList<PropertyDelegate> delegates;
    private LinkedList<PropertyUpdater> updaters;
    
    private void parseClassEOJ(Node ceojNode) {
        ClassEOJ ceoj = new ClassEOJ(ceojNode.getTextContent());
        info.setClassEOJ(ceoj);
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "parseClassEOJ", "ClassEOJ: " + ceoj);
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
        byte[] dataBytes = new byte[]{0x00};
        boolean dataSizeFixed = false;
        
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
        
        Node valueNode = propNode.getAttributes().getNamedItem("value");
        if (valueNode != null) {
            String valueStr = valueNode.getTextContent();
            
            if (valueStr.length() == 0 || (valueStr.length() % 2) != 0) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "invalid value: " + valueStr);
            }
            
            dataBytes = new byte[valueStr.length() / 2];
            dataSizeFixed = true;
            
            for (int i=0; i<valueStr.length(); i+=2) {
                String numStr = valueStr.substring(i, i+2);
                int num = Integer.parseInt(numStr, 16);
                dataBytes[i/2] = (byte)num;
            }
        }
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "parseProperty", "property: ClassEOJ: " + info.getClassEOJ() + ", EPC: " + epc + ", GET: " + getEnabled + ", SET: " + setEnabled + ", Notify: " + notifyEnabled + ", data: " + new Data(dataBytes));
        
        for (int i=0; i < propInfoList.getLength(); i++) {
            Node propInfo = propInfoList.item(i);
            String infoName = propInfo.getNodeName();
            
            if (propInfo.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            if (!infoName.equals("data")) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "invalid property: " + toNodeString(propInfo, true));
                continue;
            }
            
            Node typeNode = propInfo.getAttributes().getNamedItem("type");
            if (typeNode != null) {
                String typeName = typeNode.getTextContent();

                PropertyDelegate delegate = delegateFactory.newPropertyDelegate(typeName, info.getClassEOJ(), epc, getEnabled, setEnabled, notifyEnabled, propInfo);
                if (delegate != null) {
                    delegates.add(delegate);
                    LOGGER.logp(Level.INFO, CLASS_NAME, "parseProperty", "delegate: " + delegate + ", type: " + typeName + ", ClassEOJ: " + info.getClassEOJ() + ", EPC: " + epc + ", GET: " + getEnabled + ", SET: " + setEnabled + ", Notify: " + notifyEnabled + ", info: " + toInfoString(propInfo));
                }
            } else {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "invalid property: " + toInfoString(propInfo));
            }
        }
        
        if (epc == null) {
            return false;
        }
        
        if (dataSizeFixed) {
            info.add(epc, getEnabled, setEnabled, notifyEnabled, dataBytes);
        } else {
            info.add(epc, getEnabled, setEnabled, notifyEnabled, dataBytes, new ConstraintAny());
        }
        
        return true;
    }
    
    private boolean parseUpdater(Node propNode) throws HummingException {
        String updaterName = propNode.getTextContent().trim();
        Node intervalNode = propNode.getAttributes().getNamedItem("interval");
        
        try {
            Class<?> cls = Class.forName(updaterName);
            PropertyUpdater propertyUpdater = (PropertyUpdater)cls.newInstance();
            
            if (intervalNode != null ) {
                int interval = Integer.parseInt(intervalNode.getTextContent());
                propertyUpdater.setIntervalPeriod(interval);
                LOGGER.logp(Level.INFO, CLASS_NAME, "parseUpdater", "class: " +  updaterName + ", interval: " + interval);
            } else {
                LOGGER.logp(Level.INFO, CLASS_NAME, "parseUpdater", "class: " +  updaterName);
            }
            
            updaters.add(propertyUpdater);
            return true;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LocalObjectConfigCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(LocalObjectConfigCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(LocalObjectConfigCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NumberFormatException ex) {
            Logger.getLogger(LocalObjectConfigCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
    
    private void parseObject(Node objectNode) throws HummingException {
        Node ceojNode = objectNode.getAttributes().getNamedItem("ceoj");
        
        if (ceojNode == null) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "parseObject", "invalid ClassEOJ: " + ceojNode);
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
            } else if (nodeName.equals("updater")) {
                parseUpdater(node);
            } else {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "parseObject", "invalid XML node: " + nodeName);
            }
        }
    }

    public LocalObjectConfigCreator(Node objectNode, PropertyDelegateFactory factory) throws HummingException {
        delegateFactory = factory;
        
        info = new DeviceObjectInfo();
        delegates = new LinkedList<PropertyDelegate>();
        updaters = new LinkedList<PropertyUpdater>();
        
        parseObject(objectNode);
        
        config = new LocalObjectConfig(info);
        
        for (PropertyDelegate delegate : delegates) {
            config.addPropertyDelegate(delegate);
        }
        
        for (PropertyUpdater updater : updaters) {
            config.addPropertyUpdater(updater);
        }
    }
    
    public LocalObjectConfig getConfig() {
        return config;
    }
}