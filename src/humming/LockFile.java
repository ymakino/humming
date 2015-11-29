package humming;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class LockFile {
    private static final Logger LOGGER = Logger.getLogger(LockFile.class.getName());
    private static final String CLASS_NAME = LockFile.class.getName();
    
    String filename;
    FileOutputStream outputStream = null;
    FileChannel fileChannel = null;
    FileLock fileLock = null;
    
    public LockFile(String filename) {
        this.filename = filename;
    }
    
    public boolean lock() throws HummingException {
        LOGGER.entering(CLASS_NAME, "lock");
        
        try {
            if (outputStream != null) {
                LOGGER.exiting(CLASS_NAME, "lock", false);
                return false;
            } else {
                outputStream = new FileOutputStream(filename, true);
                fileChannel = outputStream.getChannel();
                fileLock = fileChannel.lock();
                LOGGER.exiting(CLASS_NAME, "lock", true);
                return true;
            }
        } catch (FileNotFoundException ex) {
            throw new HummingException("cannot lock a file: " + filename, ex);
        } catch (IOException ex) {
            throw new HummingException("cannot lock a file: " + filename, ex);
        }
    }
    
    public boolean release() throws HummingException {
        LOGGER.entering(CLASS_NAME, "release");
        
        try {
            if (outputStream == null) {
                LOGGER.exiting(CLASS_NAME, "release", false);
                return false;
            } else {
                try {
                    outputStream.close();
                } finally {
                    outputStream = null;
                    fileChannel = null;
                    fileLock = null;
                }
                LOGGER.exiting(CLASS_NAME, "release", true);
                return true;
            }
        } catch (IOException ex) {
            throw new HummingException("cannot release a file: " + filename, ex);
        }
    }
}
