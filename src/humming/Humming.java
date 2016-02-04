package humming;

import echowand.common.EPC;
import echowand.info.DeviceObjectInfo;
import echowand.info.TemperatureSensorInfo;
import echowand.logic.RequestDispatcher;
import echowand.logic.RequestProcessor;
import echowand.logic.TooManyObjectsException;
import echowand.net.Inet4Subnet;
import echowand.net.InternalSubnet;
import echowand.net.Subnet;
import echowand.net.SubnetException;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.Core;
import echowand.service.ExtendedSubnet;
import echowand.service.LocalObjectConfig;
import echowand.service.NodeProfileObjectConfig;
import echowand.service.PropertyUpdater;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptEngine;
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
    private static final Logger LOGGER = Logger.getLogger(Humming.class.getName());
    private static final String CLASS_NAME = Humming.class.getName();
    
    private Core core;
    private PropertyDelegateFactory factory;
    private LinkedList<Node> nodes;
    private LinkedList<LocalObjectConfig> localObjectConfigs;
    private HummingScripting hummingScripting;
    
    public Humming(Core core) {
        this.core = core;
        nodes = new LinkedList<Node>();
        localObjectConfigs = new LinkedList<LocalObjectConfig>();
        hummingScripting = new HummingScripting();
        
        factory = new PropertyDelegateFactory();
        factory.add("const", new ConstPropertyDelegateCreator());
        factory.add("variable", new VariablePropertyDelegateCreator());
        factory.add("file", new FilePropertyDelegateCreator());
        factory.add("command", new CommandPropertyDelegateCreator());
        factory.add("proxy", new ProxyPropertyDelegateCreator(core));
        factory.add("delegate", new DelegatePropertyDelegateCreator(hummingScripting));
    }
    
    public <S extends Subnet> S getSubnet(Class<S> cls) {
        if (cls.isInstance(core.getSubnet())) {
            return cls.cast(core.getSubnet());
        } else if (core.getSubnet() instanceof ExtendedSubnet) {
            return ((ExtendedSubnet)core.getSubnet()).getSubnet(cls);
        } else {
            return null;
        }
    }
    
    public void setScriptEngine(ScriptEngine engine) {
        hummingScripting.setScriptEngine(engine);
    }
    
    public ScriptEngine getScriptEngine() {
        return hummingScripting.getScriptEngine();
    }
    
    public void setTemplateBindings(Bindings bindings) {
        hummingScripting.setTemplateBindings(bindings);
    }
    
    public Bindings getTemplateBindings() {
        return hummingScripting.getTemplateBindings();
    }
    
    public PropertyDelegateFactory getDelegateFactory() {
        return factory;
    }
    
    public PropertyDelegateCreator getPropertyDelegateCreator(String name) {
        return factory.get(name);
    }
    
    public PropertyDelegateCreator addPropertyDelegateCreator(String name, PropertyDelegateCreator creator) {
        return factory.add(name, creator);
    }
    
    public Core getCore() {
        return core;
    }
    
    public int countLocalObjectConfigs() {
        return localObjectConfigs.size();
    }
    
    public LocalObjectConfig getLocalObjectConfig(int index) {
        return localObjectConfigs.get(index);
    }
    
    public void parseObject(Node objectNode) throws HummingException {
        ObjectParser objectParser = new ObjectParser(factory, hummingScripting);
        objectParser.parse(objectNode);
        
        if (objectNode.getNodeName().equals("object")) {
            LocalObjectConfig config = new LocalObjectConfig(new DeviceObjectInfo());
            objectParser.apply(config);
            
            localObjectConfigs.add(config);
            core.addLocalObjectConfig(config);
            
            nodes.add(objectNode);
        } else if (objectNode.getNodeName().equals("profile")) {
            NodeProfileObjectConfig config = core.getNodeProfileObjectConfig();
            objectParser.apply(config);
            
            nodes.add(objectNode);
        } else {
            throw new HummingException("invalid object: " + objectNode.getNodeName());
        }
    }
    
    public void parseXML(String xmlString) throws HummingException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            parseDocument(builder.parse(new ByteArrayInputStream(xmlString.getBytes())));
        } catch (ParserConfigurationException ex) {
            throw new HummingException("failed", ex);
        } catch (SAXException ex) {
            throw new HummingException("failed", ex);
        } catch (IOException ex) {
            throw new HummingException("failed", ex);
        }
    }
    
    public void parseDocument(Document document) throws HummingException {
        NodeList nodeList = document.getElementsByTagName("device");
        
        if (nodeList.getLength() != 1) {
            throw new HummingException("invalide device: " + document);
        }
        
        NodeList objectList = nodeList.item(0).getChildNodes();
        for (int i=0; i<objectList.getLength(); i++) {
            Node objectNode = objectList.item(i);
            if (objectNode.getNodeType() == Node.ELEMENT_NODE) {
                parseObject(objectList.item(i));
            }
        }
    }
    
    public void loadXMLFile(String filename) throws HummingException {
        loadXMLFile(new File(filename));
    }
    
    public void loadXMLFile(File file) throws HummingException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(file);
            parseDocument(doc);
        } catch (ParserConfigurationException ex) {
            throw new HummingException("failed", ex);
        } catch (SAXException ex) {
            throw new HummingException("failed", ex);
        } catch (IOException ex) {
            throw new HummingException("failed", ex);
        }
    }
    
    public static Core createTestInternalCore(Humming humming) {
        try {
            Core peerCore = new Core(new InternalSubnet());
            TemperatureSensorInfo info = new TemperatureSensorInfo();
            info.add(EPC.x80, true, true, true, new byte[]{0x30});
            info.add(EPC.x81, true, true, true, new byte[]{0x00});
            info.add(EPC.xE0, true, true, true, 2);
            LocalObjectConfig config = new LocalObjectConfig(info);
            config.addPropertyUpdater(new PropertyUpdater() {
                @Override
                public void loop(LocalObject localObject) {
                    setIntervalPeriod(5000);
                    
                    ObjectData data = localObject.getData(EPC.xE0);
                    int num = ((int)(data.get(0) << 8))| data.get(1);
                    num++;
                    if (num > 300) {
                        num = -300;
                    }
                    
                    byte b0 = (byte)((num & 0xff00) >> 8);
                    byte b1 = (byte)(num & 0x00ff);
                    ObjectData newData = new ObjectData(b0, b1);
                    localObject.forceSetData(EPC.xE0, newData);
                }
            });
            peerCore.addLocalObjectConfig(config);
            peerCore.startService();
            
            Core internalCore = new Core(new InternalSubnet());
            internalCore.startService();
            
            ProxyPropertyDelegateCreator creator = (ProxyPropertyDelegateCreator)humming.getPropertyDelegateCreator("proxy");
            creator.addCore("internal", internalCore);
            
            return internalCore;
        } catch (TooManyObjectsException ex) {
            Logger.getLogger(Humming.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static void replaceSetGetRequestDispatcher(Core core) {
        RequestProcessor lastProcessor = core.getSetGetRequestProcessor();
        RequestProcessor newProcessor = new ThreadedSetGetRequestProcessor(core.getLocalObjectManager());
        
        RequestDispatcher dispatcher = core.getRequestDispatcher();
        dispatcher.removeRequestProcessor(lastProcessor);
        dispatcher.addRequestProcessor(newProcessor);
    }
    
    public static void showUsage(String name) {
        System.out.println("Usage: " + name + " [ -i interface ] [ xmlfile... ]");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws HummingException, SocketException, SubnetException, ParserConfigurationException, SAXException, TooManyObjectsException, IOException {
        
        Core core;
        int fileIndex;
        
        if (args.length > 0 && args[0].equals("-h")) {
            showUsage("Humming");
            return;
        }
        
        if (args.length > 0 && args[0].equals("-i")) {
            NetworkInterface nif;
            NetworkInterface[] rnifs = null;
            
            if (args[1].equals("-")) {
                nif = NetworkInterfaceSelector.select();
            } else {
                String[] names = args[1].split(",");
                nif = NetworkInterface.getByName(names[0]);
                
                if (names.length > 1) {
                    rnifs = new NetworkInterface[names.length - 1];
                    for (int i=1; i<names.length; i++) {
                        rnifs[i-1] = NetworkInterface.getByName(names[i]);
                    }
                }
            }
            
            if (rnifs == null) {
                core = new Core(Inet4Subnet.startSubnet(nif));
            } else {
                core = new Core(Inet4Subnet.startSubnet(nif, rnifs));
            }
            
            fileIndex = 2;
        } else {
            core = new Core();
            fileIndex = 0;
        }
        
        Humming humming = new Humming(core);
        
        Core internalCore = createTestInternalCore(humming);
        
        //humming.parseXMLString("<device><object ceoj=\"0011\"><property epc=\"E0\"><data type=\"const\">0123</data></property><property epc=\"E1\" set=\"enabled\"><data type=\"variable\">0123</data></property></object></device>");
        
        for (int i=fileIndex; i<args.length; i++) {
            humming.loadXMLFile(args[i]);
        }
        
        core.initialize();
        
        // ProxyPropertyDelegate doesn't work without this line 
        replaceSetGetRequestDispatcher(core);
        
        core.startService();
    }
}
