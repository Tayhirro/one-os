package newOs.exception.ProcessException;


import lombok.Data;

@Data
public class ProcessException extends RuntimeException{
    private final String errorCode;

    public ProcessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }           //
}
