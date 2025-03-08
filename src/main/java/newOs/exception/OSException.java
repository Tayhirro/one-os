package newOs.exception;

public class OSException extends RuntimeException{
    private final String errorCode;

    public OSException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
