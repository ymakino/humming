package humming;

import echowand.common.EOJ;
import echowand.common.ESV;
import echowand.logic.DefaultRequestProcessor;
import echowand.net.CommonFrame;
import echowand.net.Frame;
import echowand.net.Node;
import echowand.net.Property;
import echowand.net.StandardPayload;
import echowand.net.Subnet;
import echowand.net.SubnetException;
import echowand.object.LocalObject;
import echowand.object.LocalObjectManager;
import echowand.object.LocalSetGetAtomic;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class ConcurrentSetGetRequestProcessor extends DefaultRequestProcessor {
    private static final Logger LOGGER = Logger.getLogger(ConcurrentSetGetRequestProcessor.class.getName());
    private static final String CLASS_NAME = ConcurrentSetGetRequestProcessor.class.getName();
    
    private LocalObjectManager manager;
    
    /**
     * ローカルオブジェクトのSetやGetを行うためにLocalObjectManagerを持っている必要がある。
     * @param manager Set、Getの対象となるローカルオブジェクト群
     */
    public ConcurrentSetGetRequestProcessor(LocalObjectManager manager) {
        LOGGER.entering(CLASS_NAME, "ConcurrentSetGetRequestProcessor", manager);
        
        this.manager = manager;
        
        LOGGER.exiting(CLASS_NAME, "ConcurrentSetGetRequestProcessor");
    }
    
    private void addAllSetFromFirst(LocalSetGetAtomic localSetGetAtomic, StandardPayload payload) {
        LOGGER.entering(CLASS_NAME, "addAllSetFromFirst", new Object[]{localSetGetAtomic, payload});
        
        int len = payload.getFirstOPC();
        for (int i=0; i<len; i++) {
            localSetGetAtomic.addSet(payload.getFirstPropertyAt(i));
        }
        
        LOGGER.exiting(CLASS_NAME, "addAllSetFromFirst");
    }
    
    private void addAllGetFromFirst(LocalSetGetAtomic localSetGetAtomic, StandardPayload payload) {
        LOGGER.entering(CLASS_NAME, "addAllGetFromFirst", new Object[]{localSetGetAtomic, payload});
        
        int len = payload.getFirstOPC();
        for (int i=0; i<len; i++) {
            localSetGetAtomic.addGet(payload.getFirstPropertyAt(i));
        }
        
        LOGGER.exiting(CLASS_NAME, "addAllGetFromFirst");
    }
    
    private void addAllGetFromSecond(LocalSetGetAtomic localSetGetAtomic, StandardPayload payload) {
        LOGGER.entering(CLASS_NAME, "addAllGetFromSecond", new Object[]{localSetGetAtomic, payload});
        
        int len = payload.getSecondOPC();
        for (int i=0; i<len; i++) {
            localSetGetAtomic.addGet(payload.getSecondPropertyAt(i));
        }
        
        LOGGER.exiting(CLASS_NAME, "addAllGetFromSecond");
    }
    
    private void addAllSetToFirst(LocalSetGetAtomic localSetGetAtomic, StandardPayload payload) {
        LOGGER.entering(CLASS_NAME, "addAllSetToFirst", new Object[]{localSetGetAtomic, payload});
        
        for (Property property : localSetGetAtomic.getSetResult()) {
            payload.addFirstProperty(property);
        }
        
        LOGGER.exiting(CLASS_NAME, "addAllSetToFirst");
    }
    
    private void addAllGetToFirst(LocalSetGetAtomic localSetGetAtomic, StandardPayload payload) {
        LOGGER.entering(CLASS_NAME, "addAllGetToFirst", new Object[]{localSetGetAtomic, payload});
        
        for (Property property : localSetGetAtomic.getGetResult()) {
            payload.addFirstProperty(property);
        }
        
        LOGGER.exiting(CLASS_NAME, "addAllGetToFirst");
    }
    
    private void addAllGetToSecond(LocalSetGetAtomic localSetGetAtomic, StandardPayload payload) {
        LOGGER.entering(CLASS_NAME, "addAllGetToSecond", new Object[]{localSetGetAtomic, payload});
        
        for (Property property : localSetGetAtomic.getGetResult()) {
            payload.addSecondProperty(property);
        }
        
        LOGGER.exiting(CLASS_NAME, "addAllGetToSecond");
    }
    
    private boolean doSetAllData(Frame frame, LocalObject object, StandardPayload res) {
        LOGGER.entering(CLASS_NAME, "doSetAllData", new Object[]{frame, object, res});
        
        StandardPayload req = frame.getCommonFrame().getEDATA(StandardPayload.class);
        
        LocalSetGetAtomic localSetGetAtomic = new LocalSetGetAtomic(object);
        
        addAllSetFromFirst(localSetGetAtomic, req);
        
        localSetGetAtomic.run();
        
        addAllSetToFirst(localSetGetAtomic, res);
        
        boolean success = localSetGetAtomic.isSuccess();
        
        LOGGER.exiting(CLASS_NAME, "doSetAllData", success);
        return success;
    }
    
    private boolean doGetAllData(Frame frame, LocalObject object, StandardPayload res, boolean announce) {
        LOGGER.entering(CLASS_NAME, "doGetAllData", new Object[]{frame, object, res, announce});
        
        StandardPayload req = frame.getCommonFrame().getEDATA(StandardPayload.class);
        
        LocalSetGetAtomic localSetGetAtomic = new LocalSetGetAtomic(object);
        localSetGetAtomic.setAnnounce(announce);
        
        addAllGetFromFirst(localSetGetAtomic, req);
        
        localSetGetAtomic.run();
        
        addAllGetToFirst(localSetGetAtomic, res);
        
        boolean success = localSetGetAtomic.isSuccess();
        
        LOGGER.exiting(CLASS_NAME, "doGetAllData", success);
        return success;
    }
    
    private boolean doSetGetAllData(Frame frame, LocalObject object, StandardPayload res) {
        LOGGER.entering(CLASS_NAME, "doSetGetAllData", new Object[]{frame, object, res});
        
        StandardPayload req = frame.getCommonFrame().getEDATA(StandardPayload.class);
        
        LocalSetGetAtomic localSetGetAtomic = new LocalSetGetAtomic(object);
        
        addAllSetFromFirst(localSetGetAtomic, req);
        addAllGetFromSecond(localSetGetAtomic, req);
        
        localSetGetAtomic.run();
        
        addAllSetToFirst(localSetGetAtomic, res);
        addAllGetToSecond(localSetGetAtomic, res);
        
        boolean success = localSetGetAtomic.isSuccess();
        
        LOGGER.exiting(CLASS_NAME, "doSetGetAllData", success);
        return success;
    }
    
    private Frame createResponse(Node sender, Frame frame, LocalObject object, StandardPayload res) {
        LOGGER.entering(CLASS_NAME, "createResponse", new Object[]{sender, frame, object, res});
        
        Frame resFrame = createResponse(sender, frame, object, res, false, null);
        
        LOGGER.exiting(CLASS_NAME, "createResponse", resFrame);
        return resFrame;
    }
    
    private Frame createResponse(Node sender, Frame frame, LocalObject object, StandardPayload res, boolean useGroup, Subnet subnet) {
        LOGGER.entering(CLASS_NAME, "createResponse", new Object[]{sender, frame, object, res, useGroup, subnet});
        
        short tid = frame.getCommonFrame().getTID();
        CommonFrame cf = new CommonFrame();
        cf.setTID(tid);
        cf.setEDATA(res);
        
        StandardPayload req = frame.getCommonFrame().getEDATA(StandardPayload.class);
        res.setDEOJ(req.getSEOJ());
        res.setSEOJ(object.getEOJ());
        
        Node peer = frame.getSender();
        if (useGroup) {
            peer = subnet.getGroupNode();
        }
        
        Frame resFrame =  new Frame(sender, peer, cf, frame.getConnection());
        
        LOGGER.exiting(CLASS_NAME, "createResponse", resFrame);
        return resFrame;
    }
    
    private List<LocalObject> getDestinationObject(Frame frame) {
        CommonFrame cf = frame.getCommonFrame();
        StandardPayload payload = cf.getEDATA(StandardPayload.class);
        EOJ eoj = payload.getDEOJ();
        if (eoj.isAllInstance()) {
            return manager.getWithClassEOJ(eoj.getClassEOJ());
        } else {
            LinkedList<LocalObject> list = new LinkedList<LocalObject>();
            LocalObject object = manager.get(eoj);
            if (object != null) {
                list.add(object);
            }
            return list;
        }
    }
    
    private void processObjectSetI(final Subnet subnet, final Frame frame, final LocalObject object, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processObjectSetI", new Object[]{subnet, frame, object, processed});
        
        new Thread() {
            @Override
            public void run() {
                StandardPayload res = new StandardPayload();
                if (!doSetAllData(frame, object, res)) {
                    res.setESV(ESV.SetI_SNA);
                    try {
                        subnet.send(createResponse(subnet.getLocalNode(), frame, object, res));
                    } catch (SubnetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        
        LOGGER.entering(CLASS_NAME, "processObjectSetI");
    }
    
    private void processObjectSetC(final Subnet subnet, final Frame frame, final LocalObject object, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processObjectSetC", new Object[]{subnet, frame, object, processed});
        
        new Thread() {
            @Override
            public void run() {
                StandardPayload res = new StandardPayload();
                if (doSetAllData(frame, object, res)) {
                    res.setESV(ESV.Set_Res);
                } else {
                    res.setESV(ESV.SetC_SNA);
                }
                try {
                    subnet.send(createResponse(subnet.getLocalNode(), frame, object, res));
                } catch (SubnetException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        
        LOGGER.exiting(CLASS_NAME, "processObjectSetC");
    }
    
    private void processObjectGet(final Subnet subnet, final Frame frame, final LocalObject object, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processObjectGet", new Object[]{subnet, frame, object, processed});

        new Thread() {
            @Override
            public void run() {
                StandardPayload res = new StandardPayload();
                if (doGetAllData(frame, object, res, false)) {
                    res.setESV(ESV.Get_Res);
                } else {
                    res.setESV(ESV.Get_SNA);
                }
                try {
                    subnet.send(createResponse(subnet.getLocalNode(), frame, object, res));
                } catch (SubnetException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        
        LOGGER.exiting(CLASS_NAME, "processObjectGet");
    }

    private void processObjectSetGet(final Subnet subnet, final Frame frame, final LocalObject object, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processObjectSetGet", new Object[]{subnet, frame, object, processed});
        
        new Thread() {
            @Override
            public void run() {
                StandardPayload res = new StandardPayload();
                if (doSetGetAllData(frame, object, res)) {
                    res.setESV(ESV.SetGet_Res);
                } else {
                    res.setESV(ESV.SetGet_SNA);
                }
                try {
                    subnet.send(createResponse(subnet.getLocalNode(), frame, object, res));
                } catch (SubnetException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        
        LOGGER.exiting(CLASS_NAME, "processObjectSetGet");
    }
    private void processObjectINF_REQ(final Subnet subnet, final Frame frame, final LocalObject object, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processObjectINF_REQ", new Object[]{subnet, frame, object, processed});
        
        new Thread() {
            @Override
            public void run() {
                boolean useGroup;

                StandardPayload res = new StandardPayload();
                if (doGetAllData(frame, object, res, true)) {
                    res.setESV(ESV.INF);
                    useGroup = true;
                } else {
                    res.setESV(ESV.INF_SNA);
                    useGroup = false;
                }

                try {
                    subnet.send(createResponse(subnet.getLocalNode(), frame, object, res, useGroup, subnet));
                } catch (SubnetException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        
        LOGGER.exiting(CLASS_NAME, "processObjectINF_REQ");
    }
    
    private boolean processRequest(Subnet subnet, Frame frame, ESV esv, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processRequest", new Object[]{subnet, frame, esv, processed});
        
        if (processed) {
            LOGGER.exiting(CLASS_NAME, "processRequest", false);
            return false;
        }
        
        List<LocalObject> objects = getDestinationObject(frame);
        if (objects.isEmpty()) {
            LOGGER.exiting(CLASS_NAME, "processRequest", false);
            return false;
        }
        
        for (LocalObject object : new ArrayList<LocalObject>(objects)) {
            switch (esv) {
                case SetI:
                    processObjectSetI(subnet, frame, object, processed);
                    break;
                case SetC:
                    processObjectSetC(subnet, frame, object, processed);
                    break;
                case Get:
                    processObjectGet(subnet, frame, object, processed);
                    break;
                case SetGet:
                    processObjectSetGet(subnet, frame, object, processed);
                    break;
                case INF_REQ:
                    processObjectINF_REQ(subnet, frame, object, processed);
                    break;
                default:
                    LOGGER.exiting(CLASS_NAME, "processRequest", false);
                    return false;
            }
        }

        LOGGER.exiting(CLASS_NAME, "processRequest", true);
        return true;
    }
    
    /**
     * ESVがSetIであるフレームの処理を行う。
     * @param subnet 受信したフレームの送受信が行なわれたサブネット
     * @param frame 受信したフレーム
     * @param processed 指定されたフレームがすでに処理済みである場合にはtrue、そうでなければfalse
     * @return 処理に成功した場合にはtrue、そうでなければfalse
     */
    @Override
    public boolean processSetI(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processRequest", new Object[]{subnet, frame, processed});
        
        boolean ret = processRequest(subnet, frame, ESV.SetI, processed);
        
        LOGGER.entering(CLASS_NAME, "processRequest", ret);
        return ret;
    }
    
    /**
     * ESVがSetCであるフレームの処理を行う。
     * @param subnet 受信したフレームの送受信が行なわれたサブネット
     * @param frame 受信したフレーム
     * @param processed 指定されたフレームがすでに処理済みである場合にはtrue、そうでなければfalse
     * @return 処理に成功した場合にはtrue、そうでなければfalse
     */
    @Override
    public boolean processSetC(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processSetC", new Object[]{subnet, frame, processed});
        
        boolean ret = processRequest(subnet, frame, ESV.SetC, processed);
        
        LOGGER.entering(CLASS_NAME, "processSetC", ret);
        return ret;
    }
    
    /**
     * ESVがGetであるフレームの処理を行う。
     * @param subnet 受信したフレームの送受信が行なわれたサブネット
     * @param frame 受信したフレーム
     * @param processed 指定されたフレームがすでに処理済みである場合にはtrue、そうでなければfalse
     * @return 処理に成功した場合にはtrue、そうでなければfalse
     */
    @Override
    public boolean processGet(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processGet", new Object[]{subnet, frame, processed});
        
        boolean ret = processRequest(subnet, frame, ESV.Get, processed);
        
        LOGGER.exiting(CLASS_NAME, "processGet", ret);
        return ret;
    }

    /**
     * ESVがSetGetであるフレームの処理を行う。
     * @param subnet 受信したフレームの送受信が行なわれたサブネット
     * @param frame 受信したフレーム
     * @param processed 指定されたフレームがすでに処理済みである場合にはtrue、そうでなければfalse
     * @return 処理に成功した場合にはtrue、そうでなければfalse
     */
    @Override
    public boolean processSetGet(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processSetGet", new Object[]{subnet, frame, processed});
        
        boolean ret = processRequest(subnet, frame, ESV.SetGet, processed);
        
        LOGGER.exiting(CLASS_NAME, "processSetGet", ret);
        return ret;
    }
    
    /**
     * ESVがINF_REQであるフレームの処理を行う。
     * @param subnet 受信したフレームの送受信が行なわれたサブネット
     * @param frame 受信したフレーム
     * @param processed 指定されたフレームがすでに処理済みである場合にはtrue、そうでなければfalse
     * @return 処理に成功した場合にはtrue、そうでなければfalse
     */
    @Override
    public boolean processINF_REQ(Subnet subnet, Frame frame, boolean processed) {
        LOGGER.entering(CLASS_NAME, "processSetGet", new Object[]{subnet, frame, processed});
        
        boolean ret = processRequest(subnet, frame, ESV.INF_REQ, processed);
        
        LOGGER.exiting(CLASS_NAME, "processSetGet", ret);
        return ret;
    }
}
