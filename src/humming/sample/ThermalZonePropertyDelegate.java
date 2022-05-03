package humming.sample;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyDelegate;
import humming.HummingException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ThermalZonePropertyDelegate extends PropertyDelegate {
    private static final Logger LOGGER = Logger.getLogger(ThermalZonePropertyDelegate.class.getName());
    private static final String CLASS_NAME = ThermalZonePropertyDelegate.class.getName();
    
    private String template = "/sys/class/thermal/thermal_zone%d/temp";
    
    private String filename;
    private int index;
    private double scale;

    public ThermalZonePropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, HashMap<String, String> params) throws HummingException {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        LOGGER.entering(CLASS_NAME, "ThermalZonePropertyDelegate", new Object[]{epc, getEnabled, setEnabled, notifyEnabled, params});
        
        scale = 0.001;
        index = 0;
        filename = String.format(template, index);
        
        parseParams(params);
        
        LOGGER.exiting(CLASS_NAME, "ThermalZonePropertyDelegate");
    }
    
    private void parseParams(HashMap<String, String> params) throws HummingException {
        LOGGER.entering(CLASS_NAME, "parseParams", params);
        
        for (String key : params.keySet()) {
            String value = params.get(key).trim();
            switch (key.toLowerCase()) {
                case "template":
                    setTemplate(value);
                    break;
                case "index":
                    setIndex(Integer.parseInt(value));
                    break;
                case "scale":
                    setScale(Double.parseDouble(value));
                    break;
                default:
                    throw new HummingException("invalid parameter: " + key + ": " + value);
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "parseParams");
    }
    
    private void updateFilename() {
        filename = String.format(template, index);
    }
    
    public void setTemplate(String template) {
        this.template = template;
        updateFilename();
    }
    
    public String getTemplate() {
        return template;
    }
    
    public void setIndex(int index) {
        this.index = index;
        updateFilename();
    }
    
    public int getIndex() {
        return index;
    }
    
    public void setScale(double scale) {
        this.scale = scale;
    }
    
    public double getScale() {
        return scale;
    }
    
    public String getFilename() {
        return filename;
    }
    
    private double getTemperature() {
        File file = new File(filename);
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            try {
                String line = br.readLine();
                return (Double.parseDouble(line) * scale);
            } catch (IOException ex) {
                Logger.getLogger(ThermalZonePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NumberFormatException ex) {
                Logger.getLogger(ThermalZonePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    br.close();
                } catch (IOException ex) {
                    Logger.getLogger(ThermalZonePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("ThermalZonePropertyDelegate.getTemperature: no such file: " + file);
        }
        
        return 0.0;
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        System.out.println("ThermalZonePropertyDelegate.getUserData("+object + ", " + epc + ")");
        double temp = getTemperature();
        int num = (int) (temp * 10);
        
        byte b1 = (byte)((num >> 8) & 0xff);
        byte b2 = (byte)(num & 0xff);
        return new ObjectData(b1, b2);
    }
}
