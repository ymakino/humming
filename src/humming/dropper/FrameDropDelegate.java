package humming.dropper;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.Core;
import echowand.service.LocalObjectServiceDelegate;

/**
 *
 * @author ymakino
 */
public class FrameDropDelegate implements LocalObjectServiceDelegate {
    private FrameDropSubnet subnet;
    
    private FrameDropper sendDropper;
    private FrameDropper receiveDropper;
    
    public FrameDropDelegate(FrameDropSubnet subnet) {
        this.subnet = subnet;
        sendDropper = null;
        receiveDropper = null;
    }
    
    public void setSendDropper(FrameDropper dropper) {
        sendDropper = dropper;
    }
    
    public void setReceiveDropper(FrameDropper dropper) {
        receiveDropper = dropper;
    }

    @Override
    public void notifyCreation(LocalObject object, Core core) {
        
        if (sendDropper != null) {
            subnet.addSendDropper(new SenderFrameSelector(object.getEOJ()), sendDropper);
        }
        
        if (receiveDropper != null) {
            subnet.addReceiveDropper(new ReceiverFrameSelector(object.getEOJ()), receiveDropper);
        }
    }

    @Override
    public void getData(GetState result, LocalObject object, EPC epc) {
        // do nothing
    }

    @Override
    public void setData(SetState result, LocalObject object, EPC epc, ObjectData newData, ObjectData curData) {
        // do nothing
    }

    @Override
    public void notifyDataChanged(NotifyState result, LocalObject object, EPC epc, ObjectData curData, ObjectData oldData) {
        // do nothing
    }
    
}
