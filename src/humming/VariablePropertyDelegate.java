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
    
    private byte[] data;
    
    public VariablePropertyDelegate(EPC epc, byte[] data) {
        super(epc, true, true, true);
        this.data = Arrays.copyOf(data, data.length);
        logger.logp(Level.INFO, className, "VariablePropertyDelegate", "epc: " + epc + " -> data: " + new ObjectData(data));
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        logger.logp(Level.INFO, className, "getUserData", object + ", EPC: " + epc);
        return new ObjectData(data);
    }

    @Override
    public boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        logger.logp(Level.INFO, className, "setUserData", object + ", EPC: " + epc + ", data: " + data);
        this.data = data.getData().toBytes();
        return true;
    }
}