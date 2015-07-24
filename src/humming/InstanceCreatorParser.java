package humming;

/**
 *
 * @author ymakino
 */
public class InstanceCreatorParser {
    public static final String DEFAULT_INSTANCE_NAME = "it";
    
    private String contents;
    private String header;
    private String instanceName;
    private String className;
    private String script;
    
    public InstanceCreatorParser(String contents) {
        this.contents = contents;
        
        String remain = contents.trim();
        
        int headerOffset = remain.indexOf('{');
        if (headerOffset == -1) {
            header = remain;
            script = null;
        } else {
            header = remain.substring(0, headerOffset).trim();
            script = remain.substring(headerOffset).trim();
        }
        
        int classNameOffset = header.indexOf(':');
        if (classNameOffset == -1) {
            instanceName = DEFAULT_INSTANCE_NAME;
            className = header;
        } else {
            instanceName = header.substring(0, classNameOffset).trim();
            className = header.substring(classNameOffset+1).trim();
        }
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getInstanceName() {
        return instanceName;
    }
    
    public String getScript() {
        return script;
    }
}
