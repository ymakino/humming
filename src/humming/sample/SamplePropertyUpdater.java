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
    private int num = 0;

    @Override
    public void loop(LocalObject localObject) {
        byte b1 = (byte)(0xff & (num >> 8));
        byte b2 = (byte)(0xff & num);
        localObject.forceSetData(EPC.xE1, new ObjectData(b1, b2));
        num++;
    }
    
}
