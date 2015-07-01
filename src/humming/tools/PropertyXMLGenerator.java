package humming.tools;

import echowand.common.EPC;
import echowand.object.EchonetObject;
import echowand.object.EchonetObjectException;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.object.RemoteObject;
import echowand.object.RemoteObjectObserver;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class PropertyXMLGenerator {
    private static final Logger LOGGER = Logger.getLogger(PropertyXMLGenerator.class.getName());
    private static final String CLASS_NAME = PropertyXMLGenerator.class.getName();
    
    private EchonetObject object;
    private EPC epc;
    private boolean getEnabled;
    private boolean setEnabled;
    private boolean notifyEnabled;
    
    private String indent1 = "    ";
    private String indent2 = "      ";
    
    private static String b2s(boolean b) {
        if (b) {
            return "enabled";
        } else {
            return "disabled";
        }
    }
    

    public PropertyXMLGenerator(EchonetObject object, EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled) {
        this.object = object;
        this.epc = epc;
        this.getEnabled = getEnabled;
        this.setEnabled = setEnabled;
        this.notifyEnabled = notifyEnabled;
    }
    
    private class NotificationObserver implements RemoteObjectObserver {
        private ObjectData data = null;
        
        @Override
        public void notifyData(RemoteObject remoteObject, EPC epc, ObjectData data) {
            this.data = data;
        }
        
        public ObjectData getData() {
            return data;
        }
    }
    
    public ObjectData getObservableData(RemoteObject remoteObject) throws GeneratorException {
        NotificationObserver notificationObserver = new NotificationObserver();
        
        try {
            remoteObject.addObserver(notificationObserver);
            remoteObject.observeData(epc);
            
            Thread.sleep(5000);
            
            return notificationObserver.getData();
        } catch (InterruptedException ex) {
            GeneratorException exception = new GeneratorException("getObservableData failed", ex);
            throw exception;
        } catch (EchonetObjectException ex) {
            GeneratorException exception = new GeneratorException("getObservableData failed", ex);
            throw exception;
        } finally {
            remoteObject.removeObserver(notificationObserver);
        }
    }

    public String generate() throws GeneratorException  {
        StringBuilder builder = new StringBuilder();
        String beginElement = String.format(indent1 + "<property epc=\"%s\" get=\"%s\" set=\"%s\" notify=\"%s\">\n", epc.toString().substring(1), b2s(getEnabled), b2s(setEnabled), b2s(notifyEnabled));
        builder.append(beginElement);
        
        ObjectData data = new ObjectData((byte)0x00);
        try {
            if (object.isGettable(epc)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "isGettable: " + epc);
                data = object.getData(epc);
            } else if (object.isObservable(epc)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "isObservable: " + epc);
                if (object instanceof LocalObject) {
                    LocalObject localObject = (LocalObject)object;
                    data = localObject.forceGetData(epc);
                } else if (object instanceof RemoteObject) {
                    RemoteObject remoteObject = (RemoteObject)object;
                    data = getObservableData(remoteObject);
                }
            }
            LOGGER.logp(Level.INFO, CLASS_NAME, "generate", "generate: " + epc + " " + data);
        } catch (EchonetObjectException ex) {
            Logger.getLogger(PropertyXMLGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (data == null) {
            GeneratorException exception = new GeneratorException("invalid data: " + data);
            throw exception;
        }
        
        builder.append(String.format(indent2 + "<data type=\"variable\">%s</data>\n", data.toString()));
        
        builder.append(indent1 + "</property>\n");
        
        return builder.toString();
    }
    
}
