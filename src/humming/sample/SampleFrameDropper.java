package humming.sample;

import echowand.logic.TooManyObjectsException;
import echowand.net.Inet4Subnet;
import echowand.net.Subnet;
import echowand.net.SubnetException;
import echowand.service.Core;
import echowand.service.LocalObjectConfig;
import humming.Humming;
import humming.HummingException;
import humming.NetworkInterfaceSelector;
import humming.dropper.FrameDropDelegate;
import humming.dropper.FrameDropSubnet;
import humming.dropper.RandomFrameDropper;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author ymakino
 */
public class SampleFrameDropper {
    
    public static void showUsage(String name) {
        System.out.println("Usage: " + name + " [ -i interface ] sendDrop receiveDrop [ xmlfile... ]");
        System.out.println("Example: " + "java -cp humming.jar humming.sample.SampleFrameDropper -i en1 0 0 ../src/humming/sample/sample_frame_drop_delegate.xml ");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws HummingException, SocketException, SubnetException, ParserConfigurationException, SAXException, TooManyObjectsException, IOException {
        
        Core core;
        int fileIndex;
        Subnet subnet;
        
        if (args.length > 0 && args[0].equals("-h")) {
            showUsage("SampleDropper");
            return;
        }
        
        if (args.length > 0 && args[0].equals("-i")) {
            NetworkInterface nif;
            NetworkInterface[] rnifs = null;
            
            if (args[1].equals("-")) {
                nif = NetworkInterfaceSelector.select();
            } else {
                String[] names = args[1].split(",");
                nif = NetworkInterface.getByName(names[0]);
                
                if (names.length > 1) {
                    rnifs = new NetworkInterface[names.length - 1];
                    for (int i=1; i<names.length; i++) {
                        rnifs[i-1] = NetworkInterface.getByName(names[i]);
                    }
                }
            }
            
            if (rnifs == null) {
                subnet = new FrameDropSubnet(Inet4Subnet.startSubnet(nif));
            } else {
                subnet = new FrameDropSubnet(Inet4Subnet.startSubnet(nif, rnifs));
            }
            
            fileIndex = 2;
        } else {
            subnet = new FrameDropSubnet(Inet4Subnet.startSubnet());
            fileIndex = 0;
        }
        
        double sendDrop = Double.parseDouble(args[fileIndex]);
        double receiveDrop = Double.parseDouble(args[fileIndex+1]);
        fileIndex += 2;
        
        if (sendDrop < 0 || 1 < sendDrop) {
            System.out.println("sendDrop must be in range between 0.00 to 1.00");
            showUsage("SampleDropper");
            System.exit(1);
        }
        
        if (receiveDrop < 0 || 1 < receiveDrop) {
            System.out.println("receiveDrop must be between 0.00 to 1.00");
            showUsage("SampleDropper");
            System.exit(1);
        }
        
        core = new Core(subnet);
        core.initialize();
        
        Humming humming = new Humming(core);
        
        for (int i=fileIndex; i<args.length; i++) {
            humming.loadXMLFile(args[i]);
        }
        
        FrameDropSubnet frameDropSubnet = humming.getSubnet(FrameDropSubnet.class);
        if (frameDropSubnet != null) {
            frameDropSubnet.setDefaultDropper(new RandomFrameDropper(sendDrop, receiveDrop));
        
            /*
            for (int i=0; i<humming.countConfigs(); i++) {
                LocalObjectConfig config = humming.getConfig(i);
                FrameDropDelegate delegate = new FrameDropDelegate((FrameDropSubnet)core.getSubnet());
                delegate.setSendDropper(new RandomFrameDropper(0.0));
                delegate.setReceiveDropper(new RandomFrameDropper(0.0));
                config.addDelegate(delegate);
            }
            */
        }
        
        core.startService();
    }
}
