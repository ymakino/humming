package humming;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyDelegate;
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
    
    public CommandPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, String[] getCommand, String[] setCommand) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        this.getCommand = getCommand;
        this.setCommand = setCommand;
        LOGGER.logp(Level.INFO, CLASS_NAME, "CommandPropertyDelegate", "epc: " + epc + " -> get: " + join(getCommand) + ", set: " + join(setCommand));
    }
    
    public String[] getGetCommand() {
        return Arrays.copyOf(getCommand, getCommand.length);
    }
    
    public String[] getSetCommand() {
        return Arrays.copyOf(setCommand, setCommand.length);
    }
    
    private String epc2str(EPC epc) {
        return String.format("%02X", 0x00ff & epc.toByte());
    }
    
    private String[] append(String[] array, String... strings) {
        String[] ret = new String[array.length + strings.length];
        System.arraycopy(array, 0, ret, 0, array.length);
        System.arraycopy(strings, 0, ret, array.length, strings.length);
        return ret;
    }
    
    private String escape(String str) {
        StringBuilder builder = new StringBuilder();
        
        for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            
            switch (c) {
                case '"': case '\'': case '\\':
                    builder.append("\\").append(c);
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                default:
                    builder.append(c);
                    break;
            }
        }
        
        return builder.toString();
    }
    
    private String join(String[] array) {
        if (array == null) {
            return null;
        }
        
        StringBuilder builder = new StringBuilder();
        
        for (int i=0; i<array.length; i++) {
            if (i != 0) {
                builder.append(" ");
            }
            builder.append('"').append(escape(array[i])).append('"');
        }
        
        return builder.toString();
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
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "begin: " + object + ", EPC: " + epc + " -> " + join(getCommand));
            proc = Runtime.getRuntime().exec(append(getCommand, object.getEOJ().toString(), epc2str(epc)));
            reader = new InputStreamReader(proc.getInputStream());
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + join(getCommand), ex);
            return null;
        }
        
        try {
            ObjectData data = new ObjectData();
            char[] chars = new char[512];
            
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
            LOGGER.logp(Level.INFO, CLASS_NAME, "getUserData", "end: " + object + ", EPC: " + epc + " -> " + join(getCommand) + ", data: " + data + ", exit: " + exitValue);
            return data;
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + join(getCommand), ex);
        } catch (NumberFormatException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + join(getCommand), ex);
        } catch (InterruptedException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getUserData", "failed: " + object + ", EPC: " + epc + " -> " + join(getCommand), ex);
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
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "begin: " + object + ", EPC: " + epc + " -> " + join(setCommand) + ", data: " + data);
            proc = Runtime.getRuntime().exec(append(setCommand, object.getEOJ().toString(), epc2str(epc), data.toString()));
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed: " + object + ", EPC: " + epc + " -> " + join(setCommand) + ", data: " + data, ex);
            return false;
        }
        
        try {
            int exitValue = proc.waitFor();
            LOGGER.logp(Level.INFO, CLASS_NAME, "setUserData", "end: " + object + ", EPC: " + epc + " -> " + join(setCommand) + ", data: " + data + ", exit: " + exitValue);
            return (exitValue == 0);
        } catch (InterruptedException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setUserData", "failed: " + object + ", EPC: " + epc + " -> " + join(setCommand) + ", data: " + data, ex);
        }
        
        return false;
    }
}

