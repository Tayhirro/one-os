package newOs.service.impl;

import lombok.extern.slf4j.Slf4j;
import newOs.exception.FileException;
import newOs.exception.MemoryAllocationException;
import newOs.exception.MemoryException;
import newOs.kernel.filesystem.node.INode;
import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.service.MemoryFileSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存文件系统服务实现
 * 负责文件数据与内存之间的映射和交互
 */
@Service
@Slf4j
public class MemoryFileSystemServiceImpl implements MemoryFileSystemService {

    private final MemoryManager memoryManager;
    
    @Value("${filesystem.block.size:4096}")
    private int blockSize = 4096;
    
    @Value("${filesystem.buffer.size:1048576}")
    private int fileSystemBufferSize = 1048576; // 1MB
    
    // 文件到内存映射，键为文件ID，值为内存地址
    private final Map<Long, PhysicalAddress> fileMemoryMap = new ConcurrentHashMap<>();
    
    // 虚拟地址映射
    private final Map<VirtualAddress, PhysicalAddress> virtualToPhysicalMap = new ConcurrentHashMap<>();
    
    // 共享内存区域
    private final Map<String, PhysicalAddress> sharedMemoryMap = new ConcurrentHashMap<>();
    
    // 页缓存
    private final Map<Integer, Map<Long, byte[]>> pageCache = new ConcurrentHashMap<>();
    private long pageCacheSize = 0;
    private long pageCacheMaxSize = 1024 * 1024 * 32; // 32MB
    private long pageCacheHits = 0;
    private long pageCacheMisses = 0;
    
    // 文件系统缓冲区
    private PhysicalAddress fileSystemBuffer;
    
    @Autowired
    public MemoryFileSystemServiceImpl(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }
    
    /**
     * 初始化文件系统缓冲区（内部使用方法）
     * @throws MemoryAllocationException 如果内存分配失败
     */
    public void initializeFileSystemBuffer() throws MemoryAllocationException {
        log.info("初始化文件系统缓冲区，大小: {} 字节", fileSystemBufferSize);
        fileSystemBuffer = memoryManager.allocatePhysicalMemory(fileSystemBufferSize);
        log.info("文件系统缓冲区初始化完成，物理地址: {}", fileSystemBuffer);
    }
    
    @Override
    public void initializeFileSystemBuffer(long size) throws MemoryException {
        log.info("初始化文件系统缓冲区，大小: {} 字节", size);
        fileSystemBuffer = memoryManager.allocatePhysicalMemory(size);
        log.info("文件系统缓冲区初始化完成，物理地址: {}", fileSystemBuffer);
    }
    
    /**
     * 为文件分配内存（内部使用方法）
     * @param fileId 文件ID
     * @param size 需要的内存大小（字节）
     * @return 分配的物理内存地址
     * @throws MemoryAllocationException 如果内存分配失败
     */
    public PhysicalAddress allocateFileMemory(long fileId, long size) throws MemoryAllocationException {
        log.debug("为文件 {} 分配内存，大小: {} 字节", fileId, size);
        
        // 检查文件是否已有内存分配
        if (fileMemoryMap.containsKey(fileId)) {
            PhysicalAddress existingAddress = fileMemoryMap.get(fileId);
            log.debug("文件 {} 已存在内存分配: {}", fileId, existingAddress);
            
            // 释放现有内存并重新分配
            memoryManager.freePhysicalMemory(existingAddress);
            log.debug("释放文件 {} 的现有内存: {}", fileId, existingAddress);
        }
        
        // 分配新内存
        PhysicalAddress address = memoryManager.allocatePhysicalMemory(size);
        fileMemoryMap.put(fileId, address);
        
        log.info("为文件 {} 分配内存成功: 地址={}, 大小={} 字节", fileId, address, size);
        return address;
    }
    
    @Override
    public VirtualAddress allocateFileMemory(long size) throws MemoryException {
        log.debug("为文件分配虚拟内存，大小: {} 字节", size);
        
        // 分配物理内存
        PhysicalAddress physicalAddress = memoryManager.allocatePhysicalMemory(size);
        
        // 创建虚拟地址
        VirtualAddress virtualAddress = new VirtualAddress(physicalAddress.getValue());
        
        // 映射关系
        virtualToPhysicalMap.put(virtualAddress, physicalAddress);
        
        log.debug("分配文件内存成功: 虚拟地址={}, 物理地址={}, 大小={} 字节", 
                 virtualAddress, physicalAddress, size);
        
        return virtualAddress;
    }
    
    /**
     * 读取文件数据到内存（内部使用方法）
     * @param fileId 文件ID
     * @param inode 文件的inode
     * @param offset 文件中的偏移量
     * @param buffer 目标缓冲区
     * @param size 要读取的字节数
     * @return 实际读取的字节数
     * @throws FileException 如果读取失败
     */
    public int readFileData(long fileId, INode inode, long offset, byte[] buffer, int size) throws FileException {
        log.debug("从文件 {} 读取数据: 偏移量={}, 大小={} 字节", fileId, offset, size);
        
        try {
            // 确保文件已经映射到内存
            PhysicalAddress fileAddress = getOrAllocateFileMemory(fileId, inode.getSize());
            
            // 读取数据到缓冲区
            int bytesToRead = (int) Math.min(size, inode.getSize() - offset);
            if (bytesToRead <= 0) {
                return 0;  // 没有可读取的数据
            }
            
            // 将物理内存数据复制到缓冲区
            byte[] tempBuffer = memoryManager.readMemory(0, new VirtualAddress(fileAddress.getValue() + offset), bytesToRead);
            System.arraycopy(tempBuffer, 0, buffer, 0, bytesToRead);
            
            log.debug("从文件 {} 读取了 {} 字节数据", fileId, bytesToRead);
            return bytesToRead;
        } catch (Exception e) {
            log.error("读取文件 {} 数据失败: {}", fileId, e.getMessage(), e);
            throw new FileException("读取文件数据失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public byte[] readFileData(VirtualAddress address, int offset, int length) throws MemoryException {
        log.debug("从虚拟地址 {} 读取数据: 偏移量={}, 大小={} 字节", address, offset, length);
        
        PhysicalAddress physicalAddress = virtualToPhysicalMap.get(address);
        if (physicalAddress == null) {
            throw new MemoryException("无效的虚拟地址: " + address);
        }
        
        // 使用MemoryManager提供的读方法
        return memoryManager.readMemory(0, address.add(offset), length);
    }
    
    /**
     * 写入数据到文件（内部使用方法）
     * @param fileId 文件ID
     * @param inode 文件的inode
     * @param offset 文件中的偏移量
     * @param buffer 源数据缓冲区
     * @param size 要写入的字节数
     * @return 实际写入的字节数
     * @throws FileException 如果写入失败
     */
    public int writeFileData(long fileId, INode inode, long offset, byte[] buffer, int size) throws FileException {
        log.debug("写入数据到文件 {}: 偏移量={}, 大小={} 字节", fileId, offset, size);
        
        try {
            // 如果写入位置超过文件当前大小，可能需要扩展文件
            long newSize = Math.max(inode.getSize(), offset + size);
            
            // 确保文件已经映射到内存，并且大小足够
            PhysicalAddress fileAddress = getOrAllocateFileMemory(fileId, newSize);
            
            // 写入数据到物理内存
            memoryManager.writeMemory(0, new VirtualAddress(fileAddress.getValue() + offset), buffer);
            
            // 更新inode大小（如果需要）
            if (newSize > inode.getSize()) {
                inode.setSize(newSize);
                log.debug("更新文件 {} 大小为 {} 字节", fileId, newSize);
            }
            
            log.debug("写入 {} 字节数据到文件 {}", size, fileId);
            return size;
        } catch (Exception e) {
            log.error("写入数据到文件 {} 失败: {}", fileId, e.getMessage(), e);
            throw new FileException("写入文件数据失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void writeFileData(VirtualAddress address, byte[] data, int offset) throws MemoryException {
        log.debug("写入数据到虚拟地址 {}: 偏移量={}, 大小={} 字节", address, offset, data.length);
        
        PhysicalAddress physicalAddress = virtualToPhysicalMap.get(address);
        if (physicalAddress == null) {
            throw new MemoryException("无效的虚拟地址: " + address);
        }
        
        // 使用MemoryManager提供的写方法
        memoryManager.writeMemory(0, address.add(offset), data);
    }
    
    /**
     * 释放文件的内存（内部使用方法）
     * @param fileId 文件ID
     * @return 是否成功释放
     */
    public boolean freeFileMemory(long fileId) {
        log.debug("释放文件 {} 的内存", fileId);
        
        PhysicalAddress address = fileMemoryMap.remove(fileId);
        if (address == null) {
            log.debug("文件 {} 没有分配内存，无需释放", fileId);
            return false;
        }
        
        try {
            memoryManager.freePhysicalMemory(address);
            log.info("文件 {} 的内存已释放: {}", fileId, address);
            return true;
        } catch (MemoryException e) {
            log.warn("释放文件 {} 的内存失败: {}", fileId, address);
            return false;
        }
    }
    
    @Override
    public boolean freeFileMemory(VirtualAddress address) throws MemoryException {
        log.debug("释放虚拟地址 {} 的内存", address);
        
        PhysicalAddress physicalAddress = virtualToPhysicalMap.remove(address);
        if (physicalAddress == null) {
            log.debug("虚拟地址 {} 没有对应的物理内存，无需释放", address);
            return false;
        }
        
        memoryManager.freePhysicalMemory(physicalAddress);
        log.info("虚拟地址 {} 对应的物理内存已释放: {}", address, physicalAddress);
        return true;
    }
    
    /**
     * 获取或分配文件内存
     * @param fileId 文件ID
     * @param size 需要的内存大小
     * @return 物理内存地址
     * @throws MemoryAllocationException 如果内存分配失败
     */
    private PhysicalAddress getOrAllocateFileMemory(long fileId, long size) throws MemoryAllocationException {
        PhysicalAddress address = fileMemoryMap.get(fileId);
        
        if (address == null) {
            // 文件尚未分配内存，分配新内存
            address = allocateFileMemory(fileId, size);
        }
        
        return address;
    }
    
    /**
     * 获取文件内存地址
     * @param fileId 文件ID
     * @return 文件的物理内存地址，如果文件没有映射到内存则返回null
     */
    public PhysicalAddress getFileMemoryAddress(long fileId) {
        return fileMemoryMap.get(fileId);
    }
    
    /**
     * 检查文件是否已映射到内存
     * @param fileId 文件ID
     * @return 是否已映射
     */
    public boolean isFileMapped(long fileId) {
        return fileMemoryMap.containsKey(fileId);
    }
    
    /**
     * 获取文件系统缓冲区
     * @return 文件系统缓冲区的物理地址
     */
    public PhysicalAddress getFileSystemBuffer() {
        return fileSystemBuffer;
    }
    
    /**
     * 获取为文件分配的内存总量
     * @return 已分配内存总量（字节）
     */
    public long getTotalAllocatedFileMemory() {
        return fileMemoryMap.size() * blockSize; // 简化实现，实际应该累加每个文件的实际大小
    }
    
    /**
     * 清理所有文件内存
     * @return 清理的文件数量
     */
    public int clearAllFileMemory() {
        log.info("清理所有文件内存映射");
        int count = 0;
        
        for (Long fileId : new ArrayList<>(fileMemoryMap.keySet())) {
            if (freeFileMemory(fileId)) {
                count++;
            }
        }
        
        log.info("清理了 {} 个文件的内存映射", count);
        return count;
    }
    
    @Override
    public PhysicalAddress allocateFileSystemCache(long size) throws MemoryAllocationException {
        log.debug("为文件系统缓存分配内存，大小: {} 字节", size);
        return memoryManager.allocatePhysicalMemory(size);
    }
    
    @Override
    public void freeFileSystemCache(PhysicalAddress physicalAddress) throws MemoryException {
        log.debug("释放文件系统缓存内存: {}", physicalAddress);
        memoryManager.freePhysicalMemory(physicalAddress);
    }
    
    @Override
    public VirtualAddress mapFileToMemory(int processId, int fileId, long fileOffset, long size, 
                                        boolean isPrivate, boolean isReadOnly) throws MemoryException {
        log.debug("将文件 {} 映射到进程 {} 的内存: 文件偏移={}, 大小={}, 私有={}, 只读={}", 
                 fileId, processId, fileOffset, size, isPrivate, isReadOnly);
        
        // 实现文件到内存的映射
        // 这里需要调用内存管理器进行实际映射
        PhysicalAddress filePhysicalAddress = fileMemoryMap.get((long)fileId);
        if (filePhysicalAddress == null) {
            throw new MemoryException("文件未加载到内存: " + fileId);
        }
        
        // 创建映射
        String protection = isReadOnly ? "r--" : "rw-";
        VirtualAddress virtualAddress;
        
        if (isPrivate) {
            virtualAddress = memoryManager.createPrivateMapping(processId, filePhysicalAddress, size, protection);
        } else {
            virtualAddress = memoryManager.createSharedMapping(processId, filePhysicalAddress, size, protection);
        }
        
        return virtualAddress;
    }
    
    @Override
    public void unmapFileFromMemory(int processId, VirtualAddress virtualAddress, long size) throws MemoryException {
        log.debug("从进程 {} 内存解除文件映射: 地址={}, 大小={}", processId, virtualAddress, size);
        
        // 实现文件映射的解除
        memoryManager.removeMemoryMapping(processId, virtualAddress, size);
    }
    
    @Override
    public void preloadFileToPageCache(int fileId, long fileOffset, long size) throws MemoryException {
        log.debug("预加载文件 {} 到页缓存: 偏移={}, 大小={}", fileId, fileOffset, size);
        
        // 实现文件预加载到页缓存
        // 由于需要具体实现页缓存逻辑，这里先留空
    }
    
    @Override
    public void removeFileFromPageCache(int fileId, long fileOffset, long size) throws MemoryException {
        log.debug("从页缓存移除文件 {}: 偏移={}, 大小={}", fileId, fileOffset, size);
        
        // 实现从页缓存移除文件
        Map<Long, byte[]> fileCache = pageCache.get(fileId);
        if (fileCache != null) {
            long blockStart = fileOffset / blockSize;
            long blockEnd = (fileOffset + size - 1) / blockSize;
            
            for (long i = blockStart; i <= blockEnd; i++) {
                byte[] removed = fileCache.remove(i);
                if (removed != null) {
                    pageCacheSize -= removed.length;
                }
            }
        }
    }
    
    @Override
    public void flushPageCache(int fileId, long fileOffset, long size) throws MemoryException {
        log.debug("刷新文件 {} 的页缓存: 偏移={}, 大小={}", fileId, fileOffset, size);
        
        // 实现页缓存刷新到存储
        // 由于需要具体实现与存储设备的交互，这里先留空
    }
    
    @Override
    public void flushAllPageCache() throws MemoryException {
        log.debug("刷新所有页缓存");
        
        // 实现所有页缓存刷新到存储
        // 由于需要具体实现与存储设备的交互，这里先留空
    }
    
    @Override
    public void createSharedMemory(String name, long size) throws MemoryException {
        log.debug("创建共享内存: 名称={}, 大小={}", name, size);
        
        if (sharedMemoryMap.containsKey(name)) {
            throw new MemoryException("共享内存已存在: " + name);
        }
        
        PhysicalAddress address = memoryManager.allocatePhysicalMemory(size);
        sharedMemoryMap.put(name, address);
    }
    
    @Override
    public void removeSharedMemory(String name) throws MemoryException {
        log.debug("移除共享内存: {}", name);
        
        PhysicalAddress address = sharedMemoryMap.remove(name);
        if (address != null) {
            memoryManager.freePhysicalMemory(address);
        }
    }
    
    @Override
    public VirtualAddress mapSharedMemory(int processId, String sharedMemoryName, long offset, 
                                        long size, boolean isReadOnly) throws MemoryException {
        log.debug("映射共享内存 {} 到进程 {}: 偏移={}, 大小={}, 只读={}", 
                 sharedMemoryName, processId, offset, size, isReadOnly);
        
        PhysicalAddress physicalAddress = sharedMemoryMap.get(sharedMemoryName);
        if (physicalAddress == null) {
            throw new MemoryException("共享内存不存在: " + sharedMemoryName);
        }
        
        // 根据读写权限设置保护标志
        String protection = isReadOnly ? "r--" : "rw-";
        
        // 创建共享映射
        return memoryManager.createSharedMapping(processId, physicalAddress.add(offset), size, protection);
    }
    
    @Override
    public void unmapSharedMemory(int processId, VirtualAddress virtualAddress) throws MemoryException {
        log.debug("从进程 {} 解除共享内存映射: {}", processId, virtualAddress);
        
        // 解除内存映射
        // 假设共享内存映射大小为页面大小的倍数
        long pageSize = 4096; // 一般的页面大小
        memoryManager.removeMemoryMapping(processId, virtualAddress, pageSize);
    }
    
    @Override
    public Map<String, Object> getPageCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", pageCacheSize);
        stats.put("maxSize", pageCacheMaxSize);
        stats.put("hits", pageCacheHits);
        stats.put("misses", pageCacheMisses);
        stats.put("hitRatio", getPageCacheHitRatio());
        return stats;
    }
    
    @Override
    public double getPageCacheHitRatio() {
        long total = pageCacheHits + pageCacheMisses;
        return total > 0 ? (double) pageCacheHits / total : 0;
    }
    
    @Override
    public long getPageCacheSize() {
        return pageCacheSize;
    }
    
    @Override
    public void setPageCacheMaxSize(long maxSize) {
        this.pageCacheMaxSize = maxSize;
    }
    
    @Override
    public List<Integer> getCachedFiles() {
        return new ArrayList<>(pageCache.keySet());
    }
    
    @Override
    public boolean isFileInPageCache(int fileId, long offset, long size) {
        Map<Long, byte[]> fileCache = pageCache.get(fileId);
        if (fileCache == null) {
            return false;
        }
        
        // 简化实现，检查每个块是否在缓存中
        long blockStart = offset / blockSize;
        long blockEnd = (offset + size - 1) / blockSize;
        
        for (long i = blockStart; i <= blockEnd; i++) {
            if (!fileCache.containsKey(i)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public double getFileCacheRatio(int fileId) {
        // 简化实现，返回文件在缓存中的比例
        return 0.0; // 实际实现需要计算
    }
    
    @Override
    public void lockFileInPageCache(int fileId, long offset, long size) throws MemoryException {
        log.debug("锁定文件 {} 在页缓存中: 偏移={}, 大小={}", fileId, offset, size);
        
        // 实现页缓存锁定
        // 这里先留空
    }
    
    @Override
    public void unlockFileInPageCache(int fileId, long offset, long size) throws MemoryException {
        log.debug("解锁文件 {} 在页缓存中: 偏移={}, 大小={}", fileId, offset, size);
        
        // 实现页缓存解锁
        // 这里先留空
    }
} 