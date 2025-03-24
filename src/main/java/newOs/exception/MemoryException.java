package newOs.exception;

/**
 * 内存异常基类
 * 所有内存相关异常的父类
 */
public class MemoryException extends RuntimeException {
    
    /**
     * 无参构造函数
     */
    public MemoryException() {
        super();
    }
    
    /**
     * 带异常信息的构造函数
     * @param message 异常信息
     */
    public MemoryException(String message) {
        super(message);
    }
    
    /**
     * 带异常信息和原因的构造函数
     * @param message 异常信息
     * @param cause 导致异常的原因
     */
    public MemoryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 带原因的构造函数
     * @param cause 导致异常的原因
     */
    public MemoryException(Throwable cause) {
        super(cause);
    }
} 