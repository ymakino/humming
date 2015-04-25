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
    private static final Logger LOGGER = Logger.getLogger(CommandPropertyDelegateCreator.class.getName());
    private static final String CLASS_NAME = CommandPropertyDelegateCreator.class.getName();
    
    public static final String GET_TAG = "get";
    public static final String SET_TAG = "set";
    
    @Override
    public PropertyDelegate newPropertyDelegate(ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) {
        String getCommand = null;
        String setCommand = null;
        
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node commandInfo = nodeList.item(i);
            String infoName = commandInfo.getNodeName();
            if (infoName.equals(GET_TAG)) {
                getCommand = commandInfo.getTextContent();
            } else if (infoName.equals(SET_TAG)) {
                setCommand = commandInfo.getTextContent();
            } else {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "invalid tag: " + infoName);
            }
        }
        
        if (getCommand != null || setCommand != null) {
            return new CommandPropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, getCommand, setCommand);
        } else {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "no command");
        }
        
        return null;
    }
}