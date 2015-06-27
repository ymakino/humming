package humming.sample;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyDelegate;

/**
 *
 * @author ymakino
 */
public class SamplePropertyDelegate extends PropertyDelegate {

    public SamplePropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        System.out.println("SamplePropertyDelegate.getUserData("+object + ", " + epc + ")");
        return new ObjectData((byte)0x12, (byte)0x34);
    }
}
