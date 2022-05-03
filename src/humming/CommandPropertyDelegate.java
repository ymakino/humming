package humming;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.Core;
import echowand.service.PropertyDelegate;
import static humming.CommandPropertyDelegateUtil.concatString;
import static humming.CommandPropertyDelegateUtil.epc2str;
import static humming.CommandPropertyDelegateUtil.joinString;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class CommandPropertyDelegate extends PropertyDelegate {
    private static final Logger LOGGER = Logger.getLogger(CommandPropertyDelegate.class.getName());
    private static final String CLASS_NAME = CommandPropertyDelegate.class.getName();
    
    private String[] getCommand;
    private String[] setCommand;
    private CommandPropertyDelegateNotifySender sender;
    
    public CommandPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, String[] getCommand, String[] setCommand) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        LOGGER.entering(CLASS_NAME, "CommandPropertyDelegate", new Object[]{epc, getEnabled, setEnabled, notifyEnabled, getCommand, setCommand});
        
        if (getCommand == null) {
            this.getCommand = null;
        } else {
            this.getCommand = Arrays.copyOf(getCommand, getCommand.length);
        }
        
        if (setCommand == null) {
            this.setCommand = null;
        } else {
            this.setCommand = Arrays.copyOf(setCommand, setCommand.length);
        }
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "CommandPropertyDelegate", "epc: " + epc + " -> get: " + joinString(getCommand) + ", set: " + joinString(setCommand));
        
        LOGGER.exiting(CLASS_NAME, "CommandPropertyDelegate");
    }
    
    public String[] getGetCommand() {
        LOGGER.entering(CLASS_NAME, "getGetCommand");
        
        String[] result = Arrays.copyOf(getCommand, getCommand.length);
        LOGGER.exiting(CLASS_NAME, "getGetCommand", result);
        return result;
    }
    
    public String[] getSetCommand() {
        LOGGER.entering(CLASS_NAME, "getSetCommand");
        
        String[] result = Arrays.copyOf(setCommand, setCommand.length);
        LOGGER.exiting(CLASS_NAME, "getSetCommand", result);
        return result;
    }
    
    public void setCommandPropertyDelegateNotifySender(CommandPropertyDelegateNotifySender sender) {
        LOGGER.entering(CLASS_NAME, "setCommandPropertyDelegateNotifySender", sender);
        
        this.sender = sender;
        
        LOGGER.exiting(CLASS_NAME, "setCommandPropertyDelegateNotifySender");
    }
    
    public CommandPropertyDelegateNotifySender getCommandPropertyDelegateNotifySender() {
        LOGGER.entering(CLASS_NAME, "getCommandPropertyDelegateNotifySender");
        
        LOGGER.exiting(CLASS_NAME, "getCommandPropertyDelegateNotifySender", sender);
        return sender;
    }
    
    @Override
    public void notifyCreation(LocalObject object, Core core) {
        LOGGER.entering(CLASS_NAME, "notifyCreation", new Object[]{object, core});
        
        if (sender != null) {
            sender.addProperty(object, getEPC());
            
            if (!sender.isAlive()) {
                sender.start();
            }
        }
        
        LOGGER.exiting(CLASS_NAME, "notifyCreation");
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        LOGGER.entering(CLASS_NAME, "getUserData", new Object[]{object, epc});
        
        Process proc;
        InputStreamReader reader;
        
        if (getCommand == null) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "get command is not set");
            LOGGER.exiting(CLASS_NAME, "getUserData", null);
            return null;
        }
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "begin: " + object + ", EPC: " + epc + " -> " + joinString(getCommand));
            proc = Runtime.getRuntime().exec(concatString(getCommand, object.getEOJ().toString(), epc2str(epc)));
            reader = new InputStreamReader(proc.getInputStream());
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(getCommand), ex);
            LOGGER.exiting(CLASS_NAME, "getUserData", null);
            return null;
        }
        
        try {
            ObjectData data = new ObjectData();
            char[] chars = new char[512];
            
            int len = reader.read(chars);
            
            if (len > 0) {
                String[] lines = String.valueOf(chars).split("[^0-9a-fA-F]");
                
                if (lines.length > 0) {
                    String line = lines[0];
                    len = line.length();

                    byte[] bytes = new byte[len/2];
                    for (int i=0; i<len-1; i+=2) {
                        bytes[i/2] = (byte)Integer.parseInt(line.substring(i, i+2), 16);
                    }

                    data = new ObjectData(bytes);
                }
            }
            
            int exitValue = proc.waitFor();
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "end: " + object + ", EPC: " + epc + " -> " + joinString(getCommand) + ", data: " + data + ", exit: " + exitValue);
            LOGGER.exiting(CLASS_NAME, "getUserData", data);
            return data;
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(getCommand), ex);
        } catch (NumberFormatException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(getCommand), ex);
        } catch (InterruptedException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(getCommand), ex);
        }
        
        return null;
    }

    @Override
    public boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        LOGGER.entering(CLASS_NAME, "setUserData", new Object[]{object, epc, data});
        
        Process proc;
        
        if (setCommand == null) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "set command is not set");
            LOGGER.exiting(CLASS_NAME, "setUserData", false);
            return false;
        }
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "begin: " + object + ", EPC: " + epc + " -> " + joinString(setCommand) + ", data: " + data);
            proc = Runtime.getRuntime().exec(concatString(setCommand, object.getEOJ().toString(), epc2str(epc), data.toString()));
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(setCommand) + ", data: " + data, ex);
            LOGGER.exiting(CLASS_NAME, "setUserData", false);
            return false;
        }
        
        try {
            int exitValue = proc.waitFor();
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "end: " + object + ", EPC: " + epc + " -> " + joinString(setCommand) + ", data: " + data + ", exit: " + exitValue);
            LOGGER.exiting(CLASS_NAME, "setUserData", (exitValue == 0));
            return (exitValue == 0);
        } catch (InterruptedException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(setCommand) + ", data: " + data, ex);
        }
        
        LOGGER.exiting(CLASS_NAME, "setUserData", false);
        return false;
    }
}
