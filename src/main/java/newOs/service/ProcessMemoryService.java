package newOs.service;

import newOs.exception.MemoryAllocationException;
import newOs.exception.MemoryException;
import newOs.exception.MemoryProtectionException;
import newOs.kernel.memory.model.VirtualAddress;

import java.util.List;
import java.util.Map;

/**
 * 进程内存服务接口
 * 提供进程内存空间管理功能
 */
public interface ProcessMemoryService {

    /**
     * 为进程分配内存
     * @param processId 进程ID
     * @param size 要分配的内存大小（字节）
     * @param aligned 是否页对齐
     * @return 分配的虚拟地址
     * @throws MemoryAllocationException 内存分配异常
     */
    VirtualAddress allocateMemory(int processId, long size, boolean aligned) throws MemoryAllocationException;

    /**
     * 释放进程内存
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @throws MemoryException 内存异常
     */
    void freeMemory(int processId, VirtualAddress virtualAddress) throws MemoryException;

    /**
     * 重新分配内存块大小
     * @param processId 进程ID
     * @param virtualAddress 原内存块虚拟地址
     * @param newSize 新的大小（字节）
     * @return 重新分配后的虚拟地址（可能与原地址不同）
     * @throws MemoryAllocationException 内存分配异常
     */
    VirtualAddress reallocateMemory(int processId, VirtualAddress virtualAddress, long newSize) throws MemoryAllocationException;

    /**
     * 创建进程的内存空间
     * @param processId 进程ID
     * @param heapSize 堆初始大小（字节）
     * @param stackSize 栈大小（字节）
     * @throws MemoryException 内存异常
     */
    void createProcessMemorySpace(int processId, long heapSize, long stackSize) throws MemoryException;

    /**
     * 销毁进程的内存空间
     * @param processId 进程ID
     * @throws MemoryException 内存异常
     */
    void destroyProcessMemorySpace(int processId) throws MemoryException;

    /**
     * 扩展进程堆空间
     * @param processId 进程ID
     * @param additionalSize 额外大小（字节）
     * @return 扩展后的堆顶虚拟地址
     * @throws MemoryAllocationException 内存分配异常
     */
    VirtualAddress expandHeap(int processId, long additionalSize) throws MemoryAllocationException;

    /**
     * 收缩进程堆空间
     * @param processId 进程ID
     * @param reduceSize 减少大小（字节）
     * @return 收缩后的堆顶虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress shrinkHeap(int processId, long reduceSize) throws MemoryException;

    /**
     * 在进程地址空间创建内存映射
     * @param processId 进程ID
     * @param size 映射大小（字节）
     * @param protection 保护标志（r/w/x）
     * @param shared 是否可共享
     * @param fileBackedMapping 是否文件映射（若否则为匿名映射）
     * @param fileId 文件ID（如果是文件映射）
     * @param fileOffset 文件偏移（如果是文件映射）
     * @return 映射的虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress createMemoryMapping(int processId, long size, String protection, 
                                     boolean shared, boolean fileBackedMapping, 
                                     int fileId, long fileOffset) throws MemoryException;

    /**
     * 移除内存映射
     * @param processId 进程ID
     * @param virtualAddress 映射的虚拟地址
     * @param size 映射大小
     * @throws MemoryException 内存异常
     */
    void removeMemoryMapping(int processId, VirtualAddress virtualAddress, long size) throws MemoryException;

    /**
     * 设置内存区域保护属性
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 区域大小
     * @param protection 保护标志（r/w/x）
     * @throws MemoryProtectionException 内存保护异常
     */
    void setMemoryProtection(int processId, VirtualAddress virtualAddress, long size, String protection) throws MemoryProtectionException;

    /**
     * 锁定进程内存区域（防止被交换出去）
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 区域大小
     * @throws MemoryException 内存异常
     */
    void lockMemory(int processId, VirtualAddress virtualAddress, long size) throws MemoryException;

    /**
     * 解锁进程内存区域
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 区域大小
     * @throws MemoryException 内存异常
     */
    void unlockMemory(int processId, VirtualAddress virtualAddress, long size) throws MemoryException;

    /**
     * 同步内存区域到文件（对于文件映射）
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 区域大小
     * @throws MemoryException 内存异常
     */
    void syncMemory(int processId, VirtualAddress virtualAddress, long size) throws MemoryException;

    /**
     * 预取内存页
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 区域大小
     * @throws MemoryException 内存异常
     */
    void prefetchMemory(int processId, VirtualAddress virtualAddress, long size) throws MemoryException;

    /**
     * 获取进程内存使用统计
     * @param processId 进程ID
     * @return 内存使用统计
     * @throws MemoryException 内存异常
     */
    Map<String, Object> getProcessMemoryStats(int processId) throws MemoryException;

    /**
     * 检查内存地址是否有效
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否有效
     */
    boolean isValidAddress(int processId, VirtualAddress virtualAddress);

    /**
     * 在进程之间共享内存区域
     * @param sourceProcessId 源进程ID
     * @param virtualAddress 源进程中的虚拟地址
     * @param size 共享区域大小
     * @param targetProcessId 目标进程ID
     * @param protection 访问权限
     * @return 目标进程中映射的虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress shareMemoryBetweenProcesses(int sourceProcessId, VirtualAddress virtualAddress, 
                                            long size, int targetProcessId, String protection) throws MemoryException;

    /**
     * 复制内存区域到新进程（写时复制）
     * @param sourceProcessId 源进程ID
     * @param virtualAddress 源进程中的虚拟地址
     * @param size 复制区域大小
     * @param targetProcessId 目标进程ID
     * @return 目标进程中映射的虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress copyOnWriteMemory(int sourceProcessId, VirtualAddress virtualAddress, 
                                   long size, int targetProcessId) throws MemoryException;

    /**
     * 获取进程的堆顶地址
     * @param processId 进程ID
     * @return 堆顶虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress getProcessHeapTop(int processId) throws MemoryException;

    /**
     * 获取进程的栈底地址
     * @param processId 进程ID
     * @return 栈底虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress getProcessStackBottom(int processId) throws MemoryException;

    /**
     * 获取进程内存映射布局
     * @param processId 进程ID
     * @return 内存映射布局
     * @throws MemoryException 内存异常
     */
    String getProcessMemoryLayout(int processId) throws MemoryException;
} 