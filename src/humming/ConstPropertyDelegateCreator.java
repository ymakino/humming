package humming;

import echowand.common.ClassEOJ;
import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;

/**
 *
 * @author ymakino
 */
public class ConstPropertyDelegateCreator implements PropertyDelegateCreator {
    private static final Logger LOGGER = Logger.getLogger(ConstPropertyDelegateCreator.class.getName());
    private static final String CLASS_NAME = ConstPropertyDelegateCreator.class.getName();
    
    @Override
    public PropertyDelegate newPropertyDelegate(ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) throws HummingException {
        String dataStr = node.getTextContent().trim();
        byte[] dataBytes = new byte[dataStr.length()/2];
        
        try {
            for (int i=0; i<dataStr.length()-1; i+=2) {
                dataBytes[i/2] = (byte)Integer.parseInt(dataStr.substring(i, i+2), 16);
            }
        } catch (NumberFormatException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "invalid data: " + dataStr);
            throw new HummingException("failed" + ex);
        }
        
        return new ConstPropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, dataBytes);
    }
}
