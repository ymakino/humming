package humming;

import echowand.common.ClassEOJ;
import echowand.common.EPC;
import echowand.info.DeviceObjectInfo;
import echowand.service.LocalObjectConfig;
import echowand.service.PropertyDelegate;
import java.util.LinkedList;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class XMLObjectConfigCreator {
    private LocalObjectConfig config;
    private DeviceObjectInfo info;
    private LinkedList<PropertyDelegate> delegates;
    
    private void parseClassEOJ(Node ceojNode) {
        ClassEOJ ceoj = new ClassEOJ(ceojNode.getTextContent());
        info.setClassEOJ(ceoj);
    }
    
    private boolean parseProperty(Node propNode) {
        NodeList propInfoList = propNode.getChildNodes();
        EPC epc = null;
        
        for (int i=0; i < propInfoList.getLength(); i++) {
            Node propInfo = propInfoList.item(i);
            String infoName = propInfo.getNodeName().toLowerCase();                    
            
            if (infoName.equals("epc")) {
                byte b = (byte)Integer.parseInt(propInfo.getTextContent(), 16);
                epc = EPC.fromByte(b);
            } else if (infoName.equals("data")) {
                Node typeNode = propInfo.getAttributes().getNamedItem("type");
                if (typeNode != null) {
                    String typeName = typeNode.getTextContent();
                    PropertyDelegateFactory factory = PropertyDelegateFactory.getInstance();
                    PropertyDelegate delegate = factory.newPropertyDelegate(typeName, epc, propInfo);
                    delegates.add(delegate);
                }
                System.out.println(typeNode);
            }
        }
        
        if (epc == null) {
            return false;
        }
        
        info.add(epc, true, false, false, 1);
        
        return true;
    }
    
    private void parseDevice(Node deviceNode) {
        
        NodeList nodes = deviceNode.getChildNodes();
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String nodeName = node.getNodeName().toLowerCase();
            if (nodeName.equals("ceoj")) {
                parseClassEOJ(node);
            } else if (nodeName.equals("property")) {
                parseProperty(node);
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