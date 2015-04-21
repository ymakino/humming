package humming;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyDelegate;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ConstPropertyDelegate extends PropertyDelegate {
    private static final Logger logger = Logger.getLogger(ConstPropertyDelegate.class.getName());
    private static final String className = ConstPropertyDelegate.class.getName();
    
    private byte[] data;
    
    public ConstPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, byte[] data) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        this.data = Arrays.copyOf(data, data.length);
        logger.logp(Level.INFO, className, "ConstPropertyDelegate", "epc: " + epc + " -> data: " + new ObjectData(data));
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        return new ObjectData(data);
    }

    @Override
    public boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        return false;
    }
}
