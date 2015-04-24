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
public class VariablePropertyDelegate extends PropertyDelegate {
    private static final Logger logger = Logger.getLogger(VariablePropertyDelegate.class.getName());
    private static final String className = VariablePropertyDelegate.class.getName();
    
    private byte[] value;
    
    public VariablePropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, byte[] data) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        this.value = Arrays.copyOf(data, data.length);
        logger.logp(Level.INFO, className, "VariablePropertyDelegate", "epc: " + epc + " -> data: " + new ObjectData(data));
    }
    
    public byte[] getValue() {
        return Arrays.copyOf(value, value.length);
    }
    
    public void setValue(byte[] newData) {
        byte[] oldData = value;
        value = Arrays.copyOf(newData, newData.length);
        getLocalObject().notifyDataChanged(EPC.x80, new ObjectData(newData), new ObjectData(oldData));
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        ObjectData data = new ObjectData(value);
        logger.logp(Level.INFO, className, "getUserData", object + ", EPC: " + epc + ", data: " + data);
        return data;
    }

    @Override
    public boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        value = data.getData().toBytes();
        logger.logp(Level.INFO, className, "setUserData", object + ", EPC: " + epc + ", data: " + data);
        return true;
    }
}