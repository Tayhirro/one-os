package newOs.dto.req.Info.InfoImplDTO;

import lombok.Data;
import lombok.experimental.Accessors;
import newOs.common.InterruptConstant.InterruptType;
import newOs.dto.req.Info.InterruptInfo;
import newOs.kernel.memory.model.VirtualAddress;

/**
 * 中断上下文DTO
 * 包含中断处理所需的上下文信息
 */
@Data
@Accessors(chain = true)
public class InterruptContextDTO implements InterruptInfo {
    
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
     * 虚拟地址对象
     */
    private VirtualAddress virtualAddress;
    
    /**
     * 创建中断上下文DTO
     * @param processId 进程ID
     * @param faultAddress 故障地址
     * @param writeAccess 是否为写操作
     * @param executeAccess 是否为执行操作
     * @param errorCode 错误码
     * @param interruptType 中断类型
     */
    public InterruptContextDTO(int processId, long faultAddress, boolean writeAccess, 
                           boolean executeAccess, int errorCode, InterruptType interruptType) {
        this.processId = processId;
        this.faultAddress = faultAddress;
        this.writeAccess = writeAccess;
        this.executeAccess = executeAccess;
        this.errorCode = errorCode;
        this.interruptType = interruptType;
        this.virtualAddress = new VirtualAddress(faultAddress);
    }
    
    /**
     * 默认构造函数
     */
    public InterruptContextDTO() {
    }
    
    /**
     * 获取中断类型
     * @return 中断类型
     */
    @Override
    public InterruptType getInterruptType() {
        return interruptType;
    }
} 