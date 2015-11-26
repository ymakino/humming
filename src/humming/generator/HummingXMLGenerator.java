package humming.generator;

import echowand.logic.TooManyObjectsException;
import echowand.net.Inet4Subnet;
import echowand.net.SubnetException;
import echowand.object.EchonetObjectException;
import echowand.service.Core;
import echowand.service.Service;
import humming.NetworkInterfaceSelector;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class HummingXMLGenerator {
    private static final Logger LOGGER = Logger.getLogger(HummingXMLGenerator.class.getName());
    private static final String CLASS_NAME = HummingXMLGenerator.class.getName();
    
    public static void showUsage(String name) {
        System.out.println("Usage: " + name + " [ -i interface ] address...");
    }

    public static void main(String[] args) throws SubnetException, TooManyObjectsException, GeneratorException, EchonetObjectException, UnknownHostException, SocketException {
        try {
            int startIndex = 0;
            Core core;
            
            if (args[0].equals("-h")) {
                showUsage("HummingXMLGenerator");
                System.exit(0);
            }

            if (args[0].equals("-i")) {
                NetworkInterface nif;
                
                if (args[1].equals("-")) {
                    nif = NetworkInterfaceSelector.select();
                } else {
                    nif = NetworkInterface.getByName(args[1]);
                }
                
                Inet4Subnet subnet = Inet4Subnet.startSubnet(nif);
                core = new Core(subnet);
                startIndex = 2;
            } else {
                core = new Core();
            }

            core.startService();
            Service service = new Service(core);

            for (int i = startIndex; i < args.length; i++) {
                DeviceElementGenerator generator = new DeviceElementGenerator(service, service.getRemoteNode(args[i]));
                System.out.print(generator.generate());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        } finally {
            System.exit(0);
        }
    }
}
