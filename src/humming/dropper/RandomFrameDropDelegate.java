package humming.dropper;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.Core;
import echowand.service.ExtendedSubnet;
import echowand.service.LocalObjectServiceDelegate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class RandomFrameDropDelegate implements LocalObjectServiceDelegate {
    private static final Logger LOGGER = Logger.getLogger(RandomFrameDropDelegate.class.getName());
    private static final String CLASS_NAME = RandomFrameDropDelegate.class.getName();
    
    private double sendDropRate = 0.0;
    private double receiveDropRate = 0.0;
    
    private boolean defaultEnabled = false;
    private double defaultSendDropRate = 0;
    private double defaultReceiveDropRate = 0;
    
    public void setSendDropRate(double sendDropRate) {
        this.sendDropRate = sendDropRate;
    }
    
    public void setReceiveDropRate(double receiveDropRate) {
        this.receiveDropRate = receiveDropRate;
    }
    
    public void setDropRate(double dropRate) {
        sendDropRate = dropRate;
        receiveDropRate = dropRate;
    }
    
    public void setDropRate(double sendDropRate, double receiveDropRate) {
        this.sendDropRate = sendDropRate;
        this.receiveDropRate = receiveDropRate;
    }
    
    public void setDefaultDropRate(double dropRate) {
        defaultSendDropRate = dropRate;
        defaultReceiveDropRate = dropRate;
        defaultEnabled = true;
    }
    
    public void setDefaultDropRate(double sendDropRate, double receiveDropRate) {
        defaultSendDropRate = sendDropRate;
        defaultReceiveDropRate = receiveDropRate;
        defaultEnabled = true;
    }
    
    public FrameDropSubnet getFrameDropSubnet(Core core) {
        if (core.getSubnet() instanceof ExtendedSubnet) {
            return ((ExtendedSubnet)core.getSubnet()).getSubnet(FrameDropSubnet.class);
        } else {
            return null;
        }
    }

    @Override
    public void notifyCreation(LocalObject object, Core core) {
        
        FrameDropSubnet subnet = getFrameDropSubnet(core);
            
        if (subnet == null) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "notifyCreation", "Cannot enable RandomFrameDropper: there is no FrameDropSubnet");
            return;
        }
            
        RandomFrameDropper frameDropper = new RandomFrameDropper(sendDropRate, receiveDropRate);
        subnet.addSendDropper(new SenderFrameSelector(object.getEOJ()), frameDropper);
        subnet.addReceiveDropper(new ReceiverFrameSelector(object.getEOJ()), frameDropper);
        
        if (defaultEnabled) {
            subnet.setDefaultDropper(new RandomFrameDropper(defaultSendDropRate, defaultReceiveDropRate));
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
