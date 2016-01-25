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
    }
    
    public String[] getGetCommand() {
        return Arrays.copyOf(getCommand, getCommand.length);
    }
    
    public String[] getSetCommand() {
        return Arrays.copyOf(setCommand, setCommand.length);
    }
    
    public void setCommandPropertyDelegateNotifySender(CommandPropertyDelegateNotifySender sender) {
        this.sender = sender;
    }
    
    public CommandPropertyDelegateNotifySender getCommandPropertyDelegateNotifySender() {
        return sender;
    }
    
    @Override
    public void notifyCreation(LocalObject object, Core core) {
        if (sender != null) {
            sender.addProperty(object, getEPC());
            
            if (!sender.isAlive()) {
                sender.start();
            }
        }
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        Process proc;
        InputStreamReader reader;
        
        if (getCommand == null) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "get command is not set");
            return null;
        }
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "begin: " + object + ", EPC: " + epc + " -> " + joinString(getCommand));
            proc = Runtime.getRuntime().exec(concatString(getCommand, object.getEOJ().toString(), epc2str(epc)));
            reader = new InputStreamReader(proc.getInputStream());
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(getCommand), ex);
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
        Process proc;
        
        if (setCommand == null) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "set command is not set");
            return false;
        }
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "begin: " + object + ", EPC: " + epc + " -> " + joinString(setCommand) + ", data: " + data);
            proc = Runtime.getRuntime().exec(concatString(setCommand, object.getEOJ().toString(), epc2str(epc), data.toString()));
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(setCommand) + ", data: " + data, ex);
            return false;
        }
        
        try {
            int exitValue = proc.waitFor();
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "end: " + object + ", EPC: " + epc + " -> " + joinString(setCommand) + ", data: " + data + ", exit: " + exitValue);
            return (exitValue == 0);
        } catch (InterruptedException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(setCommand) + ", data: " + data, ex);
        }
        
        return false;
    }
}
