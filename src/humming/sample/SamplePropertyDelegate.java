package humming.sample;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyDelegate;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class SamplePropertyDelegate extends PropertyDelegate {
    private String filename = "/sys/class/thermal/thermal_zone0/temp";

    public SamplePropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
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
                return (Double.parseDouble(line) / 1000);
            } catch (IOException ex) {
                Logger.getLogger(SamplePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NumberFormatException ex) {
                Logger.getLogger(SamplePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    br.close();
                } catch (IOException ex) {
                    Logger.getLogger(SamplePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("SamplePropertyDelegate.getTemperature: no such file: " + file);
        }
        
        return 12.3;
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        System.out.println("SamplePropertyDelegate.getUserData("+object + ", " + epc + ")");
        double temp = getTemperature();
        int num = (int) (temp * 10);
        
        byte b1 = (byte)((num >> 8) & 0xff);
        byte b2 = (byte)(num & 0xff);
        return new ObjectData(b1, b2);
    }
}
