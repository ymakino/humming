package humming;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class InProcessFile {
    private static final Logger LOGGER = Logger.getLogger(InProcessFile.class.getName());
    private static final String CLASS_NAME = InProcessFile.class.getName();
    
    private String name;
    
    public InProcessFile(String name) {
        this.name = name;
    }
    
    public void enterProcess() {
        InProcessFileManager.getInstance().enterProcess(name);
    }
    
    public synchronized void exitProcess() {
        InProcessFileManager.getInstance().exitProcess(name);
    }
}
