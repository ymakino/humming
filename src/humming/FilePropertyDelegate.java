package humming;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyDelegate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class FilePropertyDelegate extends PropertyDelegate {
    private static final Logger LOGGER = Logger.getLogger(FilePropertyDelegate.class.getName());
    private static final String CLASS_NAME = FilePropertyDelegate.class.getName();
    
    private String filename;
    private BlockerFile blockerFile;
    
    public FilePropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, String filename) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        this.filename = filename;
        blockerFile = null;
        LOGGER.logp(Level.INFO, CLASS_NAME, "FilePropertyDelegate", "epc: " + epc + " -> file: " + filename);
    }
    
    public String getFileName() {
        return filename;
    }
    
    public void setBlockerFile(BlockerFile blockerFile) {
        this.blockerFile = blockerFile;
    }
    
    public BlockerFile getBlockerFile() {
        return blockerFile;
    }
    
    public boolean isBlocking() {
        boolean result = false;
        
        if (blockerFile != null) {
            result = blockerFile.existsFile();
        }
        
        return result;
    }
    
    public boolean waitUnblocked() {
        boolean result = true;
        
        if (blockerFile != null) {
            try {
                result = blockerFile.waitFile();
            } catch (InterruptedException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "waitUnblocked", "interrupted", ex);
                result = false;
            }
        }
        
        return result;
    }
    
    @Override
    public synchronized ObjectData getUserData(LocalObject object, EPC epc) {
        boolean unblocked = waitUnblocked();
        
        if (!unblocked) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "blocked: " + blockerFile);
            return null;
        }
            
        InputStreamReader reader;
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "begin: " + object + ", EPC: " + epc + " -> " + filename);
            reader = new InputStreamReader(new FileInputStream(filename));
        } catch (FileNotFoundException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + filename, ex);
            return null;
        }
        
        ObjectData data;
        
        try {
            char[] chars = new char[256];
            int len = reader.read(chars);
            if (len > 0) {
                String line = String.valueOf(chars).split("[^0-9a-fA-F]")[0];
                len = line.length();
                
                byte[] bytes = new byte[len/2];
                for (int i=0; i<len-1; i+=2) {
                    bytes[i/2] = (byte)Integer.parseInt(line.substring(i, i+2), 16);
                }
                
                data = new ObjectData(bytes);
            } else {
                data = new ObjectData();
            }
            
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "end: " + object + ", EPC: " + epc + " -> " + filename + ", data: " + data);
            return data;
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + filename, ex);
            return null;
        } catch (NumberFormatException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + filename, ex);
            return null;
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public synchronized boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        boolean unblocked = waitUnblocked();
        
        if (!unblocked) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "blocked: " + blockerFile);
            return false;
        }
        
        OutputStreamWriter writer;
        
        try {
            writer = new OutputStreamWriter(new FileOutputStream(filename));
        } catch (FileNotFoundException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed: " + object + ", EPC: " + epc + " -> " + filename, ex);
            return false;
        }
            
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "begin: " + object + ", EPC: " + epc + " -> " + filename + ", data: " + data);
            writer.write(data.toString());
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "end: " + object + ", EPC: " + epc + " -> " + filename + ", data: " + data);
            return true;
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed: " + object + ", EPC: " + epc + " -> " + filename, ex);
            return false;
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
