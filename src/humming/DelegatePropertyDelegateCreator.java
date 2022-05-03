package humming;

import echowand.common.ClassEOJ;
import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptException;
import org.w3c.dom.Node;

/**
 *
 * @author ymakino
 */
public class DelegatePropertyDelegateCreator implements PropertyDelegateCreator {
    private static final Logger LOGGER = Logger.getLogger(DelegatePropertyDelegateCreator.class.getName());
    private static final String CLASS_NAME = DelegatePropertyDelegateCreator.class.getName();
    
    private HummingScripting hummingScripting;
    
    public DelegatePropertyDelegateCreator(HummingScripting hummingScripting) {
        LOGGER.entering(CLASS_NAME, "DelegatePropertyDelegateCreator", hummingScripting);
        
        this.hummingScripting = hummingScripting;
        
        LOGGER.exiting(CLASS_NAME, "DelegatePropertyDelegateCreator");
    }
    
    public void setHummingScripting(HummingScripting hummingScripting) {
        LOGGER.entering(CLASS_NAME, "setHummingScripting", hummingScripting);
        
        this.hummingScripting = hummingScripting;
        
        LOGGER.exiting(CLASS_NAME, "setHummingScripting");
    }
    
    public HummingScripting getHummingScripting() {
        LOGGER.entering(CLASS_NAME, "getHummingScripting");
        
        HummingScripting result = hummingScripting;
        LOGGER.entering(CLASS_NAME, "getHummingScripting", result);
        return result;
    }
    
    private PropertyDelegate createPropertyDelegate(String className, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, HashMap<String, String> params) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        LOGGER.entering(CLASS_NAME, "createPropertyDelegate", new Object[]{className, epc, getEnabled, setEnabled, notifyEnabled, params});
        
        Class cls = Class.forName(className);
        
        PropertyDelegate propertyDelegate;
        
        if (!params.isEmpty()) {
            Constructor constructor = cls.getConstructor(EPC.class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, HashMap.class);
            propertyDelegate = (PropertyDelegate)constructor.newInstance(epc, getEnabled, setEnabled, notifyEnabled, params);
            LOGGER.logp(Level.INFO, CLASS_NAME, "createPropertyDelegate", className + ": constructor with params");
        } else {
            try {
                Constructor constructor = cls.getConstructor(EPC.class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, HashMap.class);
                propertyDelegate = (PropertyDelegate)constructor.newInstance(epc, getEnabled, setEnabled, notifyEnabled, params);
                LOGGER.logp(Level.INFO, CLASS_NAME, "createPropertyDelegate", className + ": constructor with params");
            } catch (NoSuchMethodException ex) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "createPropertyDelegate", className + ": no constructor found with params");
                Constructor constructor = cls.getConstructor(EPC.class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE);
                propertyDelegate = (PropertyDelegate)constructor.newInstance(epc, getEnabled, setEnabled, notifyEnabled);
                LOGGER.logp(Level.INFO, CLASS_NAME, "createPropertyDelegate", className + ": constructor without params");
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "createPropertyDelegate", propertyDelegate);
        return propertyDelegate;
    }
    
    @Override
    public PropertyDelegate newPropertyDelegate(ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) throws HummingException {
        LOGGER.entering(CLASS_NAME, "newPropertyDelegate", new Object[]{ceoj, epc, getEnabled, setEnabled, notifyEnabled, node});
        
        InstanceCreatorParser parser = new InstanceCreatorParser(node);

        try {
            PropertyDelegate propertyDelegate = createPropertyDelegate(parser.getClassName(), epc, getEnabled, setEnabled, notifyEnabled, parser.getParams());
            
            LOGGER.logp(Level.INFO, CLASS_NAME, "newPropertyDelegate", "class: " +  parser.getClassName());
            
            if (parser.getScript() != null) {
                Bindings bindings = hummingScripting.createBindings();
                bindings.put(parser.getInstanceName(), propertyDelegate);
                hummingScripting.getScriptEngine().eval(parser.getScript(), bindings);
            }
            
            LOGGER.exiting(CLASS_NAME, "newPropertyDelegate", propertyDelegate);
            return propertyDelegate;
        } catch (InstantiationException ex) {
            Logger.getLogger(DelegatePropertyDelegateCreator.class.getName()).log(Level.SEVERE, null, ex);
            throw new HummingException("failed", ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(DelegatePropertyDelegateCreator.class.getName()).log(Level.SEVERE, null, ex);
            throw new HummingException("failed", ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(DelegatePropertyDelegateCreator.class.getName()).log(Level.SEVERE, null, ex);
            throw new HummingException("failed", ex);
        } catch (SecurityException ex) {
            Logger.getLogger(DelegatePropertyDelegateCreator.class.getName()).log(Level.SEVERE, null, ex);
            throw new HummingException("failed", ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(DelegatePropertyDelegateCreator.class.getName()).log(Level.SEVERE, null, ex);
            throw new HummingException("failed", ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(DelegatePropertyDelegateCreator.class.getName()).log(Level.SEVERE, null, ex);
            throw new HummingException("failed", ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DelegatePropertyDelegateCreator.class.getName()).log(Level.SEVERE, null, ex);
            throw new HummingException("failed", ex);
        } catch (ScriptException ex) {
            Logger.getLogger(DelegatePropertyDelegateCreator.class.getName()).log(Level.SEVERE, null, ex);
            throw new HummingException("failed", ex);
        }
    }
}
