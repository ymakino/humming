package humming.generator;

import echowand.common.EPC;
import echowand.common.PropertyMap;
import echowand.object.EchonetObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ObjectElementGenerator {
    private static final Logger LOGGER = Logger.getLogger(ObjectElementGenerator.class.getName());
    private static final String CLASS_NAME = ObjectElementGenerator.class.getName();
    
    private EchonetObject object;
    private String objectIndent = "  ";
    
    public ObjectElementGenerator(EchonetObject object) {
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
                PropertyElementGenerator propertyGenerator = new PropertyElementGenerator(object, epc, getMap.isSet(epc), setMap.isSet(epc), annoMap.isSet(epc));
                propertyBuilder.append(propertyGenerator.generate());
            }
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(objectIndent + "<object ceoj=\"%s\">\n", object.getEOJ().getClassEOJ()));
        builder.append(propertyBuilder);
        builder.append(objectIndent + "</object>\n");
        
        return builder.toString();
    }
}
