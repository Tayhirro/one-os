package newOs.exception;

import newOs.kernel.memory.model.VirtualAddress;

/**
 * 地址转换异常
 * 当虚拟地址无法转换为物理地址时抛出
 */
public class AddressTranslationException extends MemoryException {
    
    // 无法转换的虚拟地址
    private final VirtualAddress virtualAddress;
    
    // 进程ID
    private final int pid;
    
    // 转换失败原因
    private final String reason;
    
    /**
     * 构造地址转换异常
     * @param virtualAddress 无法转换的虚拟地址
     * @param reason 转换失败原因
     */
    public AddressTranslationException(VirtualAddress virtualAddress, String reason) {
        super(String.format("Address translation failed: Cannot translate virtual address %s, reason: %s", 
                virtualAddress, reason));
        this.virtualAddress = virtualAddress;
        this.pid = -1; // VirtualAddress类中没有processId字段，需要由上层传入
        this.reason = reason;
    }
    
    /**
     * 构造地址转换异常
     * @param message 异常信息
     * @param virtualAddress 无法转换的虚拟地址
     * @param reason 转换失败原因
     */
    public AddressTranslationException(String message, VirtualAddress virtualAddress, String reason) {
        super(message);
        this.virtualAddress = virtualAddress;
        this.pid = -1; // VirtualAddress类中没有processId字段，需要由上层传入
        this.reason = reason;
    }
    
    /**
     * 构造地址转换异常
     * @param message 异常信息
     * @param cause 原始异常
     */
    public AddressTranslationException(String message, Throwable cause) {
        super(message, cause);
        this.virtualAddress = null;
        this.pid = -1;
        this.reason = cause != null ? cause.getMessage() : "UNKNOWN";
    }
    
    /**
     * 构造函数
     * @param message 异常消息
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     */
    public AddressTranslationException(String message, int processId, VirtualAddress virtualAddress) {
        super(message);
        this.pid = processId;
        this.virtualAddress = virtualAddress;
        this.reason = "UNKNOWN";
    }
    
    /**
     * 构造函数
     * @param message 异常消息
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param cause 原因异常
     */
    public AddressTranslationException(String message, int processId, VirtualAddress virtualAddress, Throwable cause) {
        super(message, cause);
        this.pid = processId;
        this.virtualAddress = virtualAddress;
        this.reason = cause != null ? cause.getMessage() : "UNKNOWN";
    }
    
    /**
     * 获取无法转换的虚拟地址
     * @return 虚拟地址
     */
    public VirtualAddress getVirtualAddress() {
        return virtualAddress;
    }
    
    /**
     * 获取进程ID
     * @return 进程ID
     */
    public int getPid() {
        return pid;
    }
    
    /**
     * 获取转换失败原因
     * @return 失败原因
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * 判断是否是由于无效地址导致的转换失败
     * @return 是否是无效地址导致的
     */
    public boolean isInvalidAddress() {
        return "INVALID_ADDRESS".equals(reason);
    }
    
    /**
     * 判断是否是由于页表不存在导致的转换失败
     * @return 是否是页表不存在导致的
     */
    public boolean isPageTableNotFound() {
        return "PAGE_TABLE_NOT_FOUND".equals(reason);
    }
    
    /**
     * 判断是否是由于段表不存在导致的转换失败
     * @return 是否是段表不存在导致的
     */
    public boolean isSegmentTableNotFound() {
        return "SEGMENT_TABLE_NOT_FOUND".equals(reason);
    }
} 