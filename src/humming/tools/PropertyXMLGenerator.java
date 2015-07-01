package humming.tools;

import echowand.common.EPC;
import echowand.object.EchonetObject;
import echowand.object.EchonetObjectException;
import echowand.object.ObjectData;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class PropertyXMLGenerator {
    private static final Logger LOGGER = Logger.getLogger(PropertyXMLGenerator.class.getName());
    private static final String CLASS_NAME = PropertyXMLGenerator.class.getName();
    
    private EchonetObject object;
    private EPC epc;
    private boolean getEnabled;
    private boolean setEnabled;
    private boolean notifyEnabled;
    
    private String indent1 = "    ";
    private String indent2 = "      ";
    
    private String b2s(boolean b) {
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

    public String generate() {
        StringBuilder builder = new StringBuilder();
        String beginElement = String.format(indent1 + "<property epc=\"%s\" get=\"%s\" set=\"%s\" notify=\"%s\">\n", epc.toString().substring(1), b2s(getEnabled), b2s(setEnabled), b2s(notifyEnabled));
        builder.append(beginElement);
        
        ObjectData data = new ObjectData((byte)0x00);
        try {
            data = object.getData(epc);
            LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "generate: " + epc + " " + data);
        } catch (EchonetObjectException ex) {
            Logger.getLogger(PropertyXMLGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        builder.append(String.format(indent2 + "<data type=\"variable\">%s</data>\n", data.toString()));
        
        builder.append(indent1 + "</property>\n");
        
        return builder.toString();
    }
    
}
