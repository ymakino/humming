package humming.generator;

import echowand.logic.TooManyObjectsException;
import echowand.net.Inet4Subnet;
import echowand.net.SubnetException;
import echowand.object.EchonetObjectException;
import echowand.service.Core;
import echowand.service.Service;
import humming.NetworkInterfaceSelector;
import humming.generator.PropertyElementGenerator.GenerationType;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author ymakino
 */
public class HummingXMLGenerator {
    private static final Logger LOGGER = Logger.getLogger(HummingXMLGenerator.class.getName());
    private static final String CLASS_NAME = HummingXMLGenerator.class.getName();
    
    public static void showUsage(String name) {
        System.out.println("Usage: " + name + " [ -i interface ] [ -t generationType ] [ -p pathPrefix ] address...");
    }

    public static void main(String[] args) throws SubnetException, TooManyObjectsException, GeneratorException, EchonetObjectException, UnknownHostException, SocketException {
        try {
            int startIndex = 0;
            HashMap<String,String> config = new HashMap<String,String>();
            NetworkInterface nif = null;
            
            for (int i=0; i<args.length; i++) {
                if (args[i].equals("-h")) {
                    showUsage("HummingXMLGenerator");
                    System.exit(0);
                } else if (args[i].equals("-i")) {
                    if (args[++i].equals("-")) {
                        nif = NetworkInterfaceSelector.select();
                    } else {
                        nif = NetworkInterface.getByName(args[i]);
                    }

                    startIndex += 2;
                } else if (args[i].equals("-t")) {
                    config.put("GenerationType", args[++i]);
                    startIndex += 2;
                } else if (args[i].equals("-p")) {
                    config.put("PathPrefix", args[++i]);
                    startIndex += 2;
                } else {
                    break;
                }
            }
            
            Core core;
            
            if (nif != null) {
                Inet4Subnet subnet = new Inet4Subnet(nif);
                core = new Core(subnet);
            } else {
                core = new Core();
            }
            
            core.startService();
            Service service = new Service(core);

            for (int i = startIndex; i < args.length; i++) {
                DeviceElementGenerator generator = new DeviceElementGenerator(service, service.getRemoteNode(args[i]), config);
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
