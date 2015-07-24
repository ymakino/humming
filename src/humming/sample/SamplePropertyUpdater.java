package humming.sample;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyUpdater;

/**
 *
 * @author ymakino
 */
public class SamplePropertyUpdater extends PropertyUpdater {
    EPC epc = EPC.xE2;
    private int num = 0;
    
    public void setEPC(EPC epc) {
        this.epc = epc;
    }
    
    public EPC getEPC() {
        return epc;
    }

    @Override
    public void loop(LocalObject localObject) {
        byte b1 = (byte)(0xff & (num >> 8));
        byte b2 = (byte)(0xff & num);
        localObject.forceSetData(epc, new ObjectData(b1, b2));
        num++;
    }
    
}
