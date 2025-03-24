package newOs.exception;

/**
 * 文件系统异常
 * 文件操作相关的异常
 */
public class FileException extends RuntimeException {
    
    /**
     * 无参构造函数
     */
    public FileException() {
        super();
    }
    
    /**
     * 带异常信息的构造函数
     * @param message 异常信息
     */
    public FileException(String message) {
        super(message);
    }
    
    /**
     * 带异常信息和原因的构造函数
     * @param message 异常信息
     * @param cause 导致异常的原因
     */
    public FileException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 带原因的构造函数
     * @param cause 导致异常的原因
     */
    public FileException(Throwable cause) {
        super(cause);
    }
} 