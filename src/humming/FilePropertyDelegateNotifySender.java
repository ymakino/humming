package humming;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.util.Pair;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class FilePropertyDelegateNotifySender extends Thread {
    private static final Logger LOGGER = Logger.getLogger(FilePropertyDelegateNotifySender.class.getName());
    private static final String CLASS_NAME = FilePropertyDelegateNotifySender.class.getName();
    
    private static final int DEFAULT_INTERVAL = 1000;
    private static final int DEFAULT_DELAY = 1000;
    
    private FilePropertyDelegate delegate;
    private LinkedList<Pair<LocalObject, EPC>> objects;
    private String filename;
    private int interval;
    private int delay;
    private boolean done;
    
    public FilePropertyDelegateNotifySender(FilePropertyDelegate delegate, String filename) {
        objects = new LinkedList<Pair<LocalObject, EPC>>();
        done = false;
        this.delegate = delegate;
        this.filename = filename;
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
    
    private synchronized void notifyData() throws FileNotFoundException, IOException, NumberFormatException {
        
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filename));

        try {
            ObjectData data;
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
            } else {
                data = new ObjectData();
            }
            
            LOGGER.logp(Level.INFO, CLASS_NAME, "notifyData", "file: " + filename + ", data: " + data);
            
            if (!Files.deleteIfExists(new File(filename).toPath())) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "notifyData", "cannot delete: " + filename);
            }
            
            for (Pair<LocalObject, EPC> p: objects) {
                LocalObject object = p.first;
                EPC epc = p.second;
                LOGGER.logp(Level.INFO, CLASS_NAME, "notifyData", "send notify " + object + " " + epc + " " + data);
                object.notifyDataChanged(epc, data, null);
            }
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(FilePropertyDelegate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public boolean addProperty(LocalObject object, EPC epc) {
        return objects.add(new Pair<LocalObject, EPC>(object, epc));
    }
    
    @Override
    public void run() {
        try {
            Thread.sleep(delay);
            
            while (!done) {
                try {
                    delegate.waitUnblocked();
                    notifyData();
                } catch (FileNotFoundException ex) {
                    // do nothing
                } catch (IOException ex) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "run", "file: " + filename, ex);
                } catch (NumberFormatException ex) {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, "run", "file: " + filename, ex);
                }
                
                Thread.sleep(interval);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(FilePropertyDelegateNotifySender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
