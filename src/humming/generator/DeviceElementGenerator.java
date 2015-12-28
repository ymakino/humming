package humming.generator;

import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.net.Node;
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
public class DeviceElementGenerator {
    private static final Logger LOGGER = Logger.getLogger(DeviceElementGenerator.class.getName());
    private static final String CLASS_NAME = DeviceElementGenerator.class.getName();
    
    private Service service;
    private Node node;
    
    private String deviceIndent = "";
    
    public DeviceElementGenerator(Service service, Node node) {
        this.service = service;
        this.node = node;
    }
    
    public String generate() throws GeneratorException {
        service.registerRemoteEOJ(node, new EOJ("0ef001"));
        RemoteObject nodeProfile = service.getRemoteObject(node, new EOJ("0ef001"));
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "generate: " + nodeProfile);
        
        ObjectData eojs = EchonetObjectHelper.getData(nodeProfile, EPC.xD6);
        
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
        
        StringBuilder objectBuilder = new StringBuilder();
        
        for (EOJ eoj : eojList) {
            service.registerRemoteEOJ(node, eoj);
            RemoteObject object = service.getRemoteObject(node, eoj);

            ObjectElementGenerator generator = new ObjectElementGenerator(object);
            objectBuilder.append(generator.generate());
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append(deviceIndent + "<device>\n");
        builder.append(objectBuilder);
        builder.append(deviceIndent + "</device>\n");
        
        return builder.toString();
    }
}
