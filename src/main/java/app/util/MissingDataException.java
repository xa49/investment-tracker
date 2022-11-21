package app.util;

public class MissingDataException extends RuntimeException{
    public MissingDataException(String message) {
        super(message);
    }

    public MissingDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
