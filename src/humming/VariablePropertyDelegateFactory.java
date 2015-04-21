package humming;

import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.util.logging.Logger;
import org.w3c.dom.Node;

/**
 *
 * @author ymakino
 */
public class VariablePropertyDelegateFactory extends PropertyDelegateFactory {
    private static final Logger logger = Logger.getLogger(VariablePropertyDelegateFactory.class.getName());
    private static final String className = VariablePropertyDelegateFactory.class.getName();
    
    @Override
    public PropertyDelegate newPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) {
        String dataStr = node.getTextContent();
        byte[] dataBytes = new byte[dataStr.length()/2];
        
        for (int i=0; i<dataStr.length()-1; i+=2) {
            dataBytes[i/2] = (byte)Integer.parseInt(dataStr.substring(i, i+2), 16);
        }
        
        return new VariablePropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, dataBytes);
    }
}
