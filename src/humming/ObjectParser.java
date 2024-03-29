package humming;

import echowand.common.ClassEOJ;
import echowand.common.Data;
import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.info.BasicObjectInfo;
import echowand.info.PropertyInfo;
import echowand.net.NodeInfo;
import echowand.net.SubnetException;
import echowand.object.LocalObjectDelegate;
import echowand.service.Core;
import echowand.service.LocalObjectConfig;
import echowand.service.PropertyDelegate;
import echowand.service.PropertyUpdater;
import echowand.util.ConstraintAny;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class ObjectParser {
    private static final Logger LOGGER = Logger.getLogger(ObjectParser.class.getName());
    private static final String CLASS_NAME = ObjectParser.class.getName();
    
    private Humming humming;
    private PropertyDelegateFactory delegateFactory;
    
    private ClassEOJ classEOJ;
    private LinkedList<PropertyInfo> propertyInfos;
    private LinkedList<LocalObjectDelegate> delegates;
    private LinkedList<PropertyDelegate> propertyDelegates;
    private LinkedList<PropertyUpdater> propertyUpdaters;
    
    private HummingScripting hummingScripting;
    
    private void parseClassEOJ(Node ceojNode) throws HummingException {
        classEOJ = new ClassEOJ(ceojNode.getTextContent());
        LOGGER.logp(Level.INFO, CLASS_NAME, "parseClassEOJ", "ClassEOJ: " + classEOJ);
    }
    
    private String toNodeString(Node node, boolean enableAttributes) {
        
        if (node.getNodeType() == Node.COMMENT_NODE) {
            return "";
        }
        
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return node.getTextContent().trim();
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append("<" + node.getNodeName());
        
        if (enableAttributes) {
            for (int i=0; i<node.getAttributes().getLength(); i++) {
                Node attr = node.getAttributes().item(i);
                builder.append(" " + attr.getNodeName() + "=\"" + attr.getTextContent() + "\"");
            }
        }
        
        StringBuilder childrenBuilder = new StringBuilder();
        for (int i=0; i<node.getChildNodes().getLength(); i++) {
            Node child = node.getChildNodes().item(i);
            childrenBuilder.append(toNodeString(child, true));
        }
        
        if (childrenBuilder.length() == 0) {
            builder.append("/>");
        } else {
            builder.append(">");
            builder.append(childrenBuilder);
            builder.append("</" + node.getNodeName() + ">");
        }
        
        return builder.toString();
    }
    
    private String toInfoString(Node node) {
        if (!node.getNodeName().equals("data")) {
            return toNodeString(node, true);
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        
        for (int i=0; i<node.getChildNodes().getLength(); i++) {
            Node child = node.getChildNodes().item(i);
            builder.append(toNodeString(child, true));
        }
        
        builder.append("]");
        return builder.toString();
    }
    
    private void parseProperty(Node propNode) throws HummingException {
        NodeList propInfoList = propNode.getChildNodes();
        
        Node epcNode = propNode.getAttributes().getNamedItem("epc");
        String epcStr = epcNode.getTextContent().toLowerCase();
        if (epcStr.startsWith("0x")) {
            epcStr = epcStr.substring(2);
        }
        byte b = (byte)Integer.parseInt(epcStr, 16);
        EPC epc = EPC.fromByte(b);
        
        if (epc.isInvalid()) {
            throw new HummingException("invalid EPC: " + epcStr);
        }
        
        boolean getEnabled = true;
        boolean setEnabled = false;
        boolean notifyEnabled = false;
        byte[] dataBytes = new byte[]{0x00};
        boolean dataSizeFixed = false;
        
        Node setNode = propNode.getAttributes().getNamedItem("set");
        if (setNode != null) {
            setEnabled = setNode.getTextContent().equals("enabled");
        }
        
        Node getNode = propNode.getAttributes().getNamedItem("get");
        if (getNode != null) {
            getEnabled = getNode.getTextContent().equals("enabled");
        }
        
        Node notifyNode = propNode.getAttributes().getNamedItem("notify");
        if (notifyNode != null) {
            notifyEnabled = notifyNode.getTextContent().equals("enabled");
        }
        
        Node valueNode = propNode.getAttributes().getNamedItem("value");
        if (valueNode != null) {
            String valueStr = valueNode.getTextContent();
            
            if (valueStr.length() == 0 || (valueStr.length() % 2) != 0) {
                throw new HummingException("invalid value: " + valueStr);
            }
            
            dataBytes = new byte[valueStr.length() / 2];
            dataSizeFixed = true;
            
            for (int i=0; i<valueStr.length(); i+=2) {
                String numStr = valueStr.substring(i, i+2);
                int num = Integer.parseInt(numStr, 16);
                dataBytes[i/2] = (byte)num;
            }
        }
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "parseProperty", "property: ClassEOJ: " + classEOJ + ", EPC: " + epc + ", GET: " + getEnabled + ", SET: " + setEnabled + ", Notify: " + notifyEnabled + ", data: " + new Data(dataBytes));
        
        for (int i=0; i < propInfoList.getLength(); i++) {
            Node propInfo = propInfoList.item(i);
            String infoName = propInfo.getNodeName();
            
            if (propInfo.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            PropertyDelegate propertyDelegate = delegateFactory.newPropertyDelegate(infoName, classEOJ, epc, getEnabled, setEnabled, notifyEnabled, propInfo);
            if (propertyDelegate != null) {
                propertyDelegates.add(propertyDelegate);
                LOGGER.logp(Level.INFO, CLASS_NAME, "parseProperty", "delegate: " + propertyDelegate + ", type: " + infoName + ", ClassEOJ: " + classEOJ + ", EPC: " + epc + ", GET: " + getEnabled + ", SET: " + setEnabled + ", Notify: " + notifyEnabled + ", info: " + toInfoString(propInfo));
            } else {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "unknown type: " + infoName);
            }
        }
        
        if (dataSizeFixed) {
            propertyInfos.add(new PropertyInfo(epc, getEnabled, setEnabled, notifyEnabled, dataBytes));
        } else {
            propertyInfos.add(new PropertyInfo(epc, getEnabled, setEnabled, notifyEnabled, dataBytes, new ConstraintAny()));
        }
    }
    
    private <C> C createObject(String className, HashMap<String, String> params) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        LOGGER.entering(CLASS_NAME, "createObject", new Object[]{className, params});
        
        Class cls = Class.forName(className);
        
        C propertyUpdater;
        
        if (!params.isEmpty()) {
            Constructor constructor = cls.getConstructor(HashMap.class);
            propertyUpdater = (C)constructor.newInstance(params);
            LOGGER.logp(Level.INFO, CLASS_NAME, "createObject", className + ": constructor with params");
        } else {
            try {
                Constructor constructor = cls.getConstructor(HashMap.class);
                propertyUpdater = (C)constructor.newInstance(params);
                LOGGER.logp(Level.INFO, CLASS_NAME, "createObject", className + ": constructor with params");
            } catch (NoSuchMethodException ex) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "createObject", className + ": no constructor found with params");
                Constructor constructor = cls.getConstructor();
                propertyUpdater = (C)constructor.newInstance();
                LOGGER.logp(Level.INFO, CLASS_NAME, "createObject", className + ": constructor without params");
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "createObject", propertyUpdater);
        return propertyUpdater;
    }
    
    private PropertyUpdater createPropertyUpdater(String className, HashMap<String, String> params) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        LOGGER.entering(CLASS_NAME, "createPropertyUpdater", new Object[]{className, params});
        
        PropertyUpdater result = createObject(className, params);
        LOGGER.exiting(CLASS_NAME, "createPropertyUpdater", result);
        return result;
    }
    
    private LocalObjectDelegate createLocalObjectDelegate(String className, HashMap<String, String> params) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        LOGGER.entering(CLASS_NAME, "createLocalObjectDelegate", new Object[]{className, params});
        
        LocalObjectDelegate result = createObject(className, params);
        LOGGER.exiting(CLASS_NAME, "createLocalObjectDelegate", result);
        return result;
    }
    
    private void parseUpdater(Node propNode) throws HummingException, IllegalArgumentException {
        LOGGER.entering(CLASS_NAME, "parseUpdater", propNode);
                
        InstanceCreatorParser parser = new InstanceCreatorParser(propNode);
        Node delayNode = propNode.getAttributes().getNamedItem("delay");
        Node intervalNode = propNode.getAttributes().getNamedItem("interval");
        
        try {
            PropertyUpdater propertyUpdater = createPropertyUpdater(parser.getClassName(), parser.getParams());
            
            String msg = "class: " +  parser.getClassName();
            
            if (delayNode != null) {
                int delay = Integer.parseInt(delayNode.getTextContent());
                propertyUpdater.setDelay(delay);
                msg += ", delay: " + delay;
            }
            
            if (intervalNode != null ) {
                int interval = Integer.parseInt(intervalNode.getTextContent());
                propertyUpdater.setIntervalPeriod(interval);
                msg += ", interval: " + interval;
            }
            
            LOGGER.logp(Level.INFO, CLASS_NAME, "parseUpdater", msg);
            
            if (parser.getScript() != null) {
                Bindings bindings = hummingScripting.createBindings();
                bindings.put(parser.getInstanceName(), propertyUpdater);
                hummingScripting.getScriptEngine().eval(parser.getScript(), bindings);
            }
            
            propertyUpdaters.add(propertyUpdater);
        } catch (ClassNotFoundException ex) {
            throw new HummingException("cannot create PropertyUpdater", ex);
        } catch (InstantiationException ex) {
            throw new HummingException("cannot create PropertyUpdater", ex);
        } catch (IllegalAccessException ex) {
            throw new HummingException("cannot create PropertyUpdater", ex);
        } catch (NumberFormatException ex) {
            throw new HummingException("cannot create PropertyUpdater", ex);
        } catch (ScriptException ex) {
            throw new HummingException("cannot create PropertyUpdater", ex);
        } catch (NoSuchMethodException ex) {
            throw new HummingException("cannot create PropertyUpdater", ex);
        } catch (InvocationTargetException ex) {
            throw new HummingException("cannot create PropertyUpdater", ex);
        }
        
        LOGGER.exiting(CLASS_NAME, "parseUpdater");
    }
    
    private void parseDelegate(Node propNode) throws HummingException {
        LOGGER.entering(CLASS_NAME, "parseDelegate", propNode);
        
        InstanceCreatorParser parser = new InstanceCreatorParser(propNode);
        
        try {
            LocalObjectDelegate delegate = createLocalObjectDelegate(parser.getClassName(), parser.getParams());
            
            String msg = "class: " +  parser.getClassName();
            
            LOGGER.logp(Level.INFO, CLASS_NAME, "parseDelegate", msg);
            
            if (parser.getScript() != null) {
                Bindings bindings = hummingScripting.createBindings();
                bindings.put(parser.getInstanceName(), delegate);
                hummingScripting.getScriptEngine().eval(parser.getScript(), bindings);
            }
            
            delegates.add(delegate);
        } catch (ClassNotFoundException ex) {
            throw new HummingException("cannot create LocalObjectDelegate", ex);
        } catch (InstantiationException ex) {
            throw new HummingException("cannot create LocalObjectDelegate", ex);
        } catch (IllegalAccessException ex) {
            throw new HummingException("cannot create LocalObjectDelegate", ex);
        } catch (NumberFormatException ex) {
            throw new HummingException("cannot create LocalObjectDelegate", ex);
        } catch (ScriptException ex) {
            throw new HummingException("cannot create LocalObjectDelegate", ex);
        } catch (NoSuchMethodException ex) {
            throw new HummingException("cannot create LocalObjectDelegate", ex);
        } catch (SecurityException ex) {
            throw new HummingException("cannot create LocalObjectDelegate", ex);
        } catch (IllegalArgumentException ex) {
            throw new HummingException("cannot create LocalObjectDelegate", ex);
        } catch (InvocationTargetException ex) {
            throw new HummingException("cannot create LocalObjectDelegate", ex);
        }
        
        LOGGER.exiting(CLASS_NAME, "parseDelegate");
    }
    
    private NodeInfo parseNodeInfo(Core core, Node node) throws SubnetException {
        LOGGER.entering(CLASS_NAME, "parseNodeInfo", new Object[]{core, node});
        
        NodeInfo result = core.getSubnet().getRemoteNode(node.getTextContent()).getNodeInfo();
        LOGGER.exiting(CLASS_NAME, "parseNodeInfo", result);
        return result;
    }
    
    private EOJ parseEOJInfo(Node node) throws SubnetException {
        LOGGER.entering(CLASS_NAME, "parseEOJInfo", node);
        
        EOJ result = new EOJ(node.getTextContent());
        LOGGER.exiting(CLASS_NAME, "parseEOJInfo", result);
        return result;
    }
    
    private EOJ parseInstanceInfo(Node node, ClassEOJ ceoj) throws SubnetException {
        LOGGER.entering(CLASS_NAME, "parseInstanceInfo", new Object[]{node, ceoj});
        
        String instanceStr = node.getTextContent();
        byte instanceCode = (byte)Integer.parseInt(instanceStr);
        EOJ result = ceoj.getEOJWithInstanceCode(instanceCode);
        LOGGER.exiting(CLASS_NAME, "parseInstanceInfo", result);
        return result;
    }
    
    private void parseProxy(Node propNode) throws HummingException {
        LOGGER.entering(CLASS_NAME, "parseProxy", propNode);
        
        Core proxyCore;
        NodeInfo proxyNode = null;
        EOJ proxyEOJ = null;
        boolean objectMode = true;
        
        Node modeNode = propNode.getAttributes().getNamedItem("mode");
        
        if (modeNode != null) {
            String mode = modeNode.getTextContent().toLowerCase();
            if (mode.equals("object")) {
                objectMode = true;
            } else if (mode.equals("frame")) {
                objectMode = false;
            } else {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProxy", "unknown mode: " + modeNode.getTextContent());
            }
        }
        
        try {
            Node proxySubnetInfo = null;
            Node proxyNodeInfo = null;
            Node proxyEOJInfo = null;
            Node proxyInstanceInfo = null;
        
            NodeList nodeList = propNode.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node proxyInfo = nodeList.item(i);
            
                if (proxyInfo.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
            
                String infoName = proxyInfo.getNodeName();
                if (infoName.equals("subnet")) {
                    proxySubnetInfo = proxyInfo;
                } else if (infoName.equals("node")) {
                    proxyNodeInfo = proxyInfo;
                } else if (infoName.equals("eoj")) {
                    proxyEOJInfo = proxyInfo;
                } else if (infoName.equals("instance")) {
                    proxyInstanceInfo = proxyInfo;
                } else {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProxy", "invalid element: " + infoName);
                }
            }
            
            proxyCore = humming.getCore();
            if (proxySubnetInfo != null) {
                String subnetName = proxySubnetInfo.getTextContent();
                proxyCore = humming.getCore(subnetName);
         
                if (proxyCore == null) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProxy", "invalid subnet: " + subnetName);
                    throw new HummingException("invalid subnet: " + subnetName);
                }
            }
            
            if (proxyNodeInfo != null) {
                proxyNode = parseNodeInfo(proxyCore, proxyNodeInfo);
            }
            
            if (proxyInstanceInfo != null) {
                proxyEOJ = parseInstanceInfo(proxyInstanceInfo, classEOJ);
            }
            
            if (proxyEOJInfo != null) {
                proxyEOJ = parseEOJInfo(proxyEOJInfo);
            }
            
        } catch (SubnetException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProxy", "failed", ex);
            throw new HummingException("failed", ex);
        }
        
        if (proxyNode == null) {
            String errorMessage = "invalid proxy information: Node: " + proxyNode;
            LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProxy", errorMessage);
            throw new HummingException(errorMessage);
        }
        
        for (EPC epc : EPC.values()) {
            propertyInfos.add(new PropertyInfo(epc, true, true, true, new byte[1], new ConstraintAny()));
        }
            
        LOGGER.logp(Level.INFO, CLASS_NAME, "parseProxy", "ClassEOJ: " + classEOJ + " -> proxyNode: " + proxyNode + ", proxyEOJ: " + proxyEOJ);
        
        if (objectMode) {
            ProxyObjectDelegate objectDelegate = new ProxyObjectDelegate(proxyCore, proxyNode, proxyEOJ);
            LOGGER.logp(Level.INFO, CLASS_NAME, "parseProxy", "delegate: " + objectDelegate + ", ClassEOJ: " + classEOJ + ", proxyNode: " + proxyNode + ", proxyEOJ: " + proxyEOJ);
            delegates.add(objectDelegate);
        } else {
            ProxyRequestProcessorDelegate processorDelegate = new ProxyRequestProcessorDelegate(proxyCore, proxyNode, proxyEOJ);
            LOGGER.logp(Level.INFO, CLASS_NAME, "parseProxy", "delegate: " + processorDelegate + ", ClassEOJ: " + classEOJ + ", proxyNode: " + proxyNode + ", proxyEOJ: " + proxyEOJ);
            delegates.add(processorDelegate);
        }
        
        LOGGER.exiting(CLASS_NAME, "parseProxy");
    }
    
    private void parseObject(Node objectNode) throws HummingException {
        LOGGER.entering(CLASS_NAME, "parseObject", objectNode);
        
        Node ceojNode = objectNode.getAttributes().getNamedItem("ceoj");
        
        if (ceojNode != null) {
            parseClassEOJ(ceojNode);
        }
        
        NodeList nodes = objectNode.getChildNodes();
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String nodeName = node.getNodeName();
            
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            if (nodeName.equals("property")) {
                parseProperty(node);
            } else if (nodeName.equals("proxy")) {
                parseProxy(node);
            } else if (nodeName.equals("updater")) {
                parseUpdater(node);
            } else if (nodeName.equals("delegate")) {
                parseDelegate(node);
            } else {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "parseObject", "invalid XML node: " + nodeName);
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "parseObject");
    }

    public ObjectParser(Humming humming, PropertyDelegateFactory factory, HummingScripting hummingScripting) throws HummingException {
        LOGGER.entering(CLASS_NAME, "ObjectParser", new Object[]{humming, factory, hummingScripting});
        
        this.humming = humming;
        delegateFactory = factory;
        this.hummingScripting = hummingScripting;
        
        delegates = new LinkedList<LocalObjectDelegate>();
        propertyInfos = new LinkedList<PropertyInfo>();
        propertyDelegates = new LinkedList<PropertyDelegate>();
        propertyUpdaters = new LinkedList<PropertyUpdater>();
        
        LOGGER.exiting(CLASS_NAME, "ObjectParser");
    }
    
    public void parse(Node objectNode) throws HummingException {
        LOGGER.entering(CLASS_NAME, "parse", objectNode);
        
        parseObject(objectNode);
        
        LOGGER.exiting(CLASS_NAME, "parse");
    }
     
    public void apply(LocalObjectConfig config) throws HummingException {
        LOGGER.entering(CLASS_NAME, "apply", config);
        
        BasicObjectInfo info = BasicObjectInfo.class.cast(config.getObjectInfo());
        
        if (info == null && (classEOJ != null || propertyInfos.size() > 0)) {
            throw new HummingException("cannot update ObjectInfo: " + config.getObjectInfo());
        }
        
        if (classEOJ != null) {
            info.setClassEOJ(classEOJ);
        }
        
        for (PropertyInfo propertyInfo : propertyInfos) {
            info.add(propertyInfo);
        }
        
        for (LocalObjectDelegate delegate : delegates) {
            config.addDelegate(delegate);
        }
        
        for (PropertyDelegate propertyDelegate : propertyDelegates) {
            config.addPropertyDelegate(propertyDelegate);
        }
        
        for (PropertyUpdater propertyUpdater : propertyUpdaters) {
            config.addPropertyUpdater(propertyUpdater);
        }
        
        LOGGER.exiting(CLASS_NAME, "apply");
    }
}