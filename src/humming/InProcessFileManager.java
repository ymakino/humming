package humming;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class InProcessFileManager {
    private static final Logger LOGGER = Logger.getLogger(InProcessFileManager.class.getName());
    private static final String CLASS_NAME = InProcessFileManager.class.getName();
    
    private HashMap<String, Integer> inProcessCountMap;
    
    private static InProcessFileManager instance;
    
    public static synchronized InProcessFileManager getInstance() {
        if (instance == null) {
            instance = new InProcessFileManager();
        }
        
        return instance;
    }
    
    private InProcessFileManager() {
        inProcessCountMap = new HashMap<String, Integer>();
    }
    
    private int getInProcessCount(String name) {
        Integer num = inProcessCountMap.get(name);
        
        if (num == null) {
            num = 0;
        }
        
        return num;
    }
    
    private void incrementInProcessCount(String name) {
        int newNum = getInProcessCount(name) + 1;
        inProcessCountMap.put(name, newNum);
    }
    
    private void decrementInProcessCount(String name) {
        int newNum = getInProcessCount(name) - 1;
        
        if (newNum < 0) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "decrementInProcessCount", "underflow: " + newNum);
            newNum = 0;
        }
        
        inProcessCountMap.put(name, newNum);
    }
    
    public synchronized void enterProcess(String name) {
        if (name != null) {
            incrementInProcessCount(name);
        
            try {
                File file = new File(name);
                
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        LOGGER.logp(Level.WARNING, CLASS_NAME, "enterProcess", "cannot create: " + name);
                    }
                }
            } catch (IOException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "enterProcess", "failed", ex);
            }
        }
    }
    
    public synchronized void exitProcess(String name) {
        if (name != null) {
            decrementInProcessCount(name);
            
            if (getInProcessCount(name) == 0) {
                File file = new File(name);
                if (!file.delete()) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "exitProcess", "cannot delete: " + name);
                }
            }
        }
    }
}
