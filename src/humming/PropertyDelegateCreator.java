package humming;

import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import org.w3c.dom.Node;

/**
 *
 * @author ymakino
 */
public interface PropertyDelegateCreator {
    public PropertyDelegate newPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) throws HummingException;
}
