package humming;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.Core;
import echowand.service.PropertyDelegate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
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
    private BlockFile blockFile;
    private LockFile lockFile;
    private FilePropertyDelegateNotifySender sender;
    private String defaultValue = null;
    private InProcessFile inProcessFile;
    
    public FilePropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, String filename) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        this.filename = filename;
        blockFile = null;
        lockFile = null;
        sender = null;
        defaultValue = null;
        inProcessFile = null;
        LOGGER.logp(Level.INFO, CLASS_NAME, "FilePropertyDelegate", "epc: " + epc + " -> file: " + filename);
    }
    
    public String getFileName() {
        return filename;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setBlockFile(BlockFile blockFile) {
        this.blockFile = blockFile;
    }
    
    public BlockFile getBlockFile() {
        return blockFile;
    }
    
    public boolean isBlocking() {
        boolean result = false;
        
        if (blockFile != null) {
            result = blockFile.existsFile();
        }
        
        return result;
    }
    
    public boolean waitUnblocked() {
        boolean result = true;
        
        if (blockFile != null) {
            try {
                result = blockFile.waitFile();
            } catch (InterruptedException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "waitUnblocked", "interrupted", ex);
                result = false;
            }
        }
        
        return result;
    }
    
    public void setLockFile(LockFile lockFile) {
        this.lockFile = lockFile;
    }
    
    public LockFile getLockFile() {
        return lockFile;
    }
    
    public boolean lockFile() {
        boolean result = false;
        
        if (lockFile != null) {
            try {
                result = lockFile.lock();
            } catch (HummingException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "lockFile", "failed", ex);
                result = false;
            }
        }
        
        return result;
    }
    
    public boolean releaseFile() {
        boolean result = false;
        
        if (lockFile != null) {
            try {
                lockFile.release();
            } catch (HummingException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "releaseFile", "failed", ex);
                result = false;
            }
        }
        
        return result;
    }
    
    public void setInProcessFile(InProcessFile inProcessFile) {
        this.inProcessFile = inProcessFile;
    }
    
    public InProcessFile getInProcessFile() {
        return inProcessFile;
    }
    
    public void setFilePropertyNotifySender(FilePropertyDelegateNotifySender sender) {
        this.sender = sender;
    }
    
    public FilePropertyDelegateNotifySender getFilePropertyNotifySender() {
        return sender;
    }
    
    private synchronized void enterProcess() {
        if (inProcessFile != null) {
            inProcessFile.enterProcess();
        }
    }
    
    private synchronized void exitProcess() {
        if (inProcessFile != null) {
            inProcessFile.exitProcess();
        }
    }
    
    @Override
    public void notifyCreation(LocalObject object, Core core) {
        if (sender != null) {
            sender.addProperty(object, getEPC());
            
            if (!sender.isAlive()) {
                sender.start();
            }
        }
    }
    
    private ObjectData getUserDataPrivate(LocalObject object, EPC epc) {
        Reader reader;
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserDataPrivate", "begin: " + object + ", EPC: " + epc + " -> " + filename);
            reader = new InputStreamReader(new FileInputStream(filename));
        } catch (FileNotFoundException ex) {
            if (defaultValue != null) {
                reader = new StringReader(defaultValue);
            } else {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserDataPrivate", "failed: " + object + ", EPC: " + epc + " -> " + filename, ex);
                return null;
            }
        }
        
        ObjectData data = new ObjectData();
        
        try {
            char[] chars = new char[512];
            int len = reader.read(chars);
            if (len > 0) {
                String line = String.valueOf(chars).split("[^0-9a-fA-F]")[0];
                len = line.length();
                
                byte[] bytes = new byte[len/2];
                for (int i=0; i<len-1; i+=2) {
                    bytes[i/2] = (byte)Integer.parseInt(line.substring(i, i+2), 16);
                }
                
                data = new ObjectData(bytes);
            }
            
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserDataPrivate", "end: " + object + ", EPC: " + epc + " -> " + filename + ", data: " + data);
            return data;
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserDataPrivate", "failed: " + object + ", EPC: " + epc + " -> " + filename, ex);
            return null;
        } catch (NumberFormatException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserDataPrivate", "failed: " + object + ", EPC: " + epc + " -> " + filename, ex);
            return null;
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserDataPrivate", "filed to close: " + reader, ex);
            }
        }
    }
    
    private ObjectData getUserDataWithLock(LocalObject object, EPC epc) {
        boolean locked = lockFile();
        
        try {
            boolean unblocked = waitUnblocked();

            if (!unblocked) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "blocked: " + blockFile);
                return null;
            }

            return getUserDataPrivate(object, epc);
        } finally {
            if (locked) {
                releaseFile();
            }
        }
    }
    
    @Override
    public synchronized ObjectData getUserData(LocalObject object, EPC epc) {
        enterProcess();
        
        try {
            return getUserDataWithLock(object, epc);
        } finally {
            exitProcess();
        }
    }
    
    
    private boolean setUserDataPrivate(LocalObject object, EPC epc, ObjectData data) {
        OutputStreamWriter writer;
        
        try {
            writer = new OutputStreamWriter(new FileOutputStream(filename));
        } catch (FileNotFoundException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserDataPrivate", "failed: " + object + ", EPC: " + epc + " -> " + filename, ex);
            return false;
        }
            
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserDataPrivate", "begin: " + object + ", EPC: " + epc + " -> " + filename + ", data: " + data);
            writer.write(data.toString());
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserDataPrivate", "end: " + object + ", EPC: " + epc + " -> " + filename + ", data: " + data);
            return true;
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserDataPrivate", "failed: " + object + ", EPC: " + epc + " -> " + filename, ex);
            return false;
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserDataPrivate", "filed to close: " + writer, ex);
            }
        }
    }
    
    private boolean setUserDataWithLock(LocalObject object, EPC epc, ObjectData data) {
        boolean locked = lockFile();
        
        try {
            boolean unblocked = waitUnblocked();

            if (!unblocked) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "blocked: " + blockFile);
                return false;
            }

            return setUserDataPrivate(object, epc, data);
        } finally {
            if (locked) {
                releaseFile();
            }
        }
    }

    @Override
    public synchronized boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        enterProcess();
        
        try {
            return setUserDataWithLock(object, epc, data);
        } finally {
            exitProcess();
        }
    }
}
