package newOs.exception;

/**
 * 内存分配异常
 * 当内存分配请求无法满足时抛出
 */
public class MemoryAllocationException extends MemoryException {
    
    // 请求分配的内存大小（字节）
    private final int requestedSize;
    
    // 进程ID，如果适用
    private final int pid;
    
    // 分配失败原因
    private final String reason;
    
    /**
     * 构造内存分配异常
     * @param requestedSize 请求分配的内存大小
     * @param reason 分配失败原因
     */
    public MemoryAllocationException(int requestedSize, String reason) {
        super(String.format("Memory allocation failed: Cannot allocate %d bytes, reason: %s", 
                requestedSize, reason));
        this.requestedSize = requestedSize;
        this.pid = -1;  // 不适用时设为-1
        this.reason = reason;
    }
    
    /**
     * 构造内存分配异常
     * @param requestedSize 请求分配的内存大小
     * @param pid 进程ID
     * @param reason 分配失败原因
     */
    public MemoryAllocationException(int requestedSize, int pid, String reason) {
        super(String.format("Memory allocation failed: Process %d cannot allocate %d bytes, reason: %s", 
                pid, requestedSize, reason));
        this.requestedSize = requestedSize;
        this.pid = pid;
        this.reason = reason;
    }
    
    /**
     * 构造内存分配异常
     * @param message 异常信息
     * @param requestedSize 请求分配的内存大小
     * @param pid 进程ID
     * @param reason 分配失败原因
     */
    public MemoryAllocationException(String message, int requestedSize, int pid, String reason) {
        super(message);
        this.requestedSize = requestedSize;
        this.pid = pid;
        this.reason = reason;
    }
    
    /**
     * 获取请求分配的内存大小
     * @return 内存大小（字节）
     */
    public int getRequestedSize() {
        return requestedSize;
    }
    
    /**
     * 获取进程ID
     * @return 进程ID，如果不适用则返回-1
     */
    public int getPid() {
        return pid;
    }
    
    /**
     * 获取分配失败原因
     * @return 失败原因
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * 判断是否是由于内存不足导致的分配失败
     * @return 是否是内存不足导致的
     */
    public boolean isOutOfMemory() {
        return "OUT_OF_MEMORY".equals(reason);
    }
    
    /**
     * 判断是否是由于内存碎片导致的分配失败
     * @return 是否是内存碎片导致的
     */
    public boolean isMemoryFragmentation() {
        return "MEMORY_FRAGMENTATION".equals(reason);
    }
    
    /**
     * 构造内存分配异常
     * @param message 异常信息
     */
    public MemoryAllocationException(String message) {
        super(message);
        this.requestedSize = -1;
        this.pid = -1;
        this.reason = "UNKNOWN";
    }
    
    /**
     * 构造内存分配异常
     * @param message 异常信息
     * @param cause 原始异常
     */
    public MemoryAllocationException(String message, Throwable cause) {
        super(message, cause);
        this.requestedSize = -1;
        this.pid = -1;
        this.reason = cause != null ? cause.getMessage() : "UNKNOWN";
    }
} 