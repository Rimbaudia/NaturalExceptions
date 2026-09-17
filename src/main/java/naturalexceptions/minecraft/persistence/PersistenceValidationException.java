package naturalexceptions.minecraft.persistence;

public final class PersistenceValidationException extends RuntimeException {

    public PersistenceValidationException(String message) {
        super(message);
    }

    public PersistenceValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
