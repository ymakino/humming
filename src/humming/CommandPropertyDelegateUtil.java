package humming;

import echowand.common.EPC;

/**
 *
 * @author ymakino
 */
public class CommandPropertyDelegateUtil {
    
    public static String epc2str(EPC epc) {
        return String.format("%02X", 0x00ff & epc.toByte());
    }
    
    public static String[] concatString(String[] array, String... strings) {
        String[] ret = new String[array.length + strings.length];
        System.arraycopy(array, 0, ret, 0, array.length);
        System.arraycopy(strings, 0, ret, array.length, strings.length);
        return ret;
    }
    
    public static String escapeString(String str) {
        StringBuilder builder = new StringBuilder();
        
        for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            
            switch (c) {
                case '"': case '\'': case '\\':
                    builder.append("\\").append(c);
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                default:
                    builder.append(c);
                    break;
            }
        }
        
        return builder.toString();
    }
    
    public static String joinString(String[] array) {
        if (array == null) {
            return null;
        }
        
        StringBuilder builder = new StringBuilder();
        
        for (int i=0; i<array.length; i++) {
            if (i != 0) {
                builder.append(" ");
            }
            builder.append('"').append(escapeString(array[i])).append('"');
        }
        
        return builder.toString();
    }
}
