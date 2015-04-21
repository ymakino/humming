package humming;

import echowand.logic.RequestDispatcher;
import echowand.logic.TooManyObjectsException;
import echowand.net.Inet4Subnet;
import echowand.net.SubnetException;
import echowand.object.LocalObjectManager;
import echowand.service.Core;
import echowand.service.LocalObjectConfig;
import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.LinkedList;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author ymakino
 */
public class Humming {
    private static final Logger logger = Logger.getLogger(Humming.class.getName());
    private static final String className = Humming.class.getName();
    
    private Core core;
    private LinkedList<Node> nodes;
    private LinkedList<LocalObjectConfig> configs;
    
    public Humming(Core core) {
        this.core = core;
        nodes = new LinkedList<Node>();
        configs = new LinkedList<LocalObjectConfig>();
        
        PropertyDelegateFactory factory = PropertyDelegateFactory.getInstance();
        factory.add("const", new ConstPropertyDelegateFactory());
        factory.add("variable", new VariablePropertyDelegateFactory());
        factory.add("file", new FilePropertyDelegateFactory());
        factory.add("command", new CommandPropertyDelegateFactory());
        factory.add("proxy", new ProxyPropertyDelegateFactory(core));
    }
    
    public Core getCore() {
        return core;
    }
    
    public int countLocalObjectConfigs() {
        return configs.size();
    }
    
    public LocalObjectConfig getConfigAt(int index) {
        return configs.get(index);
    }
    
    public void addXMLDocument(Document doc) {
        NodeList objectList = doc.getElementsByTagName("device").item(0).getChildNodes();
        for (int i=0; i<objectList.getLength(); i++) {
            Node objectNode = objectList.item(i);
            if (objectNode.getNodeName().equals("object")) {
                nodes.add(objectNode);
                
                XMLObjectConfigCreator creator = new XMLObjectConfigCreator(objectNode);
                LocalObjectConfig config = creator.getConfig();
                configs.add(config);
                
                core.addLocalObjectConfig(config);
            }
        }
    }
    
    public static void replaceSetGetRequestDispatcher(Core core) {
        LocalObjectManager localManager = core.getLocalObjectManager();
        RequestDispatcher dispatcher = core.getRequestDispatcher();
        dispatcher.removeRequestProcessor(core.getSetGetRequestProcessor());
        dispatcher.addRequestProcessor(new ParallelSetGetRequestProcessor(localManager));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, SubnetException, TooManyObjectsException {
        
        Core core;
        int fileIndex;
        
        if (args.length > 0 && args[0].equals("-i")) {
            NetworkInterface nif = NetworkInterface.getByName(args[1]);
            core = new Core(Inet4Subnet.startSubnet(nif));
            fileIndex = 2;
        } else {
            core = new Core();
            fileIndex = 0;
        }
        
        Humming humming = new Humming(core);
        
        for (int i=fileIndex; i<args.length; i++) {
            String filename = args[i];
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new File(filename));
            humming.addXMLDocument(doc);
        }
        
        core.initialize();
        
        replaceSetGetRequestDispatcher(core);
        
        core.startService();
    }
}
