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
    
    private HashMap<String, PropertyDelegateFactory> factoryMap;
    
    private static PropertyDelegateFactory instance = null;
    
    public static PropertyDelegateFactory getInstance() {
        if (instance == null) {
            instance = new PropertyDelegateFactory();
        }
        return instance;
    }
    
    public PropertyDelegateFactory() {
        factoryMap = new HashMap<String, PropertyDelegateFactory>();
    }
    
    public PropertyDelegateFactory add(String name, PropertyDelegateFactory factory) {
        return factoryMap.put(name, factory);
    }
    
    public PropertyDelegate newPropertyDelegate(String name, EPC epc, Node node) {
        return factoryMap.get(name).newPropertyDelegate(epc, node);
    }
    
    private class DefaultPropertyDelegate extends PropertyDelegate {
        public DefaultPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled) {
            super(epc, getEnabled, setEnabled, notifyEnabled);
        }
    }
    
    public PropertyDelegate newPropertyDelegate(EPC epc, Node node) {
        return new DefaultPropertyDelegate(epc, true, false, false);
    }
}
