package humming;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
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
        this.file = new File(filename);
    }

    public File getFile() {
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
        return file.exists();
    }
    
    private static class TimeoutTask extends TimerTask {
        public long delay;
        public boolean timeouted;
        
        
        public TimeoutTask(long delay) {
            this.delay = delay;
            timeouted = false;
        }
        
        @Override
        public void run() {
            timeouted = true;
        }
        
        public void start() {
            Timer timer = new Timer(true);
            timer.schedule(this, delay);
        }
        
        public static TimeoutTask start(long timeout) {
            TimeoutTask timeoutTask = new TimeoutTask(timeout);
            timeoutTask.start();
            return timeoutTask;
        }
    }

    public boolean waitFile() throws InterruptedException {
        LOGGER.entering(CLASS_NAME, "waitFile");

        boolean blocking = existsFile();
        
        if (blocking) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "waitFile", "waiting: " + file);
                    
            TimeoutTask timeoutTask = TimeoutTask.start(timeout);
            
            Thread.sleep(interval);
            blocking = existsFile();

            while (blocking && !timeoutTask.timeouted) {
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
