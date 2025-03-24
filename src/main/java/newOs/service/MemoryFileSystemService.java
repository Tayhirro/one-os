package newOs.service;

import newOs.exception.MemoryAllocationException;
import newOs.exception.MemoryException;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;

import java.util.List;
import java.util.Map;

/**
 * 文件系统内存服务接口
 * 提供文件系统与内存交互的服务，包括内存映射文件、页缓存等
 */
public interface MemoryFileSystemService {

    /**
     * 初始化文件系统内存缓冲区
     * @param size 缓冲区大小（字节）
     * @throws MemoryException 内存异常
     */
    void initializeFileSystemBuffer(long size) throws MemoryException;
    
    /**
     * 为文件分配内存
     * @param size 需要分配的内存大小（字节）
     * @return 分配的虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress allocateFileMemory(long size) throws MemoryException;
    
    /**
     * 从指定虚拟地址读取文件数据
     * @param address 文件在内存中的虚拟地址
     * @param offset 读取的偏移量
     * @param length 读取的长度
     * @return 读取的数据
     * @throws MemoryException 内存异常
     */
    byte[] readFileData(VirtualAddress address, int offset, int length) throws MemoryException;
    
    /**
     * 将数据写入到指定虚拟地址
     * @param address 文件在内存中的虚拟地址
     * @param data 要写入的数据
     * @param offset 写入的偏移量
     * @throws MemoryException 内存异常
     */
    void writeFileData(VirtualAddress address, byte[] data, int offset) throws MemoryException;
    
    /**
     * 释放文件内存
     * @param address 文件在内存中的虚拟地址
     * @return 是否成功释放
     * @throws MemoryException 内存异常
     */
    boolean freeFileMemory(VirtualAddress address) throws MemoryException;

    /**
     * 为文件系统缓存分配内存
     * @param size 需要分配的内存大小（字节）
     * @return 分配的物理地址
     * @throws MemoryAllocationException 内存分配异常
     */
    PhysicalAddress allocateFileSystemCache(long size) throws MemoryAllocationException;

    /**
     * 释放文件系统缓存内存
     * @param physicalAddress 物理地址
     * @throws MemoryException 内存异常
     */
    void freeFileSystemCache(PhysicalAddress physicalAddress) throws MemoryException;

    /**
     * 将文件内容映射到进程内存空间
     * @param processId 进程ID
     * @param fileId 文件ID
     * @param fileOffset 文件偏移量
     * @param size 映射大小（字节）
     * @param isPrivate 是否为私有映射（写时复制）
     * @param isReadOnly 是否为只读映射
     * @return 映射到的虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress mapFileToMemory(int processId, int fileId, long fileOffset, long size, 
                                  boolean isPrivate, boolean isReadOnly) throws MemoryException;

    /**
     * 解除文件映射
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 映射大小（字节）
     * @throws MemoryException 内存异常
     */
    void unmapFileFromMemory(int processId, VirtualAddress virtualAddress, long size) throws MemoryException;

    /**
     * 将文件数据预加载到页缓存
     * @param fileId 文件ID
     * @param fileOffset 文件偏移量
     * @param size 预加载大小（字节）
     * @throws MemoryException 内存异常
     */
    void preloadFileToPageCache(int fileId, long fileOffset, long size) throws MemoryException;

    /**
     * 从页缓存移除文件数据
     * @param fileId 文件ID
     * @param fileOffset 文件偏移量
     * @param size 移除大小（字节）
     * @throws MemoryException 内存异常
     */
    void removeFileFromPageCache(int fileId, long fileOffset, long size) throws MemoryException;

    /**
     * 刷新页缓存中的文件数据到存储设备
     * @param fileId 文件ID
     * @param fileOffset 文件偏移量
     * @param size 刷新大小（字节）
     * @throws MemoryException 内存异常
     */
    void flushPageCache(int fileId, long fileOffset, long size) throws MemoryException;

    /**
     * 将整个页缓存刷新到存储设备
     * @throws MemoryException 内存异常
     */
    void flushAllPageCache() throws MemoryException;

    /**
     * 创建共享内存区域
     * @param name 共享内存名称
     * @param size 共享内存大小（字节）
     * @throws MemoryException 内存异常
     */
    void createSharedMemory(String name, long size) throws MemoryException;

    /**
     * 删除共享内存区域
     * @param name 共享内存名称
     * @throws MemoryException 内存异常
     */
    void removeSharedMemory(String name) throws MemoryException;

    /**
     * 将共享内存映射到进程地址空间
     * @param processId 进程ID
     * @param sharedMemoryName 共享内存名称
     * @param offset 偏移量
     * @param size 映射大小（字节）
     * @param isReadOnly 是否只读
     * @return 映射到的虚拟地址
     * @throws MemoryException 内存异常
     */
    VirtualAddress mapSharedMemory(int processId, String sharedMemoryName, long offset, 
                                  long size, boolean isReadOnly) throws MemoryException;

    /**
     * 解除共享内存映射
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @throws MemoryException 内存异常
     */
    void unmapSharedMemory(int processId, VirtualAddress virtualAddress) throws MemoryException;

    /**
     * 获取页缓存统计信息
     * @return 统计信息映射表
     */
    Map<String, Object> getPageCacheStats();

    /**
     * 获取页缓存命中率
     * @return 命中率（0-1之间的小数）
     */
    double getPageCacheHitRatio();

    /**
     * 获取页缓存当前大小
     * @return 页缓存大小（字节）
     */
    long getPageCacheSize();

    /**
     * 设置页缓存最大大小
     * @param maxSize 最大大小（字节）
     */
    void setPageCacheMaxSize(long maxSize);

    /**
     * 获取页缓存中的文件列表
     * @return 文件ID列表
     */
    List<Integer> getCachedFiles();

    /**
     * 检查文件是否在页缓存中
     * @param fileId 文件ID
     * @param offset 偏移量
     * @param size 检查范围大小
     * @return 是否在缓存中
     */
    boolean isFileInPageCache(int fileId, long offset, long size);

    /**
     * 获取文件在页缓存中的比例
     * @param fileId 文件ID
     * @return 缓存比例（0-1之间的小数）
     */
    double getFileCacheRatio(int fileId);

    /**
     * 锁定文件在页缓存中
     * @param fileId 文件ID
     * @param offset 偏移量
     * @param size 锁定大小
     * @throws MemoryException 内存异常
     */
    void lockFileInPageCache(int fileId, long offset, long size) throws MemoryException;

    /**
     * 解锁文件在页缓存中的锁定
     * @param fileId 文件ID
     * @param offset 偏移量
     * @param size 解锁大小
     * @throws MemoryException 内存异常
     */
    void unlockFileInPageCache(int fileId, long offset, long size) throws MemoryException;
} 