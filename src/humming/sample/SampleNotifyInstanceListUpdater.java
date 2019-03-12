package humming.sample;

import echowand.common.EPC;
import echowand.net.SubnetException;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyUpdater;
import echowand.service.Service;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class SampleNotifyInstanceListUpdater extends PropertyUpdater {
    private static final Logger LOGGER = Logger.getLogger(SampleNotifyInstanceListUpdater.class.getName());
    private static final String CLASS_NAME = SampleNotifyInstanceListUpdater.class.getName();
    
    private Service service = null;
    
    private ObjectData getInstanceListData() {
        return getCore().getNodeProfileObject().forceGetData(EPC.xD5);
    }

    @Override
    public void loop(LocalObject localObject) {
        LOGGER.entering(CLASS_NAME, "loop", localObject);
        
        if (service == null) {
            service = new Service(getCore());
        }
        
        try {
            LOGGER.logp(Level.INFO, CLASS_NAME, "loop", "notify instance list: " + getInstanceListData());
            service.doNotifyInstanceList();
        } catch (SubnetException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "loop", "cannot notify instance list", ex);
        }
        
        LOGGER.exiting(CLASS_NAME, "loop");
    }
    
}
