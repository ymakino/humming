package humming.sample;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.Core;
import echowand.service.LocalObjectServiceDelegate;
import humming.HummingException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class SampleObjectDelegate implements LocalObjectServiceDelegate {
    private static final Logger LOGGER = Logger.getLogger(SampleObjectDelegate.class.getName());
    private static final String CLASS_NAME = SampleObjectDelegate.class.getName();
    
    private EPC epc = EPC.xE0;
    private int value;
    
    public SampleObjectDelegate(HashMap<String, String> params) throws HummingException {
        LOGGER.entering(CLASS_NAME, "SampleObjectDelegate", new Object[]{params});
        
        parseParams(params);
        
        LOGGER.exiting(CLASS_NAME, "SampleObjectDelegate");
    }
    
    private void parseParams(HashMap<String, String> params) throws HummingException {
        LOGGER.entering(CLASS_NAME, "parseParams", params);
        
        for (String key : params.keySet()) {
            String value = params.get(key).trim();
            switch (key.toLowerCase()) {
                case "epc":
                    setEPC(EPC.fromByte((byte)Integer.parseInt(value, 16)));
                    break;
                case "value":
                    setValue(Integer.parseInt(value));
                    break;
                default:
                    throw new HummingException("invalid parameter: " + key + ": " + value);
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "parseParams");
    }
    
    public void setEPC(EPC epc) {
        LOGGER.entering(CLASS_NAME, "setEPC", epc);
        
        this.epc = epc;
        
        LOGGER.exiting(CLASS_NAME, "setEPC");
    }
    
    public void setValue(int value) {
        LOGGER.entering(CLASS_NAME, "setValue", value);
        
        this.value = value;
        
        LOGGER.exiting(CLASS_NAME, "setValue");
    }
    
    @Override
    public void notifyCreation(LocalObject object, Core core){
        LOGGER.entering(CLASS_NAME, "notifyCreation", new Object[]{object, core});
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "notifyCreation", object + ", " + core);
        
        LOGGER.exiting(CLASS_NAME, "notifyCreation");
    }

    @Override
    public void getData(GetState result, LocalObject object, EPC epc){
        LOGGER.entering(CLASS_NAME, "getData", new Object[]{result, object, epc});
        
        if (this.epc != epc) {
            LOGGER.exiting(CLASS_NAME, "getData");
            return;
        }
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "getData", result + ", " + object + ", " + epc);
        
        if (this.epc == epc) {
            byte b1 = (byte)((0xff00 & value) >> 8);
            byte b2 = (byte)(0x00ff & value);
            result.setGetData(new ObjectData(b1, b2));
            result.setDone();
        }
        
        LOGGER.exiting(CLASS_NAME, "getData");
    }

    @Override
    public void setData(SetState result, LocalObject object, EPC epc, ObjectData newData, ObjectData curData){
        LOGGER.entering(CLASS_NAME, "setData", new Object[]{result, object, epc, newData, curData});
        
        if (this.epc != epc) {
            LOGGER.exiting(CLASS_NAME, "setData");
            return;
        }
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "setData", result + ", " + object + ", " + epc + ", " + newData + ", " + curData);
        
        value = (0xff00 & (newData.get(0) << 8)) | (0x00ff & newData.get(1));
        
        LOGGER.exiting(CLASS_NAME, "setData");
    }

    @Override
    public void notifyDataChanged(NotifyState result, LocalObject object, EPC epc, ObjectData curData, ObjectData oldData){
        LOGGER.entering(CLASS_NAME, "notifyDataChanged", new Object[]{result, object, epc, curData, oldData});
        
        if (this.epc != epc) {
            LOGGER.exiting(CLASS_NAME, "notifyDataChanged");
            return;
        }
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "notifyDataChanged", result + ", " + object + ", " + epc + ", " + curData + ", " + oldData);
    }
}
