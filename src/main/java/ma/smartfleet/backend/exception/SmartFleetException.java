package ma.smartfleet.backend.exception;

public class SmartFleetException extends RuntimeException {
    public SmartFleetException(String message) { super(message); }
    public SmartFleetException(String message, Throwable cause) { super(message, cause); }
}
