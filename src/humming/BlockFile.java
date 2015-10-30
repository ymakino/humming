package humming;

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

    public boolean waitFile() throws InterruptedException {
        LOGGER.entering(CLASS_NAME, "waitFile");

        boolean blocking = existsFile();

        for (int i=0; blocking && i<600; i++) {
            if (i==0) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "waitFile", "waiting: " + file);
            }

            Thread.sleep(100);

            blocking = existsFile();
        }

        LOGGER.exiting(CLASS_NAME, "waitFile", !blocking);
        return !blocking;
    }

    @Override
    public String toString() {
        return BlockFile.class.getSimpleName() + "(" + file + ")";
    }
}
