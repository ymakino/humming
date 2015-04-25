package humming;

import echowand.common.ClassEOJ;
import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.util.logging.Logger;
import org.w3c.dom.Node;

/**
 *
 * @author ymakino
 */
public class FilePropertyDelegateCreator implements PropertyDelegateCreator {
    private static final Logger LOGGER = Logger.getLogger(FilePropertyDelegateCreator.class.getName());
    private static final String CLASS_NAME = FilePropertyDelegateCreator.class.getName();
    
    @Override
    public PropertyDelegate newPropertyDelegate(ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) {
        return new FilePropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, node.getTextContent());
    }
}
