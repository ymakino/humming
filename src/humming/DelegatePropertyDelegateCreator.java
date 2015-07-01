package humming;

import echowand.common.ClassEOJ;
import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class DelegatePropertyDelegateCreator implements PropertyDelegateCreator {
    private static final Logger LOGGER = Logger.getLogger(DelegatePropertyDelegateCreator.class.getName());
    private static final String CLASS_NAME = DelegatePropertyDelegateCreator.class.getName();
    
    @Override
    public PropertyDelegate newPropertyDelegate(ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) throws HummingException {
        String delegateClassName = node.getTextContent().trim();

        try {
            Class<?> cls = Class.forName(delegateClassName);
            Constructor<?> constructor = cls.getConstructor(EPC.class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE);
            return (PropertyDelegate)constructor.newInstance(epc, getEnabled, setEnabled, notifyEnabled);
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
        }
    }
}
