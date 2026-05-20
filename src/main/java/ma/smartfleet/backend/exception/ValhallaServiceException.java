package ma.smartfleet.backend.exception;

public class ValhallaServiceException extends SmartFleetException {
    public ValhallaServiceException(String message) { super(message); }
    public ValhallaServiceException(String message, Throwable cause) { super(message, cause); }
}
