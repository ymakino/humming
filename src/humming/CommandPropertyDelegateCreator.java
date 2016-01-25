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
    public static final String NOTIFY_TAG = "notify";
    
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
        String[] notifyCommand = null;
        int interval = -1;
        int delay = -1;
        
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node commandInfo = nodeList.item(i);
            
            if (commandInfo.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            String infoName = commandInfo.getNodeName();
            if (infoName.equals(GET_TAG)) {
                getCommand = splitString(commandInfo.getTextContent());
            } else if (infoName.equals(SET_TAG)) {
                setCommand = splitString(commandInfo.getTextContent());
            } else if (infoName.equals(NOTIFY_TAG)) {
                notifyCommand = splitString(commandInfo.getTextContent());
                Node intervalNode = commandInfo.getAttributes().getNamedItem("interval");
                Node delayNode = commandInfo.getAttributes().getNamedItem("delay");
                
                if (intervalNode != null) {
                    String intervalString = intervalNode.getNodeValue();
                    try {
                        interval = Integer.parseInt(intervalString);
                    } catch (NumberFormatException ex) {
                        LOGGER.logp(Level.WARNING, CLASS_NAME, "newPropertyDelegate", "invalid interval value: " + intervalString, ex);
                    }
                }
                
                if (delayNode != null) {
                    String delayString = delayNode.getNodeValue();
                    try {
                        delay = Integer.parseInt(delayString);
                    } catch (NumberFormatException ex) {
                        LOGGER.logp(Level.WARNING, CLASS_NAME, "newPropertyDelegate", "invalid delay number: " + delayString, ex);
                    }
                }
            } else {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "invalid element: " + infoName);
            }
        }
        
        if (getCommand != null || setCommand != null || notifyCommand != null) {
            CommandPropertyDelegate delegate = new CommandPropertyDelegate(epc, getEnabled, setEnabled, notifyEnabled, getCommand, setCommand);
            
            if (notifyCommand != null) {
                CommandPropertyDelegateNotifySender notifySender = new CommandPropertyDelegateNotifySender(notifyCommand);
            
                if (interval >= 0) {
                    notifySender.setInterval(interval);
                }

                if (delay >= 0) {
                    notifySender.setDelay(delay);
                }
                
                delegate.setCommandPropertyDelegateNotifySender(notifySender);
            }
            
            return delegate;
        } else {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "parseProperty", "there are no commands");
        }
        
        return null;
    }
}