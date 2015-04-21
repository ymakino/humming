package humming;

/**
 * Hummingにエラーが発生した時に生成される例外
 * @author ymakino
 */
public class HummingException extends Exception {
    
    /**
     * HummingExceptionを生成する。
     * @param message エラーのメッセージ
     */
    public HummingException(String message) {
        super(message);
    }
    
    /**
     * HummingExceptionを生成する。
     * @param message エラーのメッセージ
     * @param cause 例外の原因
     */
    public HummingException(String message, Throwable cause) {
        super(message, cause);
    }
}
