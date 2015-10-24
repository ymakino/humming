package humming;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class BlockerFile {
    private static final Logger LOGGER = Logger.getLogger(BlockerFile.class.getName());
    private static final String CLASS_NAME = BlockerFile.class.getName();
    
    private File blockerFile;

    public BlockerFile(String blockerFileName) {
        this.blockerFile = new File(blockerFileName);
    }

    public File getFile() {
        return blockerFile;
    }

    public boolean createFile() {
        LOGGER.entering(CLASS_NAME, "createFile");

        boolean result = false;

        try {
            result = blockerFile.createNewFile();
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "createFile", "cannot create: " + blockerFile);
        }

        LOGGER.exiting(CLASS_NAME, "createFile", result);
        return result;
    }

    public boolean deleteFile() {
        LOGGER.entering(CLASS_NAME, "deleteFile");

        boolean result = blockerFile.delete();

        LOGGER.exiting(CLASS_NAME, "deleteFile", result);
        return result;
    }

    public boolean existsFile() {
        return blockerFile.exists();
    }

    public boolean waitFile() throws InterruptedException {
        LOGGER.entering(CLASS_NAME, "waitFile");

        boolean blocking = existsFile();

        for (int i=0; blocking && i<600; i++) {
            if (i==0) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "waitFile", "waiting: " + blockerFile);
            }

            Thread.sleep(100);

            blocking = existsFile();
        }

        LOGGER.exiting(CLASS_NAME, "waitFile", !blocking);
        return !blocking;
    }

    @Override
    public String toString() {
        return BlockerFile.class.getSimpleName() + "(" + blockerFile + ")";
    }
}
