package humming;

import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.util.HashMap;
import java.util.logging.Logger;
import org.w3c.dom.Node;

/**
 *
 * @author ymakino
 */
public class PropertyDelegateFactory {
    private static final Logger logger = Logger.getLogger(PropertyDelegateFactory.class.getName());
    private static final String className = PropertyDelegateFactory.class.getName();
    
    private HashMap<String, PropertyDelegateCreator> creatorMap;
    
    private static PropertyDelegateFactory instance = null;
    
    public static PropertyDelegateFactory getInstance() {
        if (instance == null) {
            instance = new PropertyDelegateFactory();
        }
        return instance;
    }
    
    public PropertyDelegateFactory() {
        creatorMap = new HashMap<String, PropertyDelegateCreator>();
    }
    
    public PropertyDelegateCreator add(String name, PropertyDelegateCreator creator) {
        return creatorMap.put(name, creator);
    }
    
    public PropertyDelegateCreator get(String name) {
        return creatorMap.get(name);
    }
    
    public boolean contains(String name) {
        return creatorMap.containsKey(name);
    }
    
    public PropertyDelegate newPropertyDelegate(String name, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) throws HummingException {
        PropertyDelegateCreator creator = creatorMap.get(name);
        
        if (creator == null) {
            throw new HummingException("no such creator: " + name);
        }
        
        return creator.newPropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, node);
    }
}
