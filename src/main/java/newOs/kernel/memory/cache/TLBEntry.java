package newOs.kernel.memory.cache;

import lombok.Data;
import newOs.kernel.memory.model.VirtualAddress;

/**
 * TLB条目
 * 存储虚拟地址到物理地址的映射关系
 */
@Data
public class TLBEntry {
    // 进程ID
    private final int processId;
    
    // 虚拟地址
    private final VirtualAddress virtualAddress;
    
    // 物理页帧号
    private final int frameNumber;
    
    // 是否可读
    private final boolean readable;
    
    // 是否可写
    private final boolean writable;
    
    // 是否可执行
    private final boolean executable;
    
    // 最后访问时间
    private long lastAccessTime;
    
    // 是否有效
    private boolean valid = true;
    
    /**
     * 构造TLB条目
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param frameNumber 物理页帧号
     * @param readable 是否可读
     * @param writable 是否可写
     * @param executable 是否可执行
     */
    public TLBEntry(int processId, VirtualAddress virtualAddress, int frameNumber,
                    boolean readable, boolean writable, boolean executable) {
        this.processId = processId;
        this.virtualAddress = virtualAddress;
        this.frameNumber = frameNumber;
        this.readable = readable;
        this.writable = writable;
        this.executable = executable;
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    /**
     * 更新访问时间
     */
    public void updateAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    /**
     * 标记为已访问
     */
    public void markAccessed() {
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    /**
     * 检查是否匹配
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否匹配
     */
    public boolean matches(int processId, VirtualAddress virtualAddress) {
        return this.processId == processId && 
               this.virtualAddress.getPageNumber() == virtualAddress.getPageNumber();
    }
    
    /**
     * 检查条目是否有效
     * @return 是否有效
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * 使条目无效
     */
    public void invalidate() {
        this.valid = false;
    }
    
    /**
     * 获取进程ID
     * @return 进程ID
     */
    public int getPid() {
        return processId;
    }
    
    /**
     * 获取页号
     * @return 页号
     */
    public int getPageNumber() {
        return virtualAddress.getPageNumber();
    }
} 