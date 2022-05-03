package humming;

import echowand.util.TimeoutTask;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class BlockFile {
    private static final Logger LOGGER = Logger.getLogger(BlockFile.class.getName());
    private static final String CLASS_NAME = BlockFile.class.getName();
    
    private File file;
    private long timeout = 60000;
    private long interval = 100;

    public BlockFile(String filename) {
        LOGGER.entering(CLASS_NAME, "BlockFile", filename);
        
        this.file = new File(filename);
        
        LOGGER.exiting(CLASS_NAME, "BlockFile");
    }

    public File getFile() {
        LOGGER.entering(CLASS_NAME, "getFile");
        
        LOGGER.exiting(CLASS_NAME, "getFile", file);
        return file;
    }

    public boolean createFile() {
        LOGGER.entering(CLASS_NAME, "createFile");

        boolean result = false;

        try {
            result = file.createNewFile();
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "createFile", "cannot create: " + file);
        }

        LOGGER.exiting(CLASS_NAME, "createFile", result);
        return result;
    }

    public boolean deleteFile() {
        LOGGER.entering(CLASS_NAME, "deleteFile");

        boolean result = file.delete();

        LOGGER.exiting(CLASS_NAME, "deleteFile", result);
        return result;
    }

    public boolean existsFile() {
        LOGGER.entering(CLASS_NAME, "existsFile");
        
        boolean result = file.exists();
        LOGGER.exiting(CLASS_NAME, "existsFile", result);
        return result;
    }

    public boolean waitFile() throws InterruptedException {
        LOGGER.entering(CLASS_NAME, "waitFile");

        boolean blocking = existsFile();
        
        if (blocking) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "waitFile", "waiting: " + file);
                    
            TimeoutTask timeoutTask = new TimeoutTask(timeout);
            timeoutTask.start();
            
            Thread.sleep(interval);
            blocking = existsFile();

            while (blocking && !timeoutTask.isTimedOut()) {
                Thread.sleep(interval);
                blocking = existsFile();
            }
            
            LOGGER.logp(Level.INFO, CLASS_NAME, "waitFile", "waited: " + file);
        }

        LOGGER.exiting(CLASS_NAME, "waitFile", !blocking);
        return !blocking;
    }

    @Override
    public String toString() {
        return BlockFile.class.getSimpleName() + "(" + file + ")";
    }
}
