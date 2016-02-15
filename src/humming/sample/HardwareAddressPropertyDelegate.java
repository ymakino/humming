package humming.sample;

import echowand.common.EPC;
import echowand.object.LocalObject;
import echowand.object.ObjectData;
import echowand.service.PropertyDelegate;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class HardwareAddressPropertyDelegate extends PropertyDelegate {
    private static final Logger LOGGER = Logger.getLogger(HardwareAddressPropertyDelegate.class.getName());
    private static final String CLASS_NAME = HardwareAddressPropertyDelegate.class.getName();
    
    private NetworkInterface networkInterface;
    private boolean isStringMode;
    private boolean isIdentificationMode;
    
    private byte[] dataTemplate;
    private int dataOffset;

    public HardwareAddressPropertyDelegate(EPC epc, boolean getEnabled, boolean setEnabled, boolean notifyEnabled) {
        super(epc, getEnabled, setEnabled, notifyEnabled);
        LOGGER.entering(CLASS_NAME, "HardwareAddressPropertyDelegate", new Object[]{epc, getEnabled, setEnabled, notifyEnabled});
        
        networkInterface = null;
        isStringMode = false;
        dataTemplate = new byte[]{(byte)0xFE,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
        dataOffset = 1;
        
        LOGGER.exiting(CLASS_NAME, "HardwareAddressPropertyDelegate");
    }
    
    public boolean setNetworkInterface(NetworkInterface networkInterface) {
        LOGGER.entering(CLASS_NAME, "setNetworkInterface", networkInterface);
        
        this.networkInterface = networkInterface;
        
        LOGGER.exiting(CLASS_NAME, "setNetworkInterface", false);
        return true;
    }
    
    public boolean setNetworkInterface(InetAddress address) {
        LOGGER.entering(CLASS_NAME, "setNetworkInterface", address);
        
        try {
            networkInterface = NetworkInterface.getByInetAddress(address);
            LOGGER.exiting(CLASS_NAME, "setNetworkInterface", true);
            return true;
        } catch (SocketException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setNetworkInterface", "cannot find interface: " + address, ex);
            LOGGER.exiting(CLASS_NAME, "setNetworkInterface", false);
            return false;
        }
    }
    
    public boolean setNetworkInterface(String name) {
        LOGGER.entering(CLASS_NAME, "setNetworkInterface", name);
        
        try {
            NetworkInterface nif = NetworkInterface.getByName(name);
            
            if (nif != null) {
                networkInterface = nif;
                LOGGER.logp(Level.INFO, CLASS_NAME, "setNetworkInterface", "set interface: " + nif);
                LOGGER.exiting(CLASS_NAME, "setNetworkInterface", true);
                return true;
            }
            
            try {
                InetAddress inetAddress = InetAddress.getByName(name);
                nif = NetworkInterface.getByInetAddress(inetAddress);
            } catch (UnknownHostException ex) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "setNetworkInterface", "cannot find interface: " + name, ex);
            }
            
            if (nif != null) {
                networkInterface = nif;
                LOGGER.logp(Level.INFO, CLASS_NAME, "setNetworkInterface", "set interface: " + nif);
                LOGGER.exiting(CLASS_NAME, "setNetworkInterface", true);
                return true;
            }
        } catch (SocketException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "setNetworkInterface", "cannot find interface: " + name, ex);
            LOGGER.exiting(CLASS_NAME, "setNetworkInterface", false);
            return false;
        }
        
        LOGGER.logp(Level.WARNING, CLASS_NAME, "setNetworkInterface", "cannot find interface: " + name);
        LOGGER.exiting(CLASS_NAME, "setNetworkInterface", false);
        return false;
    }
    
    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }
    
    public void setStringMode(boolean isStringMode) {
        this.isStringMode = isStringMode;
    }
    
    public boolean isStringMode() {
        return isStringMode;
    }
    
    public void setIdentificationMode(boolean isIdentificationMode) {
        this.isIdentificationMode = isIdentificationMode;
    }
    
    public boolean isIdentificationMode() {
        return isIdentificationMode;
    }
    
    private byte[] getHardwareAddress() {
        LOGGER.entering(CLASS_NAME, "getHardwareAddress");
        
        byte[] haddr = null;
        
        try {
            haddr = networkInterface.getHardwareAddress();
            if (haddr == null) {
                LOGGER.logp(Level.WARNING, CLASS_NAME, "getHardwareAddress", "cannot get hardware address: " + networkInterface);
            }
        } catch (SocketException ex) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getHardwareAddress", "cannot get hardware address: ", ex);
        }
        
        LOGGER.exiting(CLASS_NAME, "getHardwareAddress", haddr);
        return haddr;
    }
    
    private String bytesToString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<bytes.length; i++) {
            builder.append(String.format("%02x", 0x00ff & bytes[i]));
        }
        
        return builder.toString();
    }
    
    private byte[] makeStringBytes(byte[] haddr) {
        return bytesToString(haddr).getBytes();
    }
    
    private byte[] makeIdentificationBytes(byte[] haddr) {
        byte[] bytes = Arrays.copyOf(dataTemplate, dataTemplate.length);
        
        int offset = Math.max(dataOffset, bytes.length - haddr.length);
        int length = Math.min(bytes.length - dataOffset, haddr.length);
        System.arraycopy(haddr, 0, bytes, offset, length);
        
        return bytes;
    }
    
    public ObjectData getHardwareAddressData() {
        LOGGER.entering(CLASS_NAME, "getHardwareAddressData");
        
        byte[] bytes = getHardwareAddress();
        
        if (bytes == null) {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "getHardwareAddressData", "cannot get hardware address: " + networkInterface);
            LOGGER.exiting(CLASS_NAME, "getHardwareAddressData", null);
            return null;
        }
        
        if (isIdentificationMode) {
            bytes = makeIdentificationBytes(bytes);
        }
        
        if (isStringMode) {
            bytes = makeStringBytes(bytes);
        }
        
        ObjectData objectData = new ObjectData(bytes);
        LOGGER.exiting(CLASS_NAME, "getHardwareAddressData", objectData);
        return objectData;
    }
    
    @Override
    public ObjectData getUserData(LocalObject object, EPC epc) {
        LOGGER.entering(CLASS_NAME, "getUserData", new Object[]{object, epc});
        
        ObjectData objectData = getHardwareAddressData();
        
        System.out.println("HardwareAddressPropertyDelegate.getUserData("+object + ", " + epc + "): " + objectData);
        
        LOGGER.exiting(CLASS_NAME, "getUserData", objectData);
        return objectData;
    }
}
