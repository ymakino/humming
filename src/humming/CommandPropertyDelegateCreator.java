package humming;

import echowand.common.ClassEOJ;
import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class CommandPropertyDelegateCreator implements PropertyDelegateCreator {
    private static final Logger logger = Logger.getLogger(CommandPropertyDelegateCreator.class.getName());
    private static final String className = CommandPropertyDelegateCreator.class.getName();
    
    @Override
    public PropertyDelegate newPropertyDelegate(ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) {
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
            } else {
                logger.logp(Level.WARNING, className, "parseProperty", "invalid tag: " + infoName);
            }
        }
        
        if (getCommand != null || setCommand != null) {
            return new CommandPropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, getCommand, setCommand);
        } else {
            logger.logp(Level.WARNING, className, "parseProperty", "no command");
        }
        
        return null;
    }
}