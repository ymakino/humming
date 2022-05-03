package humming;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class InstanceCreatorParser {
    private static final Logger LOGGER = Logger.getLogger(InstanceCreatorParser.class.getName());
    private static final String CLASS_NAME = InstanceCreatorParser.class.getName();
    
    public static final String DEFAULT_INSTANCE_NAME = "it";
    
    private String className;
    private HashMap<String, String> params = new HashMap<String, String>();
    private String initScript;
    private String instanceName;
    
    public InstanceCreatorParser(Node node) throws HummingException {
        LOGGER.entering(CLASS_NAME, "InstanceCreatorParser", node);
        
        NodeList nodeList = node.getChildNodes();
        
        for (int i=0; i<nodeList.getLength(); i++) {
            Node eachNode = nodeList.item(i);
            
            if (eachNode.getNodeName().equals("class")) {
                className = eachNode.getTextContent().trim();
            } else if (eachNode.getNodeName().equals("param")) {
                params.putAll(parseParam(eachNode));
            } else if (eachNode.getNodeName().equals("script")) {
                initScript = eachNode.getTextContent().trim();
                
                Node instanceAttr = eachNode.getAttributes().getNamedItem("instance");
                if (instanceAttr == null) {
                    instanceName = DEFAULT_INSTANCE_NAME;
                } else {
                    instanceName = instanceAttr.getTextContent().trim();
                }
            }
        }
        
        if (className == null) {
            throw new HummingException("no class element: " + node);
        }
        
        LOGGER.exiting(CLASS_NAME, "InstanceCreatorParser");
    }
    
    private static HashMap<String, String> parseParam(Node paramNode) {
        LOGGER.entering(CLASS_NAME, "parseParam", paramNode);
        
        String key = null;
        String value = null;
        
        Node nameNode = paramNode.getAttributes().getNamedItem("name");
        
        if (nameNode != null) {
            key = nameNode.getTextContent().trim();
        }
        
        value = paramNode.getTextContent();
        
        if (key == null || value == null) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "parseParam", "invalid contents: " + paramNode);
            LOGGER.exiting(CLASS_NAME, "parseParam", null);
            return null;
        }
        
        HashMap<String, String> param = new HashMap<String, String>();
        param.put(key, value);
        LOGGER.exiting(CLASS_NAME, "parseParam", param);
        return param;
    }
    
    public String getClassName() {
        return className;
    }
    
    public HashMap<String, String> getParams() {
        return params;
    }
    
    public String getInstanceName() {
        return instanceName;
    }
    
    public String getScript() {
        return initScript;
    }
}
