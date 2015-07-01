package humming.tools;

import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.net.Node;
import echowand.object.EchonetObjectException;
import echowand.object.ObjectData;
import echowand.object.RemoteObject;
import echowand.service.Service;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class DeviceXMLGenerator {
    private static final Logger LOGGER = Logger.getLogger(DeviceXMLGenerator.class.getName());
    private static final String CLASS_NAME = DeviceXMLGenerator.class.getName();
    
    private Service service;
    private Node node;
    
    public DeviceXMLGenerator(Service service, Node node) {
        this.service = service;
        this.node = node;
    }
    
    public String generate() throws EchonetObjectException, GeneratorException {
        service.registerRemoteEOJ(node, new EOJ("0ef001"));
        RemoteObject nodeProfile = service.getRemoteObject(node, new EOJ("0ef001"));
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "generate: " + nodeProfile);
        
        ObjectData eojs = nodeProfile.getData(EPC.xD6);
        
        List<EOJ> eojList = new LinkedList<EOJ>();
        
        for (int i=1; i<eojs.size(); i+=3) {
            eojList.add(new EOJ(eojs.get(i), eojs.get(i + 1), eojs.get(i + 2)));
        }

        Collections.sort(eojList, new Comparator<EOJ>() {
            @Override
            public int compare(EOJ o1, EOJ o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        
        StringBuilder builder = new StringBuilder();
        builder.append("<device>\n");
        
        for (EOJ eoj : eojList) {
            service.registerRemoteEOJ(node, eoj);
            RemoteObject object = service.getRemoteObject(node, eoj);

            ObjectXMLGenerator generator = new ObjectXMLGenerator(object);
            builder.append(generator.generate());
        }
        
        builder.append("</device>");
        
        return builder.toString();
    }
}
