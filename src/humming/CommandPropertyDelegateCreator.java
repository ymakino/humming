package humming;

import echowand.common.ClassEOJ;
import echowand.common.EPC;
import echowand.service.PropertyDelegate;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author ymakino
 */
public class CommandPropertyDelegateCreator implements PropertyDelegateCreator {
    private static final Logger LOGGER = Logger.getLogger(CommandPropertyDelegateCreator.class.getName());
    private static final String CLASS_NAME = CommandPropertyDelegateCreator.class.getName();
    
    public static final String GET_TAG = "get";
    public static final String SET_TAG = "set";
    
    private String[] splitString(String str) {
        LinkedList<String> list = new LinkedList<String>();
        StringBuilder builder = new StringBuilder();
        boolean inText = false;
        boolean inDoubleQuote = false;
        boolean inSingleQuote = false;
        boolean escape = false;
        
        for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            
            if (escape) {
                switch (c) {
                    case 'r':
                        builder.append('\r');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    default:
                        builder.append(c);
                        break;
                }
                escape = false;
                continue;
            }
            
            switch (c) {
                case '"':
                    inText = true;
                    if (!inSingleQuote) {
                        inDoubleQuote = !inDoubleQuote;
                    } else {
                        builder.append(c);
                    }
                    break;
                case '\'':
                    inText = true;
                    if (!inDoubleQuote) {
                        inSingleQuote = !inSingleQuote;
                    } else {
                        builder.append(c);
                    }
                    break;
                case '\\':
                    inText = true;
                    if (!inSingleQuote) {
                        escape = true;
                    } else {
                        builder.append(c);
                    }
                    break;
                case ' ': case '\t': case '\r': case '\n':
                    if (inText) {
                        if (inDoubleQuote || inSingleQuote) {
                            builder.append(c);
                        } else {
                            list.add(builder.toString());
                            builder = new StringBuilder();
                            inText = false;
                        }
                    }
                    break;
                default:
                    inText = true;
                    builder.append(c);
                    break;
            }
        }
        
        if (escape) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "invalid backslash: " + str);
            return null;
        }
        
        if (inSingleQuote) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "unclosed single quote: " + str);
            return null;
        }
        
        if (inDoubleQuote) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "unclosed double quote: " + str);
            return null;
        }
        
        if (inText) {
            list.add(builder.toString());
        }
        
        return list.toArray(new String[list.size()]);
    }
    
    @Override
    public PropertyDelegate newPropertyDelegate(ClassEOJ ceoj, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, Node node) {
        String[] getCommand = null;
        String[] setCommand = null;
        
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node commandInfo = nodeList.item(i);
            String infoName = commandInfo.getNodeName();
            if (infoName.equals(GET_TAG)) {
                getCommand = splitString(commandInfo.getTextContent());
            } else if (infoName.equals(SET_TAG)) {
                setCommand = splitString(commandInfo.getTextContent());
            } else {
                short nodeType = commandInfo.getNodeType();
                if (nodeType != Node.TEXT_NODE && nodeType != Node.COMMENT_NODE) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "invalid element: " + infoName);
                }
            }
        }
        
        if (getCommand != null || setCommand != null) {
            return new CommandPropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, getCommand, setCommand);
        } else {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "there are no commands");
        }
        
        return null;
    }
}