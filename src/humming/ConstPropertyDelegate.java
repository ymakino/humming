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
    private static final Logger LOGGER = Logger.getLogger(ConstPropertyDelegate.class.getName());
    private static final String CLASS_NAME = ConstPropertyDelegate.class.getName();
    
    private byte[] value;
    
    public ConstPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, byte[] value) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        this.value = Arrays.copyOf(value, value.length);
        LOGGER.logp(Level.INFO, CLASS_NAME, "ConstPropertyDelegate", "epc: " + epc + " -> data: " + new ObjectData(value));
    }
    
    public byte[] getValue() {
        return Arrays.copyOf(value, value.length);
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        ObjectData data = new ObjectData(value);
        LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", object + ", EPC: " + epc + ", data: " + data);
        return data;
    }

    @Override
    public boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        return false;
    }
}
