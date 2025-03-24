package newOs.kernel.memory.allocation;

import newOs.exception.MemoryAllocationException;
import newOs.kernel.memory.model.PhysicalAddress;
import java.util.Map;

/**
 * 内存分配器接口
 * 定义内存分配和回收的基本操作
 */
public interface MemoryAllocator {
    
    /**
     * 分配指定大小的内存
     * @param size 请求分配的内存大小（字节）
     * @param pid 进程ID
     * @return 分配的内存块的起始物理地址
     * @throws MemoryAllocationException 如果内存分配失败
     */
    PhysicalAddress allocate(int size, int pid) throws MemoryAllocationException;
    
    /**
     * 释放指定地址的内存
     * @param address 要释放的内存的起始物理地址
     * @param pid 进程ID（用于验证）
     * @return 是否成功释放
     */
    boolean free(PhysicalAddress address, int pid);
    
    /**
     * 释放指定进程的所有内存
     * @param pid 进程ID
     * @return 释放的内存块数量
     */
    int freeAll(int pid);
    
    /**
     * 获取空闲内存总量
     * @return 空闲内存总量（字节）
     */
    int getFreeMemorySize();
    
    /**
     * 获取已分配内存总量
     * @return 已分配内存总量（字节）
     */
    int getAllocatedMemorySize();
    
    /**
     * 获取最大连续空闲内存块的大小
     * @return 最大连续空闲内存块大小（字节）
     */
    int getLargestFreeBlockSize();
    
    /**
     * 获取指定进程已分配的内存大小
     * @param pid 进程ID
     * @return 进程已分配的内存大小（字节）
     */
    int getProcessAllocatedSize(int pid);
    
    /**
     * 获取内存碎片率
     * 内存碎片率 = 1 - (最大连续空闲块大小 / 总空闲内存大小)
     * @return 内存碎片率（0到1之间的值）
     */
    double getFragmentationRatio();
    
    /**
     * 获取内存使用率
     * 内存使用率 = 已分配内存大小 / 总内存大小
     * @return 内存使用率（0到1之间的值）
     */
    double getUsageRatio();
    
    /**
     * 检查指定地址是否在指定进程的分配范围内
     * @param address 物理地址
     * @param pid 进程ID
     * @return 是否在进程的分配范围内
     */
    boolean isAddressInProcessRange(PhysicalAddress address, int pid);
    
    /**
     * 获取内存映射的字符串表示
     * @return 内存映射字符串
     */
    String getMemoryMapString();
    
    /**
     * 获取内存分配器名称
     * @return 分配器名称
     */
    String getAllocatorName();
    
    /**
     * 查找指定物理地址对应的内存块
     * @param address 物理地址
     * @param pid 进程ID
     * @return 内存块对象，如果不存在则返回null
     */
    MemoryBlock findMemoryBlock(PhysicalAddress address, int pid);
    
    /**
     * 分配内存块并返回对应的内存块对象
     * @param size 大小（字节）
     * @param pid 进程ID
     * @return 内存块对象
     * @throws MemoryAllocationException 如果分配失败
     */
    MemoryBlock allocateBlock(int size, int pid) throws MemoryAllocationException;
    
    /**
     * 设置内存分配策略
     * @param strategyName 策略名称
     * @param parameters 策略参数
     */
    default void setAllocationStrategy(String strategyName, Map<String, Object> parameters) {
        // 默认实现，子类可以覆盖
        throw new UnsupportedOperationException("此分配器不支持动态更改分配策略");
    }
} 