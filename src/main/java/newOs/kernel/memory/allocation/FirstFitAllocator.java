package newOs.kernel.memory.allocation;

import lombok.Data;
import newOs.exception.MemoryAllocationException;
import newOs.kernel.memory.model.PhysicalAddress;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * First Fit内存分配器实现
 * 使用First Fit算法进行内存分配
 */
@Component
@Data
public class FirstFitAllocator implements MemoryAllocator {
    private static final Logger logger = Logger.getLogger(FirstFitAllocator.class.getName());
    
    // 空闲块链表
    private final FreeBlockList freeBlockList;
    
    // 已分配的内存块，键为起始地址
    private final Map<Long, MemoryBlock> allocatedBlocks;
    
    // 按进程ID组织的已分配内存块
    private final Map<Integer, List<MemoryBlock>> processBlocks;
    
    // 总内存大小
    private final int totalMemorySize;
    
    /**
     * 构造First Fit分配器
     * @param totalMemorySize 总内存大小（字节）
     */
    public FirstFitAllocator(int totalMemorySize) {
        this.totalMemorySize = totalMemorySize;
        this.freeBlockList = new FreeBlockList(totalMemorySize);
        this.allocatedBlocks = new HashMap<>();
        this.processBlocks = new HashMap<>();
    }
    
    /**
     * 使用First Fit算法分配内存
     * @param size 请求分配的内存大小（字节）
     * @param pid 进程ID
     * @return 分配的内存块的起始物理地址
     * @throws MemoryAllocationException 如果内存分配失败
     */
    @Override
    public PhysicalAddress allocate(int size, int pid) throws MemoryAllocationException {
        if (size <= 0) {
            throw new MemoryAllocationException("Invalid allocation size: " + size, size, pid, "INVALID_SIZE");
        }
        
        // 将分配大小向上对齐到页面大小（4KB）
        // 这确保每个进程的分配都在独立的页面中，提高内存隔离性
        final int PAGE_SIZE = 4096;
        int alignedSize = (size + PAGE_SIZE - 1) / PAGE_SIZE * PAGE_SIZE;
        
        // 找到第一个足够大的空闲块
        MemoryBlock freeBlock = freeBlockList.findFirstFit(alignedSize);
        if (freeBlock == null) {
            throw new MemoryAllocationException(size, pid, "OUT_OF_MEMORY");
        }
        
        // 分配内存块
        MemoryBlock allocatedBlock = freeBlockList.allocateBlock(freeBlock, alignedSize, pid);
        
        // 记录已分配的块
        allocatedBlocks.put(allocatedBlock.getStartAddress().getAddress(), allocatedBlock);
        
        // 记录进程的内存块
        processBlocks.computeIfAbsent(pid, k -> new ArrayList<>()).add(allocatedBlock);
        
        return allocatedBlock.getStartAddress();
    }
    
    /**
     * 释放指定地址的内存
     * @param address 要释放的内存的物理地址
     * @param pid 请求释放内存的进程ID
     * @return 如果释放成功则返回true，否则返回false
     */
    @Override
    public boolean free(PhysicalAddress address, int pid) {
        logger.info("尝试释放内存: 地址 = " + address + ", 进程 = " + pid);
        
        // 获取已分配的内存块
        MemoryBlock block = allocatedBlocks.get(address.getAddress());
        
        // 如果没有找到对应的内存块，则释放失败
        if (block == null) {
            logger.warning("未找到地址 " + address + " 对应的内存块");
            return false;
        }
        
        // 确认内存块是否属于请求释放的进程
        if (block.getPid() != pid) {
            logger.warning("内存块 " + address + " 属于进程 " + block.getPid() + "，而不是请求释放的进程 " + pid);
            return false;
        }
        
        // 释放内存块
        logger.info("释放内存块: " + block);
        block.free();
        freeBlockList.freeBlock(block);
        
        // 移除已分配块记录
        allocatedBlocks.remove(address.getAddress());
        
        // 移除进程内存块记录
        List<MemoryBlock> blocks = processBlocks.get(pid);
        if (blocks != null) {
            blocks.remove(block);
            if (blocks.isEmpty()) {
                processBlocks.remove(pid);
            }
        }
        
        return true;
    }
    
    /**
     * 释放指定进程的所有内存
     * @param pid 进程ID
     * @return 释放的内存块数量
     */
    @Override
    public int freeAll(int pid) {
        List<MemoryBlock> blocks = processBlocks.get(pid);
        if (blocks == null || blocks.isEmpty()) {
            return 0;
        }
        
        int count = blocks.size();
        
        // 创建一个副本列表，因为在释放过程中会修改原始列表
        List<MemoryBlock> blocksCopy = new ArrayList<>(blocks);
        
        for (MemoryBlock block : blocksCopy) {
            free(block.getStartAddress(), pid);
        }
        
        return count;
    }
    
    /**
     * 获取空闲内存总量
     * @return 空闲内存总量（字节）
     */
    @Override
    public int getFreeMemorySize() {
        return freeBlockList.getTotalFreeSize();
    }
    
    /**
     * 获取已分配内存总量
     * @return 已分配内存总量（字节）
     */
    @Override
    public int getAllocatedMemorySize() {
        return totalMemorySize - getFreeMemorySize();
    }
    
    /**
     * 获取最大连续空闲内存块的大小
     * @return 最大连续空闲内存块大小（字节）
     */
    @Override
    public int getLargestFreeBlockSize() {
        return freeBlockList.getLargestFreeBlockSize();
    }
    
    /**
     * 获取指定进程已分配的内存大小
     * @param pid 进程ID
     * @return 进程已分配的内存大小（字节）
     */
    @Override
    public int getProcessAllocatedSize(int pid) {
        List<MemoryBlock> blocks = processBlocks.get(pid);
        if (blocks == null) {
            return 0;
        }
        
        int total = 0;
        for (MemoryBlock block : blocks) {
            total += block.getSize();
        }
        return total;
    }
    
    /**
     * 获取内存碎片率
     * @return 内存碎片率（0到1之间的值）
     */
    @Override
    public double getFragmentationRatio() {
        int totalFree = getFreeMemorySize();
        if (totalFree == 0) {
            return 0.0;
        }
        
        int largest = getLargestFreeBlockSize();
        return 1.0 - (double) largest / totalFree;
    }
    
    /**
     * 获取内存使用率
     * @return 内存使用率（0到1之间的值）
     */
    @Override
    public double getUsageRatio() {
        return (double) getAllocatedMemorySize() / totalMemorySize;
    }
    
    /**
     * 检查指定地址是否在指定进程的分配范围内
     * @param address 物理地址
     * @param pid 进程ID
     * @return 是否在进程的分配范围内
     */
    @Override
    public boolean isAddressInProcessRange(PhysicalAddress address, int pid) {
        List<MemoryBlock> blocks = processBlocks.get(pid);
        if (blocks == null) {
            return false;
        }
        
        for (MemoryBlock block : blocks) {
            if (block.containsAddress(address)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取内存映射的字符串表示
     * @return 内存映射字符串
     */
    @Override
    public String getMemoryMapString() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== First Fit Memory Allocator =====\n");
        sb.append(freeBlockList.getMemoryMapString());
        
        sb.append("\nAllocated Blocks:\n");
        for (MemoryBlock block : allocatedBlocks.values()) {
            sb.append(String.format("  0x%08X - 0x%08X: %d bytes (PID %d)\n", 
                    block.getStartAddress().getAddress(),
                    block.getEndAddress().getAddress(),
                    block.getSize(),
                    block.getPid()));
        }
        
        sb.append(String.format("\nMemory Usage: %.2f%% (%d/%d bytes)\n", 
                getUsageRatio() * 100, 
                getAllocatedMemorySize(), 
                totalMemorySize));
        
        sb.append(String.format("Fragmentation: %.2f%%\n", getFragmentationRatio() * 100));
        
        return sb.toString();
    }
    
    /**
     * 获取内存分配器名称
     * @return 分配器名称
     */
    @Override
    public String getAllocatorName() {
        return "First Fit Allocator";
    }
    
    /**
     * 查找指定物理地址对应的内存块
     * @param address 物理地址
     * @param pid 进程ID
     * @return 内存块对象，如果不存在则返回null
     */
    @Override
    public MemoryBlock findMemoryBlock(PhysicalAddress address, int pid) {
        if (address == null) {
            return null;
        }
        
        // 将long类型的地址转换为int
        long addr = address.getAddress();
        MemoryBlock block = allocatedBlocks.get(addr);
        
        if (block != null && (pid == -1 || block.getPid() == pid)) {
            return block;
        }
        
        return null;
    }
    
    /**
     * 分配内存块并返回对应的内存块对象
     * @param size 大小（字节）
     * @param pid 进程ID
     * @return 内存块对象
     * @throws MemoryAllocationException 如果分配失败
     */
    @Override
    public MemoryBlock allocateBlock(int size, int pid) throws MemoryAllocationException {
        if (size <= 0) {
            throw new MemoryAllocationException("Invalid allocation size: " + size, size, pid, "INVALID_SIZE");
        }
        
        // 找到第一个足够大的空闲块
        MemoryBlock freeBlock = freeBlockList.findFirstFit(size);
        if (freeBlock == null) {
            throw new MemoryAllocationException(size, pid, "OUT_OF_MEMORY");
        }
        
        // 分配内存块
        MemoryBlock allocatedBlock = freeBlockList.allocateBlock(freeBlock, size, pid);
        
        // 记录已分配的块，注意PhysicalAddress.getAddress()返回long类型，需要转换为int
        long addressKey = allocatedBlock.getStartAddress().getAddress();
        allocatedBlocks.put(addressKey, allocatedBlock);
        
        // 记录进程的内存块
        processBlocks.computeIfAbsent(pid, k -> new ArrayList<>()).add(allocatedBlock);
        
        return allocatedBlock;
    }
} 