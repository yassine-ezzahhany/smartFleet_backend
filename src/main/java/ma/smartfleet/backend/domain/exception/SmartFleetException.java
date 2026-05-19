package ma.smartfleet.backend.domain.exception;

public class SmartFleetException extends RuntimeException {
    
    public SmartFleetException(String message) {
        super(message);
    }
    
    public SmartFleetException(String message, Throwable cause) {
        super(message, cause);
    }
}
