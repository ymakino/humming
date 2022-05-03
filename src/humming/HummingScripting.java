package humming;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 *
 * @author ymakino
 */
public class HummingScripting {
    private static final Logger LOGGER = Logger.getLogger(HummingScripting.class.getName());
    private static final String CLASS_NAME = HummingScripting.class.getName();
    
    private ScriptEngineManager manager;
    private ScriptEngine engine;
    private Bindings templateBindings;
    
    public HummingScripting() {
        LOGGER.entering(CLASS_NAME, "HummingScripting");
        
        manager = new ScriptEngineManager();
        
        LOGGER.exiting(CLASS_NAME, "HummingScripting");
    }
    
    public void setScriptEngine(ScriptEngine engine) {
        this.engine = engine;
    }
    
    public ScriptEngine getScriptEngine() {
        
        if (engine == null) {
            
            engine = manager.getEngineByName("javascript");

            if (engine == null) {
                engine = manager.getEngineByName("rhino");
            }

            if (engine == null) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "HummingScripting", "Cannot get a ScriptEngine");
                LOGGER.exiting(CLASS_NAME, "HummingScripting");
                return null;
            }

            templateBindings = engine.createBindings();

            try {
                engine.eval("var shared={};", templateBindings);
            } catch (ScriptException ex) {
                Logger.getLogger(HummingScripting.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return engine;
    }
    
    public void setTemplateBindings(Bindings templateBindings) {
        this.templateBindings = templateBindings;
    }
    
    public Bindings getTemplateBindings() {
        return templateBindings;
    }
    
    public Bindings createBindings() {
        Bindings bindings = getScriptEngine().createBindings();
        bindings.putAll(templateBindings);
        return bindings;
    }
}
