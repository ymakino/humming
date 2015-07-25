package humming.sample;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.LocalObjectServiceDelegate;

/**
 *
 * @author ymakino
 */
public class SampleObjectDelegate extends LocalObjectServiceDelegate {
    private EPC epc = EPC.xE0;
    private int value;
    
    public void setEPC(EPC epc) {
        this.epc = epc;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    @Override
    public void notifyCreated(LocalObject object){
        System.out.println("notifyCreated: " + this + ", " + object);
    }

    @Override
    public void getData(GetState result, LocalObject object, EPC epc){
        if (this.epc != epc) {
            return;
        }
        
        System.out.println("getData: " + this + ", " + result + ", " + object + ", " + epc);
        
        if (this.epc == epc) {
            byte b1 = (byte)((0xff00 & value) >> 8);
            byte b2 = (byte)(0x00ff & value);
            result.setGetData(new ObjectData(b1, b2));
            result.setDone();
        }
    }

    @Override
    public void setData(SetState result, LocalObject object, EPC epc, ObjectData newData, ObjectData curData){
        if (this.epc != epc) {
            return;
        }
        
        System.out.println("setData: " + this + ", " + result + ", " + object + ", " + epc + ", " + newData + ", " + curData);
        
        value = (0xff00 & (newData.get(0) << 8)) | (0x00ff & newData.get(1));
    }

    @Override
    public void notifyDataChanged(NotifyState result, LocalObject object, EPC epc, ObjectData curData, ObjectData oldData){
        if (this.epc != epc) {
            return;
        }
        
        System.out.println("notifyDataChanged: " + this + ", " + result + ", " + object + ", " + epc + ", " + curData + ", " + oldData);
    }
}
