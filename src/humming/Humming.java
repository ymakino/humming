package humming;

import echowand.logic.RequestDispatcher;
import echowand.logic.RequestProcessor;
import echowand.logic.TooManyObjectsException;
import echowand.net.Inet4Subnet;
import echowand.net.SubnetException;
import echowand.service.Core;
import echowand.service.LocalObjectConfig;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.logging.Level;
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
    private PropertyDelegateFactory factory;
    private LinkedList<Node> nodes;
    private LinkedList<LocalObjectConfig> configs;
    
    public Humming(Core core) {
        this.core = core;
        nodes = new LinkedList<Node>();
        configs = new LinkedList<LocalObjectConfig>();
        
        factory = PropertyDelegateFactory.getInstance();
        factory.add("const", new ConstPropertyDelegateCreator());
        factory.add("variable", new VariablePropertyDelegateCreator());
        factory.add("file", new FilePropertyDelegateCreator());
        factory.add("command", new CommandPropertyDelegateCreator());
        factory.add("proxy", new ProxyPropertyDelegateCreator(core));
    }
    
    public PropertyDelegateFactory getDelegateFactory() {
        return factory;
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
    
    public void addXMLObject(Node objectNode) throws HummingException {
        if (!objectNode.getNodeName().equals("object")) {
            logger.logp(Level.WARNING, className, "addXMLObject", "invalid object: " + objectNode.getNodeName());
            throw new HummingException("invalid object: " + objectNode.getNodeName());
        }
        
        nodes.add(objectNode);

        ObjectConfigCreator creator = new ObjectConfigCreator(objectNode, factory);
        LocalObjectConfig config = creator.getConfig();
        configs.add(config);

        core.addLocalObjectConfig(config);
    }
    
    public void parseXMLString(String xmlString) throws HummingException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            parseXMLDocument(builder.parse(new ByteArrayInputStream(xmlString.getBytes())));
        } catch (ParserConfigurationException ex) {
            throw new HummingException("failed", ex);
        } catch (SAXException ex) {
            throw new HummingException("failed", ex);
        } catch (IOException ex) {
            throw new HummingException("failed", ex);
        }
    }
    
    public void parseXMLDocument(Document document) throws HummingException {
        NodeList nodeList = document.getElementsByTagName("device");
        
        if (nodeList.getLength() != 1) {
            throw new HummingException("invalide device: " + document);
        }
        
        NodeList objectList = nodeList.item(0).getChildNodes();
        for (int i=0; i<objectList.getLength(); i++) {
            Node objectNode = objectList.item(i);
            if (objectNode.getNodeType() == Node.ELEMENT_NODE) {
                addXMLObject(objectList.item(i));
            }
        }
    }
    
    public void parseXMLFile(String filename) throws HummingException {
        parseXMLFile(new File(filename));
    }
    
    public void parseXMLFile(File file) throws HummingException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(file);
            parseXMLDocument(doc);
        } catch (ParserConfigurationException ex) {
            throw new HummingException("failed", ex);
        } catch (SAXException ex) {
            throw new HummingException("failed", ex);
        } catch (IOException ex) {
            throw new HummingException("failed", ex);
        }
    }
    
    public static void replaceSetGetRequestDispatcher(Core core) {
        RequestProcessor lastProcessor = core.getSetGetRequestProcessor();
        RequestProcessor newProcessor = new ParallelSetGetRequestProcessor(core.getLocalObjectManager());
        
        RequestDispatcher dispatcher = core.getRequestDispatcher();
        dispatcher.removeRequestProcessor(lastProcessor);
        dispatcher.addRequestProcessor(newProcessor);
    }
    
    public static void showUsage(String name) {
        System.out.println("Usage: " + name + " [ -i interface] [xmlfile...]");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws HummingException, SocketException, SubnetException, ParserConfigurationException, SAXException, TooManyObjectsException, IOException {
        
        Core core;
        int fileIndex;
        
        if (args.length > 0 && args[0].equals("-h")) {
            showUsage("humming");
            return;
        }
        
        if (args.length > 0 && args[0].equals("-i")) {
            NetworkInterface nif = NetworkInterface.getByName(args[1]);
            core = new Core(Inet4Subnet.startSubnet(nif));
            fileIndex = 2;
        } else {
            core = new Core();
            fileIndex = 0;
        }
        
        Humming humming = new Humming(core);
        
        humming.parseXMLString("<device><object ceoj=\"0011\"><property epc=\"E0\"><data type=\"const\">0123</data></property></object></device>");
        
        for (int i=fileIndex; i<args.length; i++) {
            humming.parseXMLFile(args[i]);
        }
        
        core.initialize();
        
        replaceSetGetRequestDispatcher(core);
        
        core.startService();
    }
}
