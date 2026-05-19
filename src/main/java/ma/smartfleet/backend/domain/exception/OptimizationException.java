package ma.smartfleet.backend.domain.exception;

public class OptimizationException extends SmartFleetException {
    
    public OptimizationException(String message) {
        super(message);
    }
    
    public OptimizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
