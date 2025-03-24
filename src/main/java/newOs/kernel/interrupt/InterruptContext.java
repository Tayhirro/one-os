package newOs.kernel.interrupt;

import lombok.Data;
import newOs.kernel.memory.model.VirtualAddress;

/**
 * 中断上下文类
 * 包含中断处理所需的上下文信息
 */
@Data
public class InterruptContext {
    
    /**
     * 进程ID
     */
    private int processId;
    
    /**
     * 故障地址
     */
    private long faultAddress;
    
    /**
     * 是否为写操作
     */
    private boolean writeAccess;
    
    /**
     * 是否为执行操作
     */
    private boolean executeAccess;
    
    /**
     * 错误码
     */
    private int errorCode;
    
    /**
     * 中断类型
     */
    private InterruptType interruptType;
    
    /**
     * 附加信息
     */
    private Object additionalInfo;
    
    /**
     * 创建中断上下文
     * @param processId 进程ID
     * @param faultAddress 故障地址
     * @param writeAccess 是否为写操作
     * @param executeAccess 是否为执行操作
     * @param errorCode 错误码
     * @param interruptType 中断类型
     */
    public InterruptContext(int processId, long faultAddress, boolean writeAccess, 
                           boolean executeAccess, int errorCode, InterruptType interruptType) {
        this.processId = processId;
        this.faultAddress = faultAddress;
        this.writeAccess = writeAccess;
        this.executeAccess = executeAccess;
        this.errorCode = errorCode;
        this.interruptType = interruptType;
    }
    
    /**
     * 获取虚拟地址对象
     * @return 虚拟地址对象
     */
    public VirtualAddress getVirtualAddress() {
        return new VirtualAddress(faultAddress);
    }
    
    /**
     * 默认构造函数
     */
    public InterruptContext() {
    }
} 