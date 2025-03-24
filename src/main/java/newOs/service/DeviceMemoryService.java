package newOs.service;

import newOs.exception.MemoryAllocationException;
import newOs.exception.MemoryException;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;

/**
 * 设备内存服务接口
 * 提供设备内存管理和DMA操作的服务
 */
public interface DeviceMemoryService {

    /**
     * 为设备分配连续物理内存
     * @param deviceId 设备ID
     * @param size 需要分配的字节数
     * @return 分配的物理地址
     * @throws MemoryAllocationException 内存分配异常
     */
    PhysicalAddress allocateDeviceMemory(int deviceId, long size) throws MemoryAllocationException;

    /**
     * 释放设备使用的内存
     * @param deviceId 设备ID
     * @param physicalAddress 物理地址
     * @throws MemoryException 内存异常
     */
    void freeDeviceMemory(int deviceId, PhysicalAddress physicalAddress) throws MemoryException;

    /**
     * 为设备分配DMA缓冲区
     * @param deviceId 设备ID
     * @param size 缓冲区大小（字节）
     * @return 缓冲区物理地址
     * @throws MemoryAllocationException 内存分配异常
     */
    PhysicalAddress allocateDMABuffer(int deviceId, long size) throws MemoryAllocationException;

    /**
     * 释放DMA缓冲区
     * @param deviceId 设备ID
     * @param physicalAddress 缓冲区物理地址
     * @throws MemoryException 内存异常
     */
    void freeDMABuffer(int deviceId, PhysicalAddress physicalAddress) throws MemoryException;

    /**
     * 将进程虚拟地址映射到设备可访问的物理地址（用于共享内存）
     * @param processId 进程ID
     * @param deviceId 设备ID
     * @param virtualAddress 进程虚拟地址
     * @param size 映射大小（字节）
     * @return 设备可访问的物理地址
     * @throws MemoryException 内存异常
     */
    PhysicalAddress mapProcessMemoryToDevice(int processId, int deviceId, VirtualAddress virtualAddress, long size) throws MemoryException;

    /**
     * 移除进程内存与设备的映射
     * @param processId 进程ID
     * @param deviceId 设备ID
     * @param virtualAddress 进程虚拟地址
     * @throws MemoryException 内存异常
     */
    void unmapProcessMemoryFromDevice(int processId, int deviceId, VirtualAddress virtualAddress) throws MemoryException;

    /**
     * 从设备内存读取数据到进程内存
     * @param processId 进程ID
     * @param deviceId 设备ID
     * @param deviceAddress 设备内存地址
     * @param processAddress 进程虚拟地址
     * @param size 传输大小（字节）
     * @throws MemoryException 内存异常
     */
    void readFromDeviceMemory(int processId, int deviceId, PhysicalAddress deviceAddress, 
                             VirtualAddress processAddress, long size) throws MemoryException;

    /**
     * 从进程内存写入数据到设备内存
     * @param processId 进程ID
     * @param deviceId 设备ID
     * @param processAddress 进程虚拟地址
     * @param deviceAddress 设备内存地址
     * @param size 传输大小（字节）
     * @throws MemoryException 内存异常
     */
    void writeToDeviceMemory(int processId, int deviceId, VirtualAddress processAddress, 
                            PhysicalAddress deviceAddress, long size) throws MemoryException;

    /**
     * 执行DMA操作，从设备内存传输到系统内存
     * @param deviceId 设备ID
     * @param sourceAddress 源地址（设备内存）
     * @param targetAddress 目标地址（系统内存）
     * @param size 传输大小（字节）
     * @throws MemoryException 内存异常
     */
    void performDMARead(int deviceId, PhysicalAddress sourceAddress, PhysicalAddress targetAddress, long size) throws MemoryException;

    /**
     * 执行DMA操作，从系统内存传输到设备内存
     * @param deviceId 设备ID
     * @param sourceAddress 源地址（系统内存）
     * @param targetAddress 目标地址（设备内存）
     * @param size 传输大小（字节）
     * @throws MemoryException 内存异常
     */
    void performDMAWrite(int deviceId, PhysicalAddress sourceAddress, PhysicalAddress targetAddress, long size) throws MemoryException;

    /**
     * 对设备内存进行缓存一致性操作
     * @param deviceId 设备ID
     * @param physicalAddress 物理地址
     * @param size 区域大小（字节）
     * @throws MemoryException 内存异常
     */
    void syncDeviceMemory(int deviceId, PhysicalAddress physicalAddress, long size) throws MemoryException;

    /**
     * 锁定设备内存（防止被分页或移动）
     * @param deviceId 设备ID
     * @param physicalAddress 物理地址
     * @param size 区域大小（字节）
     * @throws MemoryException 内存异常
     */
    void lockDeviceMemory(int deviceId, PhysicalAddress physicalAddress, long size) throws MemoryException;

    /**
     * 解锁设备内存
     * @param deviceId 设备ID
     * @param physicalAddress 物理地址
     * @param size 区域大小（字节）
     * @throws MemoryException 内存异常
     */
    void unlockDeviceMemory(int deviceId, PhysicalAddress physicalAddress, long size) throws MemoryException;

    /**
     * 获取设备内存使用情况
     * @param deviceId 设备ID
     * @return 设备内存使用量（字节）
     */
    long getDeviceMemoryUsage(int deviceId);

    /**
     * 检查设备内存是否足够
     * @param deviceId 设备ID
     * @param requiredSize 需要的内存大小（字节）
     * @return 是否有足够内存
     */
    boolean hasAvailableDeviceMemory(int deviceId, long requiredSize);
} 