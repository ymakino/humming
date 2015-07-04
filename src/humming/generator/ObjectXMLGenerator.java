package humming.generator;

import echowand.common.ClassEOJ;
import echowand.common.EPC;
import echowand.common.PropertyMap;
import echowand.object.EchonetObject;
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
    
    private PropertyMap getGetPropertyMap() throws GeneratorException {
        return new PropertyMap(EchonetObjectHelper.getData(object, EPC.x9F).toBytes());
    }
    
    private PropertyMap getSetPropertyMap() throws GeneratorException {
        return new PropertyMap(EchonetObjectHelper.getData(object, EPC.x9E).toBytes());
    }
    
    private PropertyMap getAnnoPropertyMap() throws GeneratorException {
        return new PropertyMap(EchonetObjectHelper.getData(object, EPC.x9D).toBytes());
    }
    
    private boolean isPropertyMapEPC(EPC epc) {
        return epc == EPC.x9D || epc == EPC.x9E || epc == EPC.x9F;
    }
    
    public String generate() throws GeneratorException {
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "generate: " + object);
        
        StringBuilder propertyBuilder = new StringBuilder();
        
        PropertyMap getMap = getGetPropertyMap();
        PropertyMap setMap = getSetPropertyMap();
        PropertyMap annoMap = getAnnoPropertyMap();
        
        for (int i=0x80; i<=0xff; i++) {
            EPC epc = EPC.fromByte((byte)i);
            
            if (isPropertyMapEPC(epc)) {
                continue;
            }
            
            if (setMap.isSet(epc) || getMap.isSet(epc) || annoMap.isSet(epc)) {
                PropertyXMLGenerator propertyGenerator = new PropertyXMLGenerator(object, epc, getMap.isSet(epc), setMap.isSet(epc), annoMap.isSet(epc));
                propertyBuilder.append(propertyGenerator.generate());
            }
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(indent + "<object ceoj=\"%s\">\n", object.getEOJ().getClassEOJ()));
        builder.append(propertyBuilder);
        builder.append(indent + "</object>\n");
        
        return builder.toString();
    }
}
