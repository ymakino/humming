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
    
    public static ObjectData DEFAULT_DATA = new ObjectData();
    
    public static final int GET_RETRY_COUNT = 5;
    public static final int OBSERVE_RETRY_COUNT = 5;
    public static final int ACCESS_RULE_RETRY_COUNT = 5;

    static boolean isGettable(EchonetObject object, EPC epc) throws GeneratorException {
        for (int i=0; i<ACCESS_RULE_RETRY_COUNT; i++) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "isGettable", "isGettable(" + object + ", " + epc + ") retry: " + i);
                    
            try {
                boolean result = object.isGettable(epc);
                LOGGER.logp(Level.INFO, CLASS_NAME, "isGettable", "isGettable(" + object + ", " + epc + "): " + result);
                LOGGER.exiting(CLASS_NAME, "isGettable", result);
                return result;
            } catch (EchonetObjectException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "isGettable", "failed: ", ex);
            }
        }
        
        GeneratorException exception = new GeneratorException("cannot get GET access rule: " + epc);
        LOGGER.throwing(CLASS_NAME, "isGettable", exception);
        throw exception;
    }

    static boolean isObservable(EchonetObject object, EPC epc) throws GeneratorException {
        for (int i=0; i<ACCESS_RULE_RETRY_COUNT; i++) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "isObservable", "isObservable(" + object + ", " + epc + ") retry: " + i);
                    
            try {
                boolean result = object.isGettable(epc);
                LOGGER.logp(Level.INFO, CLASS_NAME, "isObservable", "isObservable(" + object + ", " + epc + "): " + result);
                LOGGER.exiting(CLASS_NAME, "isObservable", result);
                return result;
            } catch (EchonetObjectException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "isObservable", "failed: ", ex);
            }
        }
        
        GeneratorException exception = new GeneratorException("cannot get OBSERVE access rule: " + epc);
        LOGGER.throwing(CLASS_NAME, "isObservable", exception);
        throw exception;
    }

    static boolean isSettable(EchonetObject object, EPC epc) throws GeneratorException {
        for (int i=0; i<ACCESS_RULE_RETRY_COUNT; i++) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "isSettable", "isSettable(" + object + ", " + epc + ") retry: " + i);
                    
            try {
                boolean result = object.isSettable(epc);
                LOGGER.logp(Level.INFO, CLASS_NAME, "isSettable", "isSettable(" + object + ", " + epc + "): " + result);
                LOGGER.exiting(CLASS_NAME, "isGettable", result);
                return result;
            } catch (EchonetObjectException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "isSettable", "failed: ", ex);
            }
        }
        
        GeneratorException exception = new GeneratorException("cannot get SET access rule: " + epc);
        LOGGER.throwing(CLASS_NAME, "isSettable", exception);
        throw exception;
    }
    
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
        
        GeneratorException exception = new GeneratorException("cannot get data: " + epc);
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
    
    public static ObjectData forceGetData(EchonetObject object, EPC epc) throws GeneratorException {
        LOGGER.entering(CLASS_NAME, "forceGetData", new Object[]{object, epc});
        
        ObjectData data;
        
        if (object instanceof LocalObject) {
            data = ((LocalObject)object).forceGetData(epc);
            LOGGER.logp(Level.INFO, CLASS_NAME, "forceGetData", "forceGetData(" + epc + "): " + data);
        } else {
            data = DEFAULT_DATA;
            LOGGER.logp(Level.WARNING, CLASS_NAME, "forceGetData", "unsupported object: " + object);
        }
        
        LOGGER.exiting(CLASS_NAME, "forceGetData", data);
        return data;
    }
}
