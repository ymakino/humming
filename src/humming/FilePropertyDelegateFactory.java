package humming;

import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.util.logging.Logger;
import org.w3c.dom.Node;

/**
 *
 * @author ymakino
 */
public class FilePropertyDelegateFactory extends PropertyDelegateFactory {
    private static final Logger logger = Logger.getLogger(FilePropertyDelegateFactory.class.getName());
    private static final String className = FilePropertyDelegateFactory.class.getName();
    
    @Override
    public PropertyDelegate newPropertyDelegate(EPC epc, Node node) {
        return new FilePropertyDelegate(epc, node.getTextContent());
    }
}
