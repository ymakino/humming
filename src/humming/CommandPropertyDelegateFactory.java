package humming;

import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class CommandPropertyDelegateFactory extends PropertyDelegateFactory {
    @Override
    public PropertyDelegate newPropertyDelegate(EPC epc, Node node) {
        String getCommand = null;
        String setCommand = null;
        
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node commandInfo = nodeList.item(i);
            String infoName = commandInfo.getNodeName().toLowerCase();
            if (infoName.equals("get")) {
                getCommand = commandInfo.getTextContent();
            } else if (infoName.equals("set")) {
                setCommand = commandInfo.getTextContent();
            }
        }
        
        if (getCommand != null || setCommand != null) {
            return new CommandPropertyDelegate(epc, getCommand, setCommand);
        }
        
        return null;
    }
}