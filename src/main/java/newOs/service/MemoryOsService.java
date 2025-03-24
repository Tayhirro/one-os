package newOs.service;

import newOs.exception.MemoryException;
import newOs.kernel.memory.model.MemoryRegion;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;

import java.util.List;
import java.util.Map;

/**
 * 操作系统内存服务接口
 * 提供给内核和驱动程序使用的内存管理服务
 */
public interface MemoryOsService {

    /**
     * 为内核分配内存
     * @param size 内存大小（字节）
     * @param aligned 是否页对齐
     * @return 分配的物理地址
     * @throws MemoryException 内存异常
     */
    PhysicalAddress allocateKernelMemory(long size, boolean aligned) throws MemoryException;

    /**
     * 释放内核内存
     * @param address 物理地址
     * @throws MemoryException 内存异常
     */
    void freeKernelMemory(PhysicalAddress address) throws MemoryException;

    /**
     * 映射物理地址到内核虚拟地址
     * @param physicalAddress 物理地址
     * @param size 映射大小（字节）
     * @param cacheType 缓存类型（例如：CACHED, UNCACHED, WRITE_COMBINING）
     * @return 映射的虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress mapToKernelSpace(PhysicalAddress physicalAddress, long size, String cacheType) throws MemoryException;

    /**
     * 解除内核虚拟地址映射
     * @param virtualAddress 虚拟地址
     * @throws MemoryException 内存异常
     */
    void unmapFromKernelSpace(VirtualAddress virtualAddress) throws MemoryException;

    /**
     * 分配内核栈空间
     * @param threadId 线程ID
     * @param stackSize 栈大小（字节）
     * @return 栈顶虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress allocateKernelStack(int threadId, long stackSize) throws MemoryException;

    /**
     * 释放内核栈空间
     * @param threadId 线程ID
     * @throws MemoryException 内存异常
     */
    void freeKernelStack(int threadId) throws MemoryException;

    /**
     * 从物理地址拷贝数据到内核缓冲区
     * @param srcPhysical 源物理地址
     * @param destVirtual 目标虚拟地址（内核空间）
     * @param length 长度（字节）
     * @throws MemoryException 内存异常
     */
    void copyFromPhysicalToKernel(PhysicalAddress srcPhysical, VirtualAddress destVirtual, long length) throws MemoryException;

    /**
     * 从内核缓冲区拷贝数据到物理地址
     * @param srcVirtual 源虚拟地址（内核空间）
     * @param destPhysical 目标物理地址
     * @param length 长度（字节）
     * @throws MemoryException 内存异常
     */
    void copyFromKernelToPhysical(VirtualAddress srcVirtual, PhysicalAddress destPhysical, long length) throws MemoryException;

    /**
     * 为设备分配连续的物理内存（DMA使用）
     * @param size 大小（字节）
     * @param alignment 对齐要求（字节）
     * @param dmaConstraints DMA约束（例如：最大物理地址限制）
     * @return 分配的物理地址
     * @throws MemoryException 内存异常
     */
    PhysicalAddress allocateDeviceMemory(long size, long alignment, Map<String, Object> dmaConstraints) throws MemoryException;

    /**
     * 释放设备内存
     * @param address 物理地址
     * @throws MemoryException 内存异常
     */
    void freeDeviceMemory(PhysicalAddress address) throws MemoryException;

    /**
     * 锁定用户空间内存（防止被交换出去）
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 大小（字节）
     * @throws MemoryException 内存异常
     */
    void lockUserMemory(int processId, VirtualAddress virtualAddress, long size) throws MemoryException;

    /**
     * 解锁用户空间内存
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 大小（字节）
     * @throws MemoryException 内存异常
     */
    void unlockUserMemory(int processId, VirtualAddress virtualAddress, long size) throws MemoryException;

    /**
     * 创建内核模块内存映射
     * @param moduleName 模块名称
     * @param size 大小（字节）
     * @return 映射的虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress createKernelModuleMapping(String moduleName, long size) throws MemoryException;

    /**
     * 删除内核模块内存映射
     * @param moduleName 模块名称
     * @throws MemoryException 内存异常
     */
    void removeKernelModuleMapping(String moduleName) throws MemoryException;

    /**
     * 创建内核临时映射
     * @param physicalAddress 物理地址
     * @param size 大小（字节）
     * @return 映射的虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress createKernelTemporaryMapping(PhysicalAddress physicalAddress, long size) throws MemoryException;

    /**
     * 删除内核临时映射
     * @param virtualAddress 虚拟地址
     * @throws MemoryException 内存异常
     */
    void removeKernelTemporaryMapping(VirtualAddress virtualAddress) throws MemoryException;

    /**
     * 获取内核内存使用统计
     * @return 内核内存使用统计
     */
    Map<String, Object> getKernelMemoryStatistics();

    /**
     * 获取内核内存区域列表
     * @return 内存区域列表
     */
    List<MemoryRegion> getKernelMemoryRegions();

    /**
     * 刷新处理器TLB
     * @param cpuId 处理器ID，-1表示所有处理器
     * @throws MemoryException 内存异常
     */
    void flushProcessorTLB(int cpuId) throws MemoryException;

    /**
     * 刷新处理器缓存
     * @param cpuId 处理器ID，-1表示所有处理器
     * @param cacheLevel 缓存级别（1=L1, 2=L2, 3=L3, 0=所有级别）
     * @throws MemoryException 内存异常
     */
    void flushProcessorCache(int cpuId, int cacheLevel) throws MemoryException;

    /**
     * 检查是否为内核地址
     * @param virtualAddress 虚拟地址
     * @return 是否为内核地址
     */
    boolean isKernelAddress(VirtualAddress virtualAddress);

    /**
     * 获取页框号
     * @param physicalAddress 物理地址
     * @return 页框号
     */
    long getPageFrameNumber(PhysicalAddress physicalAddress);

    /**
     * 从页框号获取物理地址
     * @param pageFrameNumber 页框号
     * @return 物理地址
     */
    PhysicalAddress getAddressFromPageFrameNumber(long pageFrameNumber);
} 