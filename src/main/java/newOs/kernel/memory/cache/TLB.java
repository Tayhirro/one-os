package newOs.kernel.memory.cache;

import newOs.exception.TLBMissException;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;

/**
 * TLB (Translation Lookaside Buffer) 接口
 * 定义TLB的基本操作，用于加速虚拟地址到物理地址的转换
 */
public interface TLB {
    
    /**
     * 在TLB中查找虚拟地址的映射
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @return 对应的TLB条目
     * @throws TLBMissException 如果TLB中未找到对应条目
     */
    TLBEntry lookup(int pid, VirtualAddress virtualAddress) throws TLBMissException;
    
    /**
     * 将虚拟地址映射到物理地址
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @param frameNumber 页帧号
     * @param readable 是否可读
     * @param writable 是否可写
     * @param executable 是否可执行
     */
    void insert(int pid, VirtualAddress virtualAddress, int frameNumber, 
               boolean readable, boolean writable, boolean executable);
    
    /**
     * 将虚拟地址直接映射到物理地址
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @param frameNumber 页帧号
     */
    default void insert(int pid, VirtualAddress virtualAddress, int frameNumber) {
        insert(pid, virtualAddress, frameNumber, true, true, false);
    }
    
    /**
     * 更新虚拟地址到物理地址的映射
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @param physicalAddress 物理地址
     * @param readable 是否可读
     * @param writable 是否可写
     * @param executable 是否可执行
     */
    default void update(int pid, VirtualAddress virtualAddress, PhysicalAddress physicalAddress, 
                      boolean readable, boolean writable, boolean executable) {
        insert(pid, virtualAddress, (int)physicalAddress.getFrameNumber(), 
               readable, writable, executable);
    }
    
    /**
     * 将TLB条目添加到TLB中
     * @param entry TLB条目
     */
    void addEntry(TLBEntry entry);
    
    /**
     * 使指定进程的所有TLB条目无效
     * @param pid 进程ID
     * @return 被无效化的条目数量
     */
    int invalidateAll(int pid);
    
    /**
     * 使指定进程的特定虚拟地址的TLB条目无效
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否成功无效化
     */
    boolean invalidate(int pid, VirtualAddress virtualAddress);
    
    /**
     * 清空TLB
     */
    void flush();
    
    /**
     * 获取TLB的命中率
     * @return 命中率（0-1之间的浮点数）
     */
    double getHitRatio();
    
    /**
     * 获取TLB的命中次数
     * @return 命中次数
     */
    long getHitCount();
    
    /**
     * 获取TLB的访问次数
     * @return 访问次数
     */
    long getAccessCount();
    
    /**
     * 获取TLB的当前大小
     * @return 当前大小
     */
    int getSize();
    
    /**
     * 获取TLB的容量
     * @return 容量
     */
    int getCapacity();
    
    /**
     * 重置统计信息
     */
    void resetStats();
    
    /**
     * 获取TLB的名称
     * @return TLB名称
     */
    String getName();
    
    /**
     * 获取TLB统计信息的字符串表示
     * @return 统计信息字符串
     */
    String getStatsInfo();
} 