package humming;

import echowand.common.ClassEOJ;
import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
        this.hummingScripting = hummingScripting;
    }
    
    public void setHummingScripting(HummingScripting hummingScripting) {
        this.hummingScripting = hummingScripting;
    }
    
    public HummingScripting getHummingScripting() {
        return hummingScripting;
    }
    
    @Override
    public PropertyDelegate newPropertyDelegate(ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) throws HummingException {
        InstanceCreatorParser parser = new InstanceCreatorParser(node);

        try {
            Class<?> cls = Class.forName(parser.getClassName());
            Constructor<?> constructor = cls.getConstructor(EPC.class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE);
            PropertyDelegate propertyDelegate = (PropertyDelegate)constructor.newInstance(epc, getEnabled, setEnabled, notifyEnabled);
            
            LOGGER.logp(Level.INFO, CLASS_NAME, "newPropertyDelegate", "class: " +  parser.getClassName());
            
            if (parser.getScript() != null) {
                Bindings bindings = hummingScripting.createBindings();
                bindings.put(parser.getInstanceName(), propertyDelegate);
                hummingScripting.getScriptEngine().eval(parser.getScript(), bindings);
            }
            
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
