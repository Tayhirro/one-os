package newOs.exception;

import newOs.kernel.memory.model.VirtualAddress;

/**
 * TLB未命中异常
 * 当在TLB中找不到虚拟地址的映射时抛出
 */
public class TLBMissException extends AddressTranslationException {
    
    // TLB级别
    private final String tlbLevel;
    
    /**
     * 构造函数
     * @param message 异常消息
     */
    public TLBMissException(String message) {
        super(message, -1, null);
        this.tlbLevel = "未知";
    }
    
    /**
     * 构造函数
     * @param virtualAddress 虚拟地址
     * @param tlbLevel TLB级别
     * @param pid 进程ID
     * @param message 详细信息
     */
    public TLBMissException(VirtualAddress virtualAddress, String tlbLevel, int pid, String message) {
        super(message, pid, virtualAddress);
        this.tlbLevel = tlbLevel;
    }
    
    /**
     * 获取TLB级别
     * @return TLB级别
     */
    public String getTlbLevel() {
        return tlbLevel;
    }
} 