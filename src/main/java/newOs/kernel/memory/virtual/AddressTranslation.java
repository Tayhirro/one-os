package newOs.kernel.memory.virtual;

import newOs.exception.AddressTranslationException;
import newOs.exception.PageFaultException;
import newOs.kernel.memory.allocation.MemoryBlock;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.exception.MemoryException;

/**
 * 地址转换接口
 * 负责虚拟地址与物理地址之间的转换
 */
public interface AddressTranslation {
    
    /**
     * 将虚拟地址转换为物理地址
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 物理地址
     * @throws AddressTranslationException 地址转换异常
     * @throws PageFaultException 缺页异常
     */
    PhysicalAddress translateToPhysical(int processId, VirtualAddress virtualAddress) 
            throws AddressTranslationException, PageFaultException;
    
    /**
     * 将内存块映射到虚拟地址空间
     * @param processId 进程ID
     * @param memoryBlock 内存块
     * @return 映射的虚拟地址
     * @throws AddressTranslationException 地址转换异常
     */
    VirtualAddress mapMemoryBlock(int processId, MemoryBlock memoryBlock)
            throws AddressTranslationException;
    
    /**
     * 解除内存块映射
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @throws AddressTranslationException 地址转换异常
     */
    void unmapMemoryBlock(int processId, VirtualAddress virtualAddress)
            throws AddressTranslationException;
    
    /**
     * 创建私有内存映射（写时复制）
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param physicalAddress 物理地址
     * @param size 映射大小
     * @throws AddressTranslationException 地址转换异常
     */
    void createPrivateMapping(int processId, VirtualAddress virtualAddress, 
                                  PhysicalAddress physicalAddress, long size)
            throws AddressTranslationException;
    
    /**
     * 创建共享内存映射
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param physicalAddress 物理地址
     * @param size 映射大小
     * @throws AddressTranslationException 地址转换异常
     */
    void createSharedMapping(int processId, VirtualAddress virtualAddress, 
                                PhysicalAddress physicalAddress, long size)
            throws AddressTranslationException;
    
    /**
     * 移除内存映射
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 映射大小
     * @throws AddressTranslationException 地址转换异常
     */
    void removeMapping(int processId, VirtualAddress virtualAddress, long size)
            throws AddressTranslationException;
    
    /**
     * 映射物理地址到虚拟地址
     * 创建虚拟地址和物理地址之间的映射关系
     * 
     * @param processId 进程ID
     * @param physicalAddress 物理地址
     * @param size 映射大小（字节）
     * @return 分配的虚拟地址
     * @throws MemoryException 如果映射失败
     */
    public VirtualAddress mapPhysicalAddress(int processId, PhysicalAddress physicalAddress, int size) throws MemoryException;
} 