package humming;

import echowand.common.ClassEOJ;
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
    private static final Logger LOGGER = Logger.getLogger(PropertyDelegateFactory.class.getName());
    private static final String CLASS_NAME = PropertyDelegateFactory.class.getName();
    
    private HashMap<String, PropertyDelegateCreator> creatorMap;
    
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
    
    public PropertyDelegate newPropertyDelegate(String name, ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) throws HummingException {
        PropertyDelegateCreator creator = creatorMap.get(name);
        
        if (creator == null) {
            throw new HummingException("no such creator: " + name);
        }
        
        return creator.newPropertyDelegate(ceoj, epc, getEnabled, setEnabled, notifyEnabled, node);
    }
}
