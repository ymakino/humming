package humming;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.util.Pair;
import static humming.CommandPropertyDelegateUtil.concatString;
import static humming.CommandPropertyDelegateUtil.epc2str;
import static humming.CommandPropertyDelegateUtil.joinString;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class CommandPropertyDelegateNotifySender extends Thread {
    private static final Logger LOGGER = Logger.getLogger(CommandPropertyDelegateNotifySender.class.getName());
    private static final String CLASS_NAME = CommandPropertyDelegateNotifySender.class.getName();
    
    private static int DEFAULT_INTERVAL = 0;
    private static int DEFAULT_DELAY = 0;
    
    private String[] notifyCommand;
    LocalObject object = null;
    EPC epc = null;
    private LinkedList<Pair<LocalObject, EPC>> objects;
    private int interval;
    private int delay;
    private boolean done;
    
    public CommandPropertyDelegateNotifySender(String[] notifyCommand) {
        objects = new LinkedList<Pair<LocalObject, EPC>>();
        this.notifyCommand = Arrays.copyOf(notifyCommand, notifyCommand.length);
        this.done = false;
        this.interval = DEFAULT_INTERVAL;
        this.delay = DEFAULT_DELAY;
    }
    
    public int getInterval() {
        return interval;
    }
    
    public void setInterval(int interval) {
        this.interval = interval;
    }
    
    public int getDelay() {
        return interval;
    }
    
    public void setDelay(int delay) {
        this.delay = delay;
    }
    
    public synchronized void finish() {
        done = true;
    }
    
    public synchronized boolean isDone() {
        return done;
    }
    
    public void setCommandProperty(LocalObject object, EPC epc) {
        this.object = object;
        this.epc = epc;
    }
    
    public boolean addProperty(LocalObject object, EPC epc) {
        if (this.object == null) {
            this.object = object;
        }
        
        if (this.epc == null) {
            this.epc = epc;
        }
        
        return objects.add(new Pair<LocalObject, EPC>(object, epc));
    }
    
    public String[] getNotifyCommand() {
        return Arrays.copyOf(notifyCommand, notifyCommand.length);
    }
    
    public ObjectData getNotifyData() {
        Process proc;
        InputStreamReader reader;
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getNotifyData", "begin: " + object + ", EPC: " + epc + " -> " + joinString(notifyCommand));
            proc = Runtime.getRuntime().exec(concatString(notifyCommand, object.getEOJ().toString(), epc2str(epc)));
            reader = new InputStreamReader(proc.getInputStream());
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getNotifyData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(notifyCommand), ex);
            return null;
        }
        
        try {
            ObjectData data = new ObjectData();
            char[] chars = new char[512];
            
            int len = reader.read(chars);
            String str = String.valueOf(chars).toUpperCase().trim();
            LOGGER.logp(Level.INFO, CLASS_NAME, "getNotifyData", "read: " + object + ", EPC: " + epc + " -> " + joinString(notifyCommand) + ", str: " + str);
            String[] lines = str.split("[^0-9a-fA-F]");
            
            if (len > 0) {
                    
                if (str.equals("UPDATE")) {
                    data = object.getData(epc);
                } else if (lines.length > 0) {
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
            LOGGER.logp(Level.INFO, CLASS_NAME, "getNotifyData", "end: " + object + ", EPC: " + epc + " -> " + joinString(notifyCommand) + ", str: " + str + ", data: " + data + ", exit: " + exitValue);
            return data;
        } catch (IOException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getNotifyData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(notifyCommand), ex);
        } catch (NumberFormatException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getNotifyData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(notifyCommand), ex);
        } catch (InterruptedException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getNotifyData", "failed: " + object + ", EPC: " + epc + " -> " + joinString(notifyCommand), ex);
        }
        
        return null;
    }
    
    private synchronized void notifyData() {
        ObjectData data = getNotifyData();

        if (data == null) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "notifyData", "invalid: " + object + ", EPC: " + epc + " -> " + joinString(notifyCommand));
            return;
        }
        
        if (data.isEmpty()) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "notifyData", "skip: " + object + ", EPC: " + epc + " -> " + joinString(notifyCommand));
            return;
        }

        for (Pair<LocalObject, EPC> p : objects) {
            LocalObject object = p.first;
            EPC epc = p.second;
            LOGGER.logp(Level.INFO, CLASS_NAME, "notifyData", "send notify " + object + " " + epc + " " + data);
            object.notifyDataChanged(epc, data, null);
        }
    }
    
    @Override
    public void run() {
        try {
            Thread.sleep(delay);
            
            while (!done) {
                notifyData();
                Thread.sleep(interval);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(CommandPropertyDelegateNotifySender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}