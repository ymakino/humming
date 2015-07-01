package humming.generator;

/**
 *
 * @author ymakino
 */
public class GeneratorException extends Exception {
    
    public GeneratorException(String message) {
        super(message);
    }
    
    public GeneratorException(String message, Throwable cause) {
        super(message, cause);
    }
}