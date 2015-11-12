package humming.sample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class GPIOPin {
    private static final Logger LOGGER = Logger.getLogger(GPIOPin.class.getName());
    private static final String CLASS_NAME = GPIOPin.class.getName();
    
    private String exportFile = "/sys/class/gpio/export";
    private String unexportFile = "/sys/class/gpio/unexport";
    private String baseDirectoryTemplate = "/sys/class/gpio/gpio%d";
    private String valueFileTemplate = "/sys/class/gpio/gpio%d/value";
    private String directionFileTemplate = "/sys/class/gpio/gpio%d/direction";
    
    private int pinNumber;
    private boolean negative = false;
    
    private String getBaseDirectory() {
        LOGGER.entering(CLASS_NAME, "getBaseDirectory");
        
        String result = String.format(baseDirectoryTemplate, pinNumber);
        
        LOGGER.exiting(CLASS_NAME, "getBaseDirectory", result);
        return result;
    }
    
    private String getValueFile() {
        LOGGER.entering(CLASS_NAME, "getValueFile");
        
        String result = String.format(valueFileTemplate, pinNumber);
        
        LOGGER.exiting(CLASS_NAME, "getValueFile", result);
        return result;
    }
    
    private String getDirectionFile() {
        LOGGER.entering(CLASS_NAME, "getDirectionFile");
        
        String result = String.format(directionFileTemplate, pinNumber);
        
        LOGGER.exiting(CLASS_NAME, "getDirectionFile", result);
        return result;
    }
    
    private boolean write(String filename, int num) {
        LOGGER.entering(CLASS_NAME, "write", new Object[]{filename, num});
        
        boolean result = write(filename, String.format("%d", num));
        
        LOGGER.exiting(CLASS_NAME, "write", result);
        return result;
    }
    
    private boolean write(String filename, String value) {
        LOGGER.entering(CLASS_NAME, "write", new Object[]{filename, value});
        
        try {
            FileWriter writer = new FileWriter(filename);
            writer.append(value);
            writer.close();
            LOGGER.exiting(CLASS_NAME, "write", true);
            return true;
        } catch (IOException ex) {
            LOGGER.exiting(CLASS_NAME, "write", false);
            return false;
        }
    }
    
    public void setNegative(boolean negative) {
        LOGGER.entering(CLASS_NAME, "setNegative", negative);
        
        this.negative = negative;
        
        LOGGER.exiting(CLASS_NAME, "setNegative");
    }
    
    public boolean isNegative() {
        LOGGER.entering(CLASS_NAME, "isNegative");
        LOGGER.exiting(CLASS_NAME, "isNegative", negative);
        return negative;
    }
    
    private int convertLogic(int value) {
        LOGGER.entering(CLASS_NAME, "convertLogic", value);
        
        int newValue = value;
        
        if (negative) {
            switch (value) {
                case 0:
                    newValue = 1;
                    break;
                case 1:
                    newValue = 0;
                    break;
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "convertLogic", newValue);
        return newValue;
    }
    
    public boolean isExported() {
        LOGGER.entering(CLASS_NAME, "isExported");
        
        boolean result = Files.exists(new File(getBaseDirectory()).toPath());
        
        LOGGER.exiting(CLASS_NAME, "isExported", result);
        return result;
    }
    
    public boolean export() {
        LOGGER.entering(CLASS_NAME, "export");
        
        boolean result = false;
        
        if (write(exportFile, pinNumber)) {
            result = isExported();
        }

        LOGGER.exiting(CLASS_NAME, "export", result);
        return result;
    }
    
    public boolean unexport() {
        LOGGER.entering(CLASS_NAME, "unexport");
        
        boolean result = false;
        
        if (write(unexportFile, pinNumber)) {
            result = !isExported();
        }
        
        LOGGER.exiting(CLASS_NAME, "unexport", result);
        return result;
    }
    
    public GPIOPin(int pinNumber) {
        LOGGER.entering(CLASS_NAME, "GPIOPin", pinNumber);
        
        this.pinNumber = pinNumber;
        
        LOGGER.exiting(CLASS_NAME, "GPIOPin");
    }
    
    public int getPinNumber() {
        LOGGER.entering(CLASS_NAME, "getPinNumber");
        
        int result = pinNumber;
        
        LOGGER.exiting(CLASS_NAME, "getPinNumber", result);
        return result;
    }
    
    public boolean setInput() {
        LOGGER.entering(CLASS_NAME, "setInput");
        
        boolean result = write(getDirectionFile(), "in");
        
        LOGGER.exiting(CLASS_NAME, "setInput", result);
        return result;
    }
    
    public boolean setOutput() {
        LOGGER.entering(CLASS_NAME, "setOutput");
        
        boolean result = write(getDirectionFile(), "out");
        
        LOGGER.exiting(CLASS_NAME, "setOutput", result);
        return result;
    }
    
    public boolean setValue(int value) {
        LOGGER.entering(CLASS_NAME, "setValue", value);
        
        value = convertLogic(value);
        
        boolean result = write(getValueFile(), value);
        
        LOGGER.exiting(CLASS_NAME, "setValue", result);
        return result;
    }
    
    public int getValue() {
        LOGGER.entering(CLASS_NAME, "getValue");
        
        int value = -1;
        
        try {
            FileReader reader = new FileReader(getValueFile());
            int ch = reader.read();
            reader.close();
            
            if (ch != -1) {
                switch ((char)ch) {
                    case '0': value = convertLogic(0); break;
                    case '1': value = convertLogic(1); break;
                }
            }
            
        } catch (FileNotFoundException ex) {
            // Logger.getLogger(GPIOPin.class.getName()).log(Level.SEVERE, null, ex);
            value = -1;
        } catch (IOException ex) {
            // Logger.getLogger(GPIOPin.class.getName()).log(Level.SEVERE, null, ex);
            value = -1;
        }
        
        LOGGER.exiting(CLASS_NAME, "getValue", value);
        return value;
    }
    
    public static void main(String[] args) throws InterruptedException {
        LinkedList<GPIOPin> pins = new LinkedList<GPIOPin>();
        
        for (String arg : args) {
            pins.add(new GPIOPin(Integer.parseInt(arg)));
        }
        
        System.out.print("EXPORT:");
        
        for (GPIOPin pin : pins) {
            if (pin.export()) {
                System.out.print(" " + pin.getPinNumber());
            } else {
                System.out.print(" (" + pin.getPinNumber() + ")");
            }
        }
        
        System.out.println();
        
        System.out.print("SET_OUTPUT:");
        
        for (GPIOPin pin : pins) {
            if (pin.setOutput()) {
                System.out.print(" " + pin.getPinNumber());
            } else {
                System.out.print(" (" + pin.getPinNumber() + ")");
            }
        }
        
        System.out.println();
        
        for (;;) {
            
            System.out.print("ON: ");
            
            for (GPIOPin pin : pins) {
                if (pin.setValue(1)) {
                    System.out.print(" " + pin.getPinNumber());
                } else {
                    System.out.print(" (" + pin.getPinNumber() + ")");
                }
            }
            
            System.out.println();
            
            Thread.sleep(1000);
            
            System.out.print("OFF:");
            
            for (GPIOPin pin : pins) {
                if (pin.setValue(0)) {
                    System.out.print(" " + pin.getPinNumber());
                } else {
                    System.out.print(" (" + pin.getPinNumber() + ")");
                }
            }
            
            System.out.println();
            
            Thread.sleep(1000);
        }
    }
}
