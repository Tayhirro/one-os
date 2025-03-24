package newOs.exception;

import newOs.kernel.memory.model.VirtualAddress;

/**
 * 内存保护异常
 * 当内存访问权限校验失败时抛出
 */
public class MemoryProtectionException extends MemoryException {
    
    /**
     * 进程ID
     */
    private final int processId;
    
    /**
     * 访问的内存地址
     */
    private final long address;
    
    /**
     * 是否为执行操作
     */
    private final boolean execOperation;
    
    /**
     * 是否为读操作
     */
    private final boolean readOperation;
    
    /**
     * 是否为写操作
     */
    private final boolean writeOperation;
    
    /**
     * 构造内存保护异常
     * @param message 异常信息
     * @param processId 进程ID
     * @param address 访问的内存地址
     * @param execOperation 是否为执行操作
     * @param readOperation 是否为读操作
     * @param writeOperation 是否为写操作
     */
    public MemoryProtectionException(String message, int processId, long address,
                                   boolean execOperation, boolean readOperation, boolean writeOperation) {
        super(message);
        this.processId = processId;
        this.address = address;
        this.execOperation = execOperation;
        this.readOperation = readOperation;
        this.writeOperation = writeOperation;
    }
    
    /**
     * 构造内存保护异常
     * @param message 异常信息
     * @param processId 进程ID
     * @param virtualAddress 访问的虚拟地址
     * @param execOperation 是否为执行操作
     * @param readOperation 是否为读操作
     * @param writeOperation 是否为写操作
     */
    public MemoryProtectionException(String message, int processId, VirtualAddress virtualAddress,
                                   boolean execOperation, boolean readOperation, boolean writeOperation) {
        this(message, processId, virtualAddress.getValue(), execOperation, readOperation, writeOperation);
    }
    
    /**
     * 构造内存保护异常
     * @param message 异常信息
     * @param cause 原始异常
     */
    public MemoryProtectionException(String message, Throwable cause) {
        super(message, cause);
        this.processId = -1;
        this.address = 0;
        this.execOperation = false;
        this.readOperation = false;
        this.writeOperation = false;
    }
    
    /**
     * 构造内存保护异常（简化版本）
     * @param message 异常信息
     */
    public MemoryProtectionException(String message) {
        super(message);
        this.processId = -1;
        this.address = 0;
        this.execOperation = false;
        this.readOperation = false;
        this.writeOperation = false;
    }
    
    /**
     * 获取进程ID
     * @return 进程ID
     */
    public int getProcessId() {
        return processId;
    }
    
    /**
     * 获取访问的内存地址
     * @return 内存地址
     */
    public long getAddress() {
        return address;
    }
    
    /**
     * 是否为执行操作
     * @return 是否为执行操作
     */
    public boolean isExecOperation() {
        return execOperation;
    }
    
    /**
     * 是否为读操作
     * @return 是否为读操作
     */
    public boolean isReadOperation() {
        return readOperation;
    }
    
    /**
     * 是否为写操作
     * @return 是否为写操作
     */
    public boolean isWriteOperation() {
        return writeOperation;
    }
    
    @Override
    public String toString() {
        String operationType = "";
        if (readOperation) {
            operationType = "读取";
        } else if (writeOperation) {
            operationType = "写入";
        } else if (execOperation) {
            operationType = "执行";
        }
        
        return String.format("内存保护异常: 进程ID=%d, 地址=0x%016X, 操作类型=%s, 消息=%s",
                            processId, address, operationType, getMessage());
    }
} 