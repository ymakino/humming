package humming;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyDelegate;
import java.util.Arrays;

/**
 *
 * @author ymakino
 */
public class ConstPropertyDelegate extends PropertyDelegate {
    private byte[] data;
    
    public ConstPropertyDelegate(EPC epc, byte[] data) {
        super(epc, true, false, false);
        this.data = Arrays.copyOf(data, data.length);
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
