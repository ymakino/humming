package humming;

import echowand.common.ClassEOJ;
import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class FilePropertyDelegateCreator implements PropertyDelegateCreator {
    private static final Logger LOGGER = Logger.getLogger(FilePropertyDelegateCreator.class.getName());
    private static final String CLASS_NAME = FilePropertyDelegateCreator.class.getName();
    
    public static final String FILE_TAG = "file";
    public static final String BLOCK_TAG = "block";
    public static final String LOCK_TAG = "lock";
    public static final String INPROCESS_TAG = "inprocess";
    public static final String IN_PROCESS_TAG = "in-process";
    public static final String NOTIFY_TAG = "notify";
    
    private boolean isValidFilename(String filename) throws HummingException {
        return true;
    }
    
    @Override
    public PropertyDelegate newPropertyDelegate(ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) throws HummingException {
        
        String filename = null;
        String blockName = null;
        String lockName = null;
        String inProcessName = null;
        String notifyName = null;
        int interval = -1;
        int delay = -1;
        String defaultValue = null;
        
        boolean useText = true;
        NodeList nodeList = node.getChildNodes();
        
        for (int i=0; i<nodeList.getLength(); i++) {
            Node fileInfo = nodeList.item(i);
            String infoName = fileInfo.getNodeName();
            
            if (fileInfo.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            useText = false;
            
            if (infoName.equals(FILE_TAG)) {
                filename = fileInfo.getTextContent();
                
                Node defaultNode = fileInfo.getAttributes().getNamedItem("default");
                if (defaultNode != null) {
                    defaultValue = defaultNode.getNodeValue();
                }
            } else if (infoName.equals(BLOCK_TAG)) {
                blockName = fileInfo.getTextContent();
            } else if (infoName.equals(LOCK_TAG)) {
                lockName = fileInfo.getTextContent();
            } else if (infoName.equals(INPROCESS_TAG) || infoName.equals(IN_PROCESS_TAG)) {
                inProcessName = fileInfo.getTextContent();
            } else if (infoName.equals(NOTIFY_TAG)) {
                notifyName = fileInfo.getTextContent();
                Node intervalNode = fileInfo.getAttributes().getNamedItem("interval");
                Node delayNode = fileInfo.getAttributes().getNamedItem("delay");
                
                if (intervalNode != null) {
                    String intervalString = intervalNode.getNodeValue();
                    try {
                        interval = Integer.parseInt(intervalString);
                    } catch (NumberFormatException ex) {
                        LOGGER.logp(Level.WARNING, CLASS_NAME, "newPropertyDelegate", "invalid interval value: " + intervalString, ex);
                    }
                }
                
                if (delayNode != null) {
                    String delayString = delayNode.getNodeValue();
                    try {
                        delay = Integer.parseInt(delayString);
                    } catch (NumberFormatException ex) {
                        LOGGER.logp(Level.WARNING, CLASS_NAME, "newPropertyDelegate", "invalid delay number: " + delayString, ex);
                    }
                }
            } else {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "newPropertyDelegate", "invalid element: " + infoName);
            }
        }
        
        if (useText) {
            filename = node.getTextContent();
        }
        
        if (filename == null) {
            throw new HummingException("no filename: " + node);
        }
        
        
        if (!isValidFilename(filename)) {
            throw new HummingException("invalid filename: " + filename);
        }
        
        FilePropertyDelegate delegate = new FilePropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, filename);
        
        if (defaultValue != null) {
            delegate.setDefaultValue(defaultValue);
        }
        
        if (blockName != null) {
            delegate.setBlockFile(new BlockFile(blockName));
        }
        
        if (lockName != null) {
            delegate.setLockFile(new LockFile(lockName));
        }
        
        if (inProcessName != null) {
            delegate.setInProcessFile(new InProcessFile(inProcessName));
        }
        
        if (notifyName != null) {
            FilePropertyDelegateNotifySender notifySender = new FilePropertyDelegateNotifySender(delegate, notifyName);
            
            if (interval >= 0) {
                notifySender.setInterval(interval);
            }
            
            if (delay >= 0) {
                notifySender.setDelay(delay);
            }
            
            delegate.setFilePropertyNotifySender(notifySender);
        }
        
        return delegate;
    }
}
