package humming.sample;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.Core;
import echowand.service.PropertyDelegate;
import humming.HummingException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class GPIOPinPropertyDelegate extends PropertyDelegate {
    private static final Logger LOGGER = Logger.getLogger(GPIOPinPropertyDelegate.class.getName());
    private static final String CLASS_NAME = GPIOPinPropertyDelegate.class.getName();
    
    private GPIOPin pin = null;
    private Timer timer = null;
    private TimerTask updateTask = null;
    private int delay = 1000;
    private int interval = 1000;
    private ObjectData onData = new ObjectData((byte)0x30);
    private ObjectData offData = new ObjectData((byte)0x31);

    public GPIOPinPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, HashMap<String, String> params) throws HummingException {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        LOGGER.entering(CLASS_NAME, "GPIOPinPropertyDelegate", new Object[]{epc, getEnabled, setEnabled, notifyEnabled, params});
        
        parseParams(params);
        
        LOGGER.exiting(CLASS_NAME, "GPIOPinPropertyDelegate");
    }
    
    private void parseParams(HashMap<String, String> params) throws HummingException {
        LOGGER.entering(CLASS_NAME, "parseParams", params);
        
        for (String key : params.keySet()) {
            String value = params.get(key).trim();
            switch (key.toLowerCase()) {
                case "pinnumber":
                    setPinNumber(Integer.parseInt(value));
                    break;
            }
        }
        
        for (String key : params.keySet()) {
            String value = params.get(key).trim();
            switch (key.toLowerCase()) {
                case "pinnumber":
                    break;
                case "delay":
                    setDelay(Integer.parseInt(value));
                    break;
                case "interval":
                    setInterval(Integer.parseInt(value));
                    break;
                case "negative":
                    setNegative(Boolean.parseBoolean(value));
                    break;
                case "usesudo":
                    setUseSudo(Boolean.parseBoolean(value));
                    break;
                case "groupname":
                    setGroupName(value);
                    break;
                case "groupreadable":
                    this.setGroupReadable(Boolean.parseBoolean(value));
                    break;
                case "groupwritable":
                    this.setGroupWritable(Boolean.parseBoolean(value));
                    break;
                case "otherreadable":
                    this.setOtherReadable(Boolean.parseBoolean(value));
                    break;
                case "otherwritable":
                    this.setOtherWritable(Boolean.parseBoolean(value));
                    break;
                case "ondata":
                    this.setOnData(value);
                    break;
                case "offdata":
                    this.setOffData(value);
                    break;
                default:
                    throw new HummingException("invalid parameter: " + key + ": " + value);
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "parseParams");
    }
    
    public void setOnData(byte... data) {
        LOGGER.entering(CLASS_NAME, "setOnData", data);
        
        onData = new ObjectData(data);
        
        LOGGER.exiting(CLASS_NAME, "setOnData");
    }
    
    public void setOnData(ObjectData data) {
        LOGGER.entering(CLASS_NAME, "setOnData", data);
        
        onData = data;
        
        LOGGER.exiting(CLASS_NAME, "setOnData");
    }
    
    public ObjectData getOnData() {
        LOGGER.entering(CLASS_NAME, "getOnData");
        
        LOGGER.exiting(CLASS_NAME, "getOnData", onData);
        return onData;
    }
    
    public void setOffData(byte... data) {
        LOGGER.entering(CLASS_NAME, "setOffData", data);
        
        offData = new ObjectData(data);
        
        LOGGER.exiting(CLASS_NAME, "setOffData");
    }
    
    public void setOffData(ObjectData data) {
        LOGGER.entering(CLASS_NAME, "setOffData", data);
        
        offData = data;
        
        LOGGER.exiting(CLASS_NAME, "setOffData");
    }
    
    public ObjectData getOffData() {
        LOGGER.entering(CLASS_NAME, "getOffData");
        
        LOGGER.exiting(CLASS_NAME, "getOffData", offData);
        return offData;
    }
    
    public void setPinNumber(int pinNumber) {
        LOGGER.entering(CLASS_NAME, "setPinNumber", pinNumber);
        
        pin = new GPIOPin(pinNumber);
        
        LOGGER.exiting(CLASS_NAME, "setPinNumber");
    }
    
    private void exportPin() {
        LOGGER.entering(CLASS_NAME, "exportPin");
        
        if (!pin.isExported()) {
            if (!pin.export()) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "exportPin", "Cannot export pin: " + pin.getPinNumber());
                LOGGER.exiting(CLASS_NAME, "setPinNumber");
                return;
            }
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.logp(Level.INFO, CLASS_NAME, "exportPin", "Set pin \"in\": " + pin.getPinNumber());
                if (!pin.setInput()) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "exportPin", "Cannot set pin \"in\": " + pin.getPinNumber());
                }
                
                LOGGER.logp(Level.INFO, CLASS_NAME, "exportPin", "Unexport pin: " + pin.getPinNumber());
                if (!pin.unexport()) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "exportPin", "Cannot unexport pin: " + pin.getPinNumber());
                }
            }
        });
        
        if (isSetEnabled()) {
            if (!pin.setOutput()) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "exportPin", "Cannot set pin \"out\": " + pin.getPinNumber());
            }
        } else {
            if (!pin.setInput()) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "exportPin", "Cannot set pin \"in\": " + pin.getPinNumber());
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "exportPin");
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
    
    public void setGroupName(String name) {
        pin.setGroupName(name);
    }
    
    public void clearGroupName(String name) {
        pin.setGroupName(null);
    }
    
    public void setGroupReadable(boolean readable) {
        pin.setGroupReadable(readable);
    }
    
    public void setGroupWritable(boolean writable) {
        pin.setGroupWritable(writable);
    }
    
    public void setGroup(boolean readable, boolean writable) {
        pin.setGroup(readable, writable);
    }
    
    public void setOtherReadable(boolean readable) {
        pin.setOtherReadable(readable);
    }
    
    public void setOtherWritable(boolean writable) {
        pin.setOtherWritable(writable);
    }
    
    public void setOther(boolean readable, boolean writable) {
        pin.setOther(readable, writable);
    }
    
    public void setUseSudo(boolean useSudo) {
        pin.setUseSudo(useSudo);
    }
    
    private ObjectData newObjectData(String s) {
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        
        if (s.length() == 0 || (s.length() % 2) == 1) {
            return null;
        }
        
        byte[] bytes = new byte[s.length() / 2];
        
        try {
            for (int i=0; i<bytes.length; i++) {
                String d = s.substring(i*2, i*2 + 2);
                bytes[i] = (byte)Integer.parseInt(d, 16);
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        
        return new ObjectData(bytes);
    }
    
    public void setOnData(String value) {
        ObjectData data = newObjectData(value);
        
        if (data == null) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setOnData", "invalid data: " + value);
            return;
        }
        
        onData = data;
    }
    
    public void setOffData(String value) {
        ObjectData data = newObjectData(value);
        
        if (data == null) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setOffData", "invalid data: " + value);
            return;
        }
        
        offData = data;
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
            
            EPC epc = GPIOPinPropertyDelegate.this.getEPC();
            ObjectData data = object.getData(epc);
            object.notifyDataChanged(epc, data, null);
            lastValue = value;

            value = pin.getValue();
        }
        
        // LOGGER.exiting(CLASS_NAME, "notifyPinStatus");
    }
    
    @Override
    public void notifyCreation(final LocalObject object, Core core) {
        LOGGER.entering(CLASS_NAME, "notifyCreation", object);
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "notifyCreation", "created LocalObject: " + object + ", GPIO: " + pin.getPinNumber());
        
        exportPin();
        
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
                data = offData;
                break;
            case 1:
                data = onData;
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
        
        if (data.equals(onData)) {
            result = pin.setValue(1);
        } else if (data.equals(offData)) {
            result = pin.setValue(0);
        } else {
            result = false;
        }
        
        if (!result) {
            System.err.println("GPIOPinPropertyDelegate.setUserData:  Cannot write pin: " + pin.getPinNumber());
        }
        
        lastValue = pin.getValue();
        
        LOGGER.exiting(CLASS_NAME, "setUserData", result);
        return result;
    }
}
