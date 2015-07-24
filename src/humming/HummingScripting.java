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
    private ScriptEngineManager manager;
    private ScriptEngine engine;
    private Bindings templateBindings;
    
    public HummingScripting() {
        manager = new ScriptEngineManager();
        engine = manager.getEngineByName("javascript");
        templateBindings = engine.createBindings();
        
        try {
            engine.eval("var shared={};", templateBindings);
        } catch (ScriptException ex) {
            Logger.getLogger(HummingScripting.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setScriptEngine(ScriptEngine engine) {
        this.engine = engine;
    }
    
    public ScriptEngine getScriptEngine() {
        return engine;
    }
    
    public void setTemplateBindings(Bindings templateBindings) {
        this.templateBindings = templateBindings;
    }
    
    public Bindings getTemplateBindings() {
        return templateBindings;
    }
    
    public Bindings createBindings() {
        Bindings bindings = engine.createBindings();
        bindings.putAll(templateBindings);
        return bindings;
    }
}
