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
    private File lockfile;
    
    public FilePropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, String filename) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        this.filename = filename;
        lockfile = null;
        LOGGER.logp(Level.INFO, CLASS_NAME, "FilePropertyDelegate", "epc: " + epc + " -> file: " + filename);
    }
    
    public String getFileName() {
        return filename;
    }
    
    public void setLockFile(String lockfile) {
        this.lockfile = new File(lockfile);
    }
    
    public void setLockFile(File lockfile) {
        this.lockfile = lockfile;
    }
    
    public File getLockFile() {
        return lockfile;
    }
    
    public boolean isLocked() {
        return lockfile != null && lockfile.exists();
    }
    
    public boolean waitUnlocked() throws InterruptedException {
        LOGGER.entering(CLASS_NAME, "waitUnlocked");
        
        boolean unlocked = !isLocked();
        
        for (int i=0; !unlocked && i<600; i++) {
            if (i==0) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "waitUnlocked", "waiting: " + lockfile);
            }
            
            Thread.sleep(100);
            
            unlocked = !isLocked();
        }
        
        LOGGER.exiting(CLASS_NAME, "waitUnlocked", unlocked);
        return unlocked;
    }
    
    @Override
    public synchronized ObjectData getUserData(LocalObject object, EPC epc) {
        try {
            boolean unlocked = waitUnlocked();
            if (!unlocked) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "locked: " + lockfile);
                return null;
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
            
        InputStreamReader reader;
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData begin", object + ", EPC: " + epc + " -> " + filename);
            reader = new InputStreamReader(new FileInputStream(filename));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
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
            
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData end", object + ", EPC: " + epc + " -> " + filename + ", data: " + data);
            return data;
        } catch (IOException ex) {
            Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NumberFormatException ex) {
            Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return null;
    }

    @Override
    public synchronized boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        try {
            boolean unlocked = waitUnlocked();
            if (!unlocked) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "locked: " + lockfile);
                return false;
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        OutputStreamWriter writer;
        
        try {
            writer = new OutputStreamWriter(new FileOutputStream(filename));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
            
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData begin", object + ", EPC: " + epc + " -> " + filename + ", data: " + data);
            writer.write(data.toString());
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData end", object + ", EPC: " + epc + " -> " + filename + ", data: " + data);
            return true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return false;
    }
}
