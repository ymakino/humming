package humming;

import echowand.logic.TooManyObjectsException;
import echowand.net.Inet4Subnet;
import echowand.net.SubnetException;
import echowand.service.Core;
import echowand.service.LocalObjectConfig;
import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.LinkedList;
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
    private Core core;
    private LinkedList<Node> nodes;
    private LinkedList<LocalObjectConfig> configs;
    
    public Humming(Core core) {
        this.core = core;
        nodes = new LinkedList<Node>();
        configs = new LinkedList<LocalObjectConfig>();
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
        PropertyDelegateFactory factory = PropertyDelegateFactory.getInstance();
        factory.add("const", new ConstPropertyDelegateFactory());
        factory.add("file", new FilePropertyDelegateFactory());
        factory.add("proxy", new ProxyPropertyDelegateFactory(core));
        
        NodeList deviceList = doc.getElementsByTagName("devices").item(0).getChildNodes();
        for (int i=0; i<deviceList.getLength(); i++) {
            Node deviceNode = deviceList.item(i);
            if (deviceNode.getNodeName().equals("device")) {
                nodes.add(deviceNode);
                
                // System.out.println(deviceNode);
                XMLObjectConfigCreator creator = new XMLObjectConfigCreator(deviceNode);
                LocalObjectConfig config = creator.getConfig();
                configs.add(config);
                // System.out.println(config.getObjectInfo().getClassEOJ());
                
                core.addLocalObjectConfig(config);
            }
        }
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
        
        String filename = "/tmp/hoge.xml";
        if (args.length > fileIndex) {
            filename = args[fileIndex];
        }
        
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new File(filename));
        
        Humming humming = new Humming(core);
        humming.addXMLDocument(doc);
        
        core.startService();
    }
}
