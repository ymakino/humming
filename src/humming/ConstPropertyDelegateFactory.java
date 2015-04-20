package humming;

import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import org.w3c.dom.Node;

/**
 *
 * @author ymakino
 */
public class ConstPropertyDelegateFactory extends PropertyDelegateFactory {
    @Override
    public PropertyDelegate newPropertyDelegate(EPC epc, Node node) {
        String dataStr = node.getTextContent();
        byte[] dataBytes = new byte[dataStr.length()/2];
        
        for (int i=0; i<dataStr.length()-1; i+=2) {
            dataBytes[i/2] = (byte)Integer.parseInt(dataStr.substring(i, i+2), 16);
        }
        
        return new ConstPropertyDelegate(epc, dataBytes);
    }
}
