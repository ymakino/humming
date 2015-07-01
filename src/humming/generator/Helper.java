package humming.generator;

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
public class Helper {
    private static final Logger LOGGER = Logger.getLogger(DeviceXMLGenerator.class.getName());
    private static final String CLASS_NAME = DeviceXMLGenerator.class.getName();
    
    public static ObjectData getData(EchonetObject object, EPC epc) throws GeneratorException {
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
}
