package humming;

import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import org.w3c.dom.Node;

/**
 *
 * @author ymakino
 */
public class FilePropertyDelegateFactory extends PropertyDelegateFactory {
    @Override
    public PropertyDelegate newPropertyDelegate(EPC epc, Node node) {
        return new FilePropertyDelegate(epc, node.getTextContent());
    }
}
