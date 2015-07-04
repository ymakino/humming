package humming.generator;

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
public class EchonetObjectHelper {
    private static final Logger LOGGER = Logger.getLogger(EchonetObjectHelper.class.getName());
    private static final String CLASS_NAME = EchonetObjectHelper.class.getName();
    
    public static final int GET_RETRY_COUNT = 5;
    public static final int OBSERVE_RETRY_COUNT = 5;
    
    public static ObjectData getData(EchonetObject object, EPC epc) throws GeneratorException {
        LOGGER.entering(CLASS_NAME, "getData", new Object[]{object, epc});
        
        for (int i=0; i<GET_RETRY_COUNT; i++) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "getData", "getData(" + epc + ") retry: " + i);
                    
            try {
                ObjectData data = object.getData(epc);
                LOGGER.logp(Level.INFO, CLASS_NAME, "getData", "getData(" + epc + "): " + data);
                LOGGER.exiting(CLASS_NAME, "getData", data);
                return data;
            } catch (EchonetObjectException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "getData", "failed: ", ex);
            }
        }
        
        GeneratorException exception = new GeneratorException("cannot get property map: " + epc);
        LOGGER.throwing(CLASS_NAME, "getData", exception);
        throw exception;
    }
    
    private static class NotificationObserver implements RemoteObjectObserver {
        private ObjectData data = null;
        
        @Override
        public synchronized void notifyData(RemoteObject remoteObject, EPC epc, ObjectData data) {
            this.data = data;
        }
        
        public synchronized ObjectData getData() {
            return data;
        }
    }
    
    public static ObjectData observeData(EchonetObject object, EPC epc) throws GeneratorException {
        LOGGER.entering(CLASS_NAME, "observeData", new Object[]{object, epc});
        
        ObjectData data = null;
        
        if (object instanceof LocalObject) {
            LocalObject localObject = (LocalObject) object;
            data = localObject.forceGetData(epc);
        } else if (object instanceof RemoteObject) {
            RemoteObject remoteObject = (RemoteObject) object;
            NotificationObserver notificationObserver = new NotificationObserver();
            remoteObject.addObserver(notificationObserver);

            try {
                for (int i = 0; i < OBSERVE_RETRY_COUNT; i++) {
                    LOGGER.logp(Level.INFO, CLASS_NAME, "observeData", "observeData(" + epc + ") retry: " + i);
                    
                    remoteObject.observeData(epc);

                    for (int j = 0; j < 50; j++) {
                        Thread.sleep(100);
                        data = notificationObserver.getData();
                        if (data != null) {
                            LOGGER.logp(Level.INFO, CLASS_NAME, "obsereData", "obsereData(" + epc + "): " + data);
                            break;
                        }
                    }
                }
            } catch (EchonetObjectException ex) {
                GeneratorException exception = new GeneratorException("failed", ex);
                LOGGER.throwing(CLASS_NAME, "obsereData", exception);
                throw exception;
            } catch (InterruptedException ex) {
                GeneratorException exception = new GeneratorException("failed", ex);
                LOGGER.throwing(CLASS_NAME, "obsereData", exception);
                throw exception;
            } finally {
                remoteObject.removeObserver(notificationObserver);
            }
        } else {
            GeneratorException exception = new GeneratorException("unsupported object: " + object);
            LOGGER.throwing(CLASS_NAME, "obsereData", exception);
            throw exception;
        }

        LOGGER.exiting(CLASS_NAME, "obsereData", data);
        return data;
    }
}
