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
    private static final Logger logger = Logger.getLogger(CommandPropertyDelegate.class.getName());
    private static final String className = CommandPropertyDelegate.class.getName();
    
    private String getCommand;
    private String setCommand;
    
    public CommandPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled, String getCommand, String setCommand) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        this.getCommand = getCommand;
        this.setCommand = setCommand;
        logger.logp(Level.INFO, className, "CommandPropertyDelegate", "epc: " + epc + " -> get: " + getCommand + ", set: " + setCommand);
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        Process proc;
        InputStreamReader reader;
        
        if (getCommand == null) {
            logger.logp(Level.WARNING, className, "getUserData", "get command is not set");
            return null;
        }
        
        try {
            logger.logp(Level.INFO, className, "getUserData", object + ", EPC: " + epc + " -> " + getCommand);
            proc = Runtime.getRuntime().exec(getCommand);
            reader = new InputStreamReader(proc.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(CommandPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        try {
            char[] chars = new char[256];
            int len = reader.read(chars);
            if (len > 0) {
                String line = String.valueOf(chars).split("[^0-9a-fA-F]")[0];
                len = line.length();
                
                byte[] bytes = new byte[len/2];
                for (int i=0; i<len-1; i+=2) {
                    bytes[i/2] = (byte)Integer.parseInt(line.substring(i, i+2), 16);
                }
                
                return new ObjectData(bytes);
            }
                
            return new ObjectData();
        } catch (IOException ex) {
            Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NumberFormatException ex) {
            Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                int exitValue = proc.waitFor();
                logger.logp(Level.INFO, className, "getUserData", object + ", EPC: " + epc + " -> " + getCommand + ", exit: " + exitValue);
            } catch (InterruptedException ex) {
                Logger.getLogger(CommandPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return null;
    }

    @Override
    public boolean setUserData(LocalObject object, EPC epc, ObjectData data) {
        Process proc;
        
        if (setCommand == null) {
            logger.logp(Level.WARNING, className, "setUserData", "set command is not set");
            return false;
        }
        
        try {
            logger.logp(Level.INFO, className, "setUserData", object + ", EPC: " + epc + " -> " + setCommand + ", data: " + data);
            proc = Runtime.getRuntime().exec(new String[]{setCommand, data.toString()});
        } catch (IOException ex) {
            Logger.getLogger(CommandPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        try {
            int exitValue = proc.waitFor();
            logger.logp(Level.INFO, className, "setUserData", object + ", EPC: " + epc + " -> " + setCommand + ", exit: " + exitValue);
            return (exitValue == 0);
        } catch (InterruptedException ex) {
            Logger.getLogger(CommandPropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
}
