package humming.sample;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyDelegate;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class GPIOPinDelegate extends PropertyDelegate {
    private static final Logger LOGGER = Logger.getLogger(GPIOPinDelegate.class.getName());
    private static final String CLASS_NAME = GPIOPinDelegate.class.getName();
    
    private GPIOPin pin;
    private Timer timer = null;
    private TimerTask updateTask = null;
    private int delay = 1000;
    private int interval = 1000;

    public GPIOPinDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        LOGGER.entering(CLASS_NAME, "GPIOPinDelegate", new Object[]{epc, getEnabled, setEnabled, notifyEnabled});
        
        LOGGER.exiting(CLASS_NAME, "GPIOPinDelegate");
    }
    
    public void setPinNumber(int pinNumber) {
        LOGGER.entering(CLASS_NAME, "setPinNumber", pinNumber);
        
        pin = new GPIOPin(pinNumber);
        
        if (!pin.isExported()) {
            if (!pin.export()) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "setPinNumber", "Cannot export pin: " + pinNumber);
                LOGGER.exiting(CLASS_NAME, "setPinNumber");
                return;
            }
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.logp(Level.INFO, CLASS_NAME, "setPinNumber", "Set pin \"in\": " + pin.getPinNumber());
                if (!pin.setInput()) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "setPinNumber", "Cannot set pin \"in\": " + pin.getPinNumber());
                }
                
                LOGGER.logp(Level.INFO, CLASS_NAME, "setPinNumber", "Unexport pin: " + pin.getPinNumber());
                if (!pin.unexport()) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "setPinNumber", "Cannot unexport pin: " + pin.getPinNumber());
                }
            }
        });
        
        if (isSetEnabled()) {
            if (!pin.setOutput()) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "setPinNumber", "Cannot set pin \"out\": " + pin.getPinNumber());
            }
        } else {
            if (!pin.setInput()) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "setPinNumber", "Cannot set pin \"in\": " + pin.getPinNumber());
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "setPinNumber");
    }
    
    public void setDelay(int delay) {
        LOGGER.entering(CLASS_NAME, "setDelay", delay);
        
        this.delay = delay;
        
        LOGGER.exiting(CLASS_NAME, "setDelay");
    }
    
    public void setInterval(int interval) {
        LOGGER.entering(CLASS_NAME, "setInterval", interval);
        
        this.interval = interval;
        
        LOGGER.exiting(CLASS_NAME, "setInterval");
    }
    
    public void setNegative(boolean negative) {
        LOGGER.entering(CLASS_NAME, "setNegative", negative);
        
        pin.setNegative(negative);
        
        LOGGER.exiting(CLASS_NAME, "setNegative");
    }
    
    private int lastValue;
    
    public synchronized void notifyPinStatus(LocalObject object) {
        // LOGGER.entering(CLASS_NAME, "notifyPinStatus", object);
        
        int value = pin.getValue();
        for (;;) {
            if (lastValue == value) {
                break;
            }

            LOGGER.logp(Level.INFO, CLASS_NAME, "notifyPinStatus", "notify LocalObject: " + object + ", GPIO: " + pin.getPinNumber() + ", value: " + value);
            
            EPC epc = GPIOPinDelegate.this.getEPC();
            ObjectData data = object.getData(epc);
            object.notifyDataChanged(epc, data, null);
            lastValue = value;

            value = pin.getValue();
        }
        
        // LOGGER.exiting(CLASS_NAME, "notifyPinStatus");
    }
    
    @Override
    public void notifyCreation(final LocalObject object) {
        LOGGER.entering(CLASS_NAME, "notifyCreation", object);
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "notifyCreation", "created LocalObject: " + object + ", GPIO: " + pin.getPinNumber());
        
        if (isNotifyEnabled()) {
            timer = new Timer();
            lastValue = pin.getValue();
            
            updateTask = new TimerTask() {
                @Override
                public void run() {
                    notifyPinStatus(object);
                }
            };
            
            LOGGER.logp(Level.INFO, CLASS_NAME, "notifyCreation", "start notification task: LocalObject: " + object + ", EPC:" + this.getEPC() + ", GPIO: " + pin.getPinNumber() + ", delay: " + delay + ", interval: " + interval);
        
            timer.schedule(updateTask, delay, interval);
        }
        
        LOGGER.exiting(CLASS_NAME, "notifyCreation");
    }
    
    @Override
    public synchronized ObjectData getUserData(LocalObject object, EPC epc) {
        LOGGER.entering(CLASS_NAME, "getUserData", new Object[]{object, epc});
        
        ObjectData data;
        
        switch (pin.getValue()) {
            case 0:
                data = new ObjectData((byte)0x31);
                break;
            case 1:
                data = new ObjectData((byte)0x30);
                break;
            default: 
                LOGGER.logp(Level.WARNING, CLASS_NAME, "setPinNumber", "Cannot read pin: " + pin.getPinNumber());
                data = null;
        }
        
        LOGGER.exiting(CLASS_NAME, "getUserData", data);
        return data;
    }
    
    @Override
    public synchronized boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        LOGGER.entering(CLASS_NAME, "setUserData", new Object[]{object, epc, data});
        
        boolean result;
        
        if (data.equals(new ObjectData((byte)0x30))) {
            result = pin.setValue(1);
        } else if (data.equals(new ObjectData((byte)0x31))) {
            result = pin.setValue(0);
        } else {
            result = false;
        }
        
        if (!result) {
            System.err.println("GPIOPinDelegate.setUserData:  Cannot write pin: " + pin.getPinNumber());
        }
        
        lastValue = pin.getValue();
        
        LOGGER.exiting(CLASS_NAME, "setUserData", result);
        return result;
    }
}
