package humming;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class InstanceCreatorParser {
    public static final String DEFAULT_INSTANCE_NAME = "it";
    
    private String instanceName;
    private String className;
    private String initScript;
    
    public InstanceCreatorParser(Node node) throws HummingException {
        NodeList nodeList = node.getChildNodes();
        
        for (int i=0; i<nodeList.getLength(); i++) {
            Node eachNode = nodeList.item(i);
            
            if (eachNode.getNodeName().equals("class")) {
                className = eachNode.getTextContent().trim();
            } else if (eachNode.getNodeName().equals("config")) {
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
            className = node.getTextContent().trim();
        }
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getInstanceName() {
        return instanceName;
    }
    
    public String getScript() {
        return initScript;
    }
}
