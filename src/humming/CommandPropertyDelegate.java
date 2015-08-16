package humming;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyDelegate;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class CommandPropertyDelegate extends PropertyDelegate {
    private static final Logger LOGGER = Logger.getLogger(CommandPropertyDelegate.class.getName());
    private static final String CLASS_NAME = CommandPropertyDelegate.class.getName();
    
    private String getCommand;
    private String setCommand;
    
    public CommandPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, String getCommand, String setCommand) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        this.getCommand = getCommand;
        this.setCommand = setCommand;
        LOGGER.logp(Level.INFO, CLASS_NAME, "CommandPropertyDelegate", "epc: " + epc + " -> get: " + getCommand + ", set: " + setCommand);
    }
    
    public String getGetCommand() {
        return getCommand;
    }
    
    public String getSetCommand() {
        return setCommand;
    }
    
    private String epc2str(EPC epc) {
        return epc.toString().substring(1);
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
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "begin: " + object + ", EPC: " + epc + " -> " + getCommand);
            proc = Runtime.getRuntime().exec(new String[]{getCommand, object.getEOJ().toString(), epc2str(epc)});
            reader = new InputStreamReader(proc.getInputStream());
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + getCommand, ex);
            return null;
        }
        
        try {
            ObjectData data = new ObjectData();
            char[] chars = new char[256];
            
            int len = reader.read(chars);
            
            if (len > 0) {
                String line = String.valueOf(chars).split("[^0-9a-fA-F]")[0];
                len = line.length();
                
                byte[] bytes = new byte[len/2];
                for (int i=0; i<len-1; i+=2) {
                    bytes[i/2] = (byte)Integer.parseInt(line.substring(i, i+2), 16);
                }
                
                data = new ObjectData(bytes);
            }
            
            int exitValue = proc.waitFor();
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "end: " + object + ", EPC: " + epc + " -> " + getCommand + ", data: " + data + ", exit: " + exitValue);
            return data;
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + getCommand, ex);
        } catch (NumberFormatException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + getCommand, ex);
        } catch (InterruptedException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + getCommand, ex);
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
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "begin: " + object + ", EPC: " + epc + " -> " + setCommand + ", data: " + data);
            proc = Runtime.getRuntime().exec(new String[]{setCommand, object.getEOJ().toString(), epc2str(epc), data.toString()});
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed: " + object + ", EPC: " + epc + " -> " + setCommand + ", data: " + data, ex);
            return false;
        }
        
        try {
            int exitValue = proc.waitFor();
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "end: " + object + ", EPC: " + epc + " -> " + setCommand + ", data: " + data + ", exit: " + exitValue);
            return (exitValue == 0);
        } catch (InterruptedException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed: " + object + ", EPC: " + epc + " -> " + setCommand + ", data: " + data, ex);
        }
        
        return false;
    }
}

