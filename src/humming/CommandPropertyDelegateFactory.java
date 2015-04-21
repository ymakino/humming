package humming;

import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class CommandPropertyDelegateFactory extends PropertyDelegateFactory {
    private static final Logger logger = Logger.getLogger(CommandPropertyDelegateFactory.class.getName());
    private static final String className = CommandPropertyDelegateFactory.class.getName();
    
    @Override
    public PropertyDelegate newPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) {
        String getCommand = null;
        String setCommand = null;
        
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node commandInfo = nodeList.item(i);
            String infoName = commandInfo.getNodeName();
            if (infoName.equals("get")) {
                getCommand = commandInfo.getTextContent();
            } else if (infoName.equals("set")) {
                setCommand = commandInfo.getTextContent();
            }
        }
        
        if (getCommand != null || setCommand != null) {
            return new CommandPropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, getCommand, setCommand);
        }
        
        return null;
    }
}