package newOs.kernel.memory.cache;

import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;

/**
 * 缓存写策略接口
 * 定义不同的缓存写入策略，包括写直达(Write-Through)和写回(Write-Back)
 */
public interface CacheWritePolicy {
    
    /**
     * 策略类型枚举
     */
    enum PolicyType {
        /**
         * 写直达策略：数据同时写入缓存和内存
         */
        WRITE_THROUGH,
        
        /**
         * 写回策略：数据仅写入缓存，延迟写入内存
         */
        WRITE_BACK,
        
        /**
         * 写分配策略：写未命中时将数据加载到缓存中
         */
        WRITE_ALLOCATE,
        
        /**
         * 非写分配策略：写未命中时不将数据加载到缓存中
         */
        NO_WRITE_ALLOCATE
    }
    
    /**
     * 获取策略类型
     * @return 策略类型
     */
    PolicyType getPolicyType();
    
    /**
     * 写入操作
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @param physicalAddress 物理地址
     * @param data 数据
     * @param size 数据大小
     * @return 是否写入成功
     */
    boolean write(int pid, VirtualAddress virtualAddress, PhysicalAddress physicalAddress, 
                 byte[] data, int size);
    
    /**
     * 读取操作
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @param physicalAddress 物理地址
     * @param size 数据大小
     * @return 读取的数据
     */
    byte[] read(int pid, VirtualAddress virtualAddress, PhysicalAddress physicalAddress, 
               int size);
    
    /**
     * 刷新指定地址的缓存数据到内存
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否刷新成功
     */
    boolean flush(int pid, VirtualAddress virtualAddress);
    
    /**
     * 刷新指定进程的所有缓存数据到内存
     * @param pid 进程ID
     * @return 刷新的条目数
     */
    int flushAll(int pid);
    
    /**
     * 使指定地址的缓存条目失效
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否使失效成功
     */
    boolean invalidate(int pid, VirtualAddress virtualAddress);
    
    /**
     * 使指定进程的所有缓存条目失效
     * @param pid 进程ID
     * @return 使失效的条目数
     */
    int invalidateAll(int pid);
    
    /**
     * 获取脏项数量
     * @return 脏项数量
     */
    int getDirtyCount();
    
    /**
     * 获取写操作统计信息
     * @return 统计信息字符串
     */
    String getWriteStatsInfo();
} 