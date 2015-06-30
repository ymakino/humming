package humming.tools;

import echowand.common.EPC;
import echowand.common.PropertyMap;
import echowand.object.EchonetObject;
import echowand.object.EchonetObjectException;
import echowand.object.ObjectData;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ObjectXMLGenerator {
    private static final Logger LOGGER = Logger.getLogger(ObjectXMLGenerator.class.getName());
    private static final String CLASS_NAME = ObjectXMLGenerator.class.getName();
    
    private EchonetObject object;
    private String indent = "  ";
    
    public ObjectXMLGenerator(EchonetObject object) {
        this.object = object;
    }
    
    private ObjectData getData(EPC epc) throws GeneratorException {
        for (int i=0; i<3; i++) {
            try {
                ObjectData data = object.getData(epc);
                LOGGER.logp(Level.INFO, CLASS_NAME, "getData", "getData(" + epc + "): " + data);
                return data;
            } catch (EchonetObjectException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "getData", "failed: ", ex);
            }
        }
        
        throw new GeneratorException("cannot get property map: " + epc);
    }
    
    private PropertyMap getGetPropertyMap() throws GeneratorException {
        return new PropertyMap(getData(EPC.x9F).toBytes());
    }
    
    private PropertyMap getSetPropertyMap() throws GeneratorException {
        return new PropertyMap(getData(EPC.x9D).toBytes());
    }
    
    private PropertyMap getAnnoPropertyMap() throws GeneratorException {
        return new PropertyMap(getData(EPC.x9E).toBytes());
    }
    
    public String generate() throws GeneratorException {
        StringBuilder builder = new StringBuilder();
        
        builder.append(String.format(indent + "<object ceoj=\"%s\">\n", object.getEOJ().getClassEOJ().toString()));
        
        PropertyMap getMap = getGetPropertyMap();
        PropertyMap setMap = getSetPropertyMap();
        PropertyMap annoMap = getAnnoPropertyMap();
        
        for (int i=0x80; i<=0xff; i++) {
            EPC epc = EPC.fromByte((byte)i);
            
            if (epc == EPC.x9D || epc == EPC.x9E || epc == EPC.x9F) {
                continue;
            }
            
            if (setMap.isSet(epc) || getMap.isSet(epc) || annoMap.isSet(epc)) {
                PropertyXMLGenerator propertyGenerator = new PropertyXMLGenerator(object, epc, getMap.isSet(epc), setMap.isSet(epc), annoMap.isSet(epc));
                builder.append(propertyGenerator.generate());
            }
        }
        
        builder.append(indent + "</object>\n");
        
        return builder.toString();
    }
}
