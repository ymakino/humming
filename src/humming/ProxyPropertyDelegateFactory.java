package humming;

import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.net.InetNodeInfo;
import echowand.net.NodeInfo;
import echowand.service.Core;
import echowand.service.PropertyDelegate;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class ProxyPropertyDelegateFactory extends PropertyDelegateFactory {
    private static final Logger logger = Logger.getLogger(ProxyPropertyDelegateFactory.class.getName());
    private static final String className = ProxyPropertyDelegateFactory.class.getName();
    
    private Core core;
    
    public ProxyPropertyDelegateFactory(Core core) {
        this.core = core;
    }
    
    @Override
    public PropertyDelegate newPropertyDelegate(EPC epc, Node node) {
        NodeInfo proxyNode = null;
        EOJ proxyEOJ = null;
        EPC proxyEPC = null;
        
        try {
            NodeList nodeList = node.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node proxyInfo = nodeList.item(i);
                String infoName = proxyInfo.getNodeName().toLowerCase();
                if (infoName.equals("addr")) {
                    InetAddress addr = InetAddress.getByName(proxyInfo.getTextContent());
                    proxyNode = new InetNodeInfo(addr);
                } else if (infoName.equals("eoj")) {
                    proxyEOJ = new EOJ(proxyInfo.getTextContent());
                } else if (infoName.equals("epc")) {
                    String epcName = proxyInfo.getTextContent();
                    if (epcName.toLowerCase().startsWith("0x")) {
                        epcName = epcName.substring(2);
                    }
                    
                    byte b = (byte)Integer.parseInt(epcName, 16);
                    proxyEPC = EPC.fromByte(b);
                }
            }
        } catch (UnknownHostException ex) {
            Logger.getLogger(ProxyPropertyDelegateFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (proxyNode != null && proxyEOJ != null && proxyEPC != null) {
            return new ProxyPropertyDelegate(core, epc, proxyNode, proxyEOJ, proxyEPC);
        }
        
        return null;
    }
}
