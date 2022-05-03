package humming.sample;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyUpdater;
import humming.HummingException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class SamplePropertyUpdater extends PropertyUpdater {
    private static final Logger LOGGER = Logger.getLogger(SamplePropertyUpdater.class.getName());
    private static final String CLASS_NAME = SamplePropertyUpdater.class.getName();
    
    EPC epc = EPC.xE3;
    private int num = 0;
    
    public SamplePropertyUpdater(HashMap<String, String> params) throws HummingException {
        LOGGER.entering(CLASS_NAME, "SamplePropertyUpdater", new Object[]{params});
        
        parseParams(params);
        
        LOGGER.exiting(CLASS_NAME, "SamplePropertyUpdater");
    }
    
    private void parseParams(HashMap<String, String> params) throws HummingException {
        LOGGER.entering(CLASS_NAME, "parseParams", params);
        
        for (String key : params.keySet()) {
            String value = params.get(key).trim();
            switch (key.toLowerCase()) {
                case "epc":
                    setEPC(EPC.fromByte((byte)Integer.parseInt(value, 16)));
                    break;
                default:
                    throw new HummingException("invalid parameter: " + key + ": " + value);
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "parseParams");
    }
    
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
        LOGGER.logp(Level.INFO, CLASS_NAME, "loop", "set " + localObject + " " + epc + ": " + new ObjectData(b1, b2));
        localObject.forceSetData(epc, new ObjectData(b1, b2));
        num++;
    }
    
}
