package humming.generator;

import echowand.common.EPC;
import echowand.object.EchonetObject;
import echowand.object.ObjectData;
import echowand.object.RemoteObject;
import java.io.File;
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
    
    private boolean useFileDefault = true;
    private String fileTemplate = "[NODE]" + File.separator + "[EOJ]" + File.separator + "0x[EPC]";
    private String fileNotifyTemplate = "[NODE]" + File.separator + "[EOJ]" + File.separator + "0x[EPC]notify";
    private String fileBlockTemplate = "[NODE]" + File.separator + "[EOJ]" + File.separator + "0x[EPC]block";
    private String fileLockTemplate = null;
    
    private String commandGetTemplate = "[NODE]" + File.separator + "[EOJ]" + File.separator + "0x[EPC]get";
    private String commandSetTemplate = "[NODE]" + File.separator + "[EOJ]" + File.separator + "0x[EPC]set";
    
    private String propertyIndent = "    ";
    private String dataIndent     = "      ";
    private String innerIndent    = "        ";
    
    public enum GenerationType {
        VARIABLE,
        PROXY,
        FILE,
        COMMAND
    }
    
    private GenerationType generationType = GenerationType.VARIABLE;
    
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
    
    public GenerationType getGenerationType() {
        return generationType;
    }
    
    public void setGenerationType(GenerationType generateMode) {
        this.generationType = generateMode;
    } 
    
    public String generateVariableDataElement() throws GeneratorException {
        ObjectData data = null;
        
        if (EchonetObjectHelper.isGettable(object, epc)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "generateVariableDataElement", "isGettable: " + epc);
            data = EchonetObjectHelper.getData(object, epc);
        } else if (EchonetObjectHelper.isObservable(object, epc)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "generateVariableDataElement", "isObservable: " + epc);
            data = EchonetObjectHelper.observeData(object, epc);
        } else if (EchonetObjectHelper.isSettable(object, epc)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "generateVariableDataElement", "isSettable: " + epc);
            data = EchonetObjectHelper.forceGetData(object, epc);
        } else {
            GeneratorException exception = new GeneratorException("no valid access rule: " + epc);
            throw exception;
        }

        LOGGER.logp(Level.INFO, CLASS_NAME, "generateVariableDataElement", "generate: " + epc + " " + data);
        
        if (data == null) {
            GeneratorException exception = new GeneratorException("invalid data: " + data);
            throw exception;
        }
        
        return String.format(dataIndent + "<data type=\"variable\">%s</data>\n", data);
    }
    
    public String generateProxyDataElement() throws GeneratorException {
        if (object instanceof RemoteObject) {
            String nodeElement = String.format("<node>%s</node>", ((RemoteObject)object).getNode());
            String eojElement = String.format("<eoj>%s</eoj>", object.getEOJ());
            return String.format(dataIndent + "<data type=\"proxy\">%s%s</data>\n", nodeElement, eojElement);
        } else {
            throw new GeneratorException("unsupported object: " + object);
        }
    }
    
    private String generatePath(String template) {
        String nodeName = ((RemoteObject)object).getNode().toString();
        String eojName = object.getEOJ().toString();
        String epcName = epc.toString().substring(1);
        
        return template.replace("[NODE]", nodeName)
                       .replace("[EOJ]", eojName)
                       .replace("[EPC]", epcName);
    }
    
    public String generateFileDataElement() throws GeneratorException {
        if (object instanceof RemoteObject) {
            
            String defaultAttribute = "";
            
            if (useFileDefault) {
                if (EchonetObjectHelper.isGettable(object, epc)) {
                    LOGGER.logp(Level.INFO, CLASS_NAME, "generateFileDataElement", "isGettable: " + epc);
                    ObjectData data = EchonetObjectHelper.getData(object, epc);
                    defaultAttribute = String.format(" default=\"%s\"", data.toString());
                } else if (EchonetObjectHelper.isObservable(object, epc)) {
                    LOGGER.logp(Level.INFO, CLASS_NAME, "generateFileDataElement", "isObservable: " + epc);
                    ObjectData data = EchonetObjectHelper.observeData(object, epc);
                    defaultAttribute = String.format(" default=\"%s\"", data.toString());
                }
            }
            
            String fileElement = String.format(innerIndent + "<file%s>%s</file>\n", defaultAttribute, generatePath(fileTemplate));
            String fileNotifyElement = "";
            String fileBlockElement = "";
            String fileLockElement = "";
            
            if (notifyEnabled && fileNotifyTemplate != null) {
                fileNotifyElement = String.format(innerIndent + "<notify>%s</notify>\n", generatePath(fileNotifyTemplate));
            }
            
            if (fileBlockTemplate != null) {
                fileBlockElement = String.format(innerIndent + "<block>%s</block>\n", generatePath(fileBlockTemplate));
            }
            
            if (fileLockTemplate != null) {
                fileLockElement = String.format(innerIndent + "<lock>%s</lock>\n", generatePath(fileLockTemplate));
            }
            
            StringBuilder builder = new StringBuilder();
            builder.append(dataIndent + "<data type=\"file\">\n");
            builder.append(fileElement);
            builder.append(fileNotifyElement);
            builder.append(fileBlockElement);
            builder.append(fileLockElement);
            builder.append(dataIndent + "</data>\n");
            
            return builder.toString();
        }else {
            throw new GeneratorException("unsupported object: " + object);
        }
    }
    
    public String generateCommandDataElement() throws GeneratorException {
        if (object instanceof RemoteObject) {
            String getElement = "";
            String setElement = "";

            if (getEnabled) {
                getElement = String.format(innerIndent + "<get>%s</get>\n", generatePath(commandGetTemplate));
            }
            
            if (setEnabled) {
                setElement = String.format(innerIndent + "<set>%s</set>\n", generatePath(commandSetTemplate));
            }
            
            StringBuilder builder = new StringBuilder();
            builder.append(dataIndent + "<data type=\"command\">\n");
            builder.append(getElement);
            builder.append(setElement);
            builder.append(dataIndent + "</data>\n");
                
            return builder.toString();
        }else {
            throw new GeneratorException("unsupported object: " + object);
        }
    }
    
    public String generateDataElement() throws GeneratorException {
        switch (generationType) {
            case VARIABLE: return generateVariableDataElement();
            case PROXY: return generateProxyDataElement();
            case FILE: return generateFileDataElement();
            case COMMAND: return generateCommandDataElement();
            default: throw new GeneratorException("unsupported type: " + generationType);
        }
    }

    public String generate() throws GeneratorException  {
        
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(propertyIndent + "<property epc=\"%s\" get=\"%s\" set=\"%s\" notify=\"%s\">\n", epc.toString().substring(1), b2s(getEnabled), b2s(setEnabled), b2s(notifyEnabled)));
        builder.append(generateDataElement());
        builder.append(propertyIndent + "</property>\n");
        
        return builder.toString();
    }
    
}
