package humming.generator;

import echowand.common.EPC;
import echowand.object.EchonetObject;
import echowand.object.EchonetObjectException;
import echowand.object.ObjectData;
import echowand.object.RemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class PropertyXMLGenerator {
    private static final Logger LOGGER = Logger.getLogger(PropertyXMLGenerator.class.getName());
    private static final String CLASS_NAME = PropertyXMLGenerator.class.getName();
    
    private ObjectData DEFAULT_DATA = new ObjectData((byte)0x00);
    
    private EchonetObject object;
    private EPC epc;
    private boolean getEnabled;
    private boolean setEnabled;
    private boolean notifyEnabled;
    
    private String indent1 = "    ";
    private String indent2 = "      ";
    
    public enum GenerateMode {
        VARIABLE,
        PROXY
    }
    
    private GenerateMode generateMode = GenerateMode.VARIABLE;
    
    private static String b2s(boolean b) {
        if (b) {
            return "enabled";
        } else {
            return "disabled";
        }
    }
    

    public PropertyXMLGenerator(EchonetObject object, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled) {
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
        ObjectData data = DEFAULT_DATA;
        
        try {
            if (object.isGettable(epc)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "isGettable: " + epc);
                data = EchonetObjectHelper.getData(object, epc);
            } else if (object.isObservable(epc)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "isObservable: " + epc);
                data = EchonetObjectHelper.observeData(object, epc);
            }
            
            LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "generate: " + epc + " " + data);
        } catch (EchonetObjectException ex) {
            Logger.getLogger(PropertyXMLGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (data == null) {
            GeneratorException exception = new GeneratorException("invalid data: " + data);
            throw exception;
        }
        
        return String.format("<data type=\"variable\">%s</data>\n", data);
    }
    
    public String generateProxyDataElement() {
        String nodeElement = String.format("<node>%s</node>", ((RemoteObject)object).getNode());
        String eojElement = String.format("<eoj>%s</eoj>", object.getEOJ());
        return String.format("<data type=\"proxy\">%s%s</data>\n", nodeElement, eojElement);
    }
    
    public String generateDataElement() throws GeneratorException {
        switch (generateMode) {
            case PROXY: return generateProxyDataElement();
            case VARIABLE: return generateVariableDataElement();
            default: return generateVariableDataElement();
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
