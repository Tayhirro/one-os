package newOs.exception;

import newOs.kernel.memory.model.VirtualAddress;

/**
 * 缺页异常
 * 当进程访问的页面不在内存中时抛出，需要执行页面调入
 */
public class PageFaultException extends MemoryException {
    
    // 引起缺页异常的虚拟地址
    private final VirtualAddress faultAddress;
    
    // 访问类型：读(READ)、写(WRITE)、执行(EXECUTE)
    private final String accessType;
    
    // 进程ID
    private final int pid;
    
    // 缺页原因：不存在(NOT_PRESENT)、权限不足(NO_PERMISSION)、交换出去(SWAPPED_OUT)
    private final String reason;
    
    /**
     * 构造缺页异常
     * @param faultAddress 引起缺页异常的虚拟地址
     * @param accessType 访问类型
     * @param pid 进程ID
     * @param reason 缺页原因
     */
    public PageFaultException(VirtualAddress faultAddress, String accessType, int pid, String reason) {
        super(String.format("Page fault: Process %d attempted to %s address %s, reason: %s", 
                pid, accessType, faultAddress, reason));
        this.faultAddress = faultAddress;
        this.accessType = accessType;
        this.pid = pid;
        this.reason = reason;
    }
    
    /**
     * 构造缺页异常
     * @param message 异常信息
     * @param faultAddress 引起缺页异常的虚拟地址
     * @param accessType 访问类型
     * @param pid 进程ID
     * @param reason 缺页原因
     */
    public PageFaultException(String message, VirtualAddress faultAddress, String accessType, int pid, String reason) {
        super(message);
        this.faultAddress = faultAddress;
        this.accessType = accessType;
        this.pid = pid;
        this.reason = reason;
    }
    
    /**
     * 获取引起缺页异常的虚拟地址
     * @return 虚拟地址
     */
    public VirtualAddress getFaultAddress() {
        return faultAddress;
    }
    
    /**
     * 获取访问类型
     * @return 访问类型
     */
    public String getAccessType() {
        return accessType;
    }
    
    /**
     * 获取进程ID
     * @return 进程ID
     */
    public int getPid() {
        return pid;
    }
    
    /**
     * 获取缺页原因
     * @return 缺页原因
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * 判断是否是由于页面不存在导致的缺页异常
     * @return 是否是页面不存在导致的
     */
    public boolean isNotPresent() {
        return "NOT_PRESENT".equals(reason);
    }
    
    /**
     * 判断是否是由于权限不足导致的缺页异常
     * @return 是否是权限不足导致的
     */
    public boolean isNoPermission() {
        return "NO_PERMISSION".equals(reason);
    }
    
    /**
     * 判断是否是由于页面已被交换出去导致的缺页异常
     * @return 是否是页面已被交换出去导致的
     */
    public boolean isSwappedOut() {
        return "SWAPPED_OUT".equals(reason);
    }
} 