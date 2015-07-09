package humming.generator;

import echowand.common.EPC;
import echowand.object.EchonetObject;
import echowand.object.ObjectData;
import echowand.object.RemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class PropertyElementGenerator {
    private static final Logger LOGGER = Logger.getLogger(PropertyElementGenerator.class.getName());
    private static final String CLASS_NAME = PropertyElementGenerator.class.getName();
    
    private EchonetObject object;
    private EPC epc;
    private boolean getEnabled;
    private boolean setEnabled;
    private boolean notifyEnabled;
    
    private String indent1 = "    ";
    private String indent2 = "      ";
    
    public enum GenerateMode {
        VARIABLE,
        PROXY,
        FILE,
        COMMAND
    }
    
    private GenerateMode generateMode = GenerateMode.VARIABLE;
    
    private static String b2s(boolean b) {
        if (b) {
            return "enabled";
        } else {
            return "disabled";
        }
    }
    

    public PropertyElementGenerator(EchonetObject object, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled) {
        this.object = object;
        this.epc = epc;
        this.getEnabled = getEnabled;
        this.setEnabled = setEnabled;
        this.notifyEnabled = notifyEnabled;
    }
    
    public GenerateMode getGenerateType() {
        return generateMode;
    }
    
    public void setGenerateType(GenerateMode generateMode) {
        this.generateMode = generateMode;
    } 
    
    public String generateVariableDataElement() throws GeneratorException {
        ObjectData data = null;
        
        if (EchonetObjectHelper.isGettable(object, epc)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "isGettable: " + epc);
            data = EchonetObjectHelper.getData(object, epc);
        } else if (EchonetObjectHelper.isObservable(object, epc)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "isObservable: " + epc);
            data = EchonetObjectHelper.observeData(object, epc);
        } else if (EchonetObjectHelper.isSettable(object, epc)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "isSettable: " + epc);
            data = EchonetObjectHelper.forceGetData(object, epc);
        } else {
            GeneratorException exception = new GeneratorException("no valid access rule: " + epc);
            throw exception;
        }

        LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "generate: " + epc + " " + data);
        
        if (data == null) {
            GeneratorException exception = new GeneratorException("invalid data: " + data);
            throw exception;
        }
        
        return String.format("<data type=\"variable\">%s</data>\n", data);
    }
    
    public String generateProxyDataElement() throws GeneratorException {
        if (object instanceof RemoteObject) {
            String nodeElement = String.format("<node>%s</node>", ((RemoteObject)object).getNode());
            String eojElement = String.format("<eoj>%s</eoj>", object.getEOJ());
            return String.format("<data type=\"proxy\">%s%s</data>\n", nodeElement, eojElement);
        } else {
            throw new GeneratorException("unsupported object: " + object);
        }
    }
    
    public String generateDataElement() throws GeneratorException {
        switch (generateMode) {
            case PROXY: return generateProxyDataElement();
            case VARIABLE: return generateVariableDataElement();
            default: throw new GeneratorException("unsupported mode: " + generateMode);
        }
    }

    public String generate() throws GeneratorException  {
        
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(indent1 + "<property epc=\"%s\" get=\"%s\" set=\"%s\" notify=\"%s\">\n", epc.toString().substring(1), b2s(getEnabled), b2s(setEnabled), b2s(notifyEnabled)));
        builder.append(indent2 + generateDataElement());
        builder.append(indent1 + "</property>\n");
        
        return builder.toString();
    }
    
}
