package newOs.kernel.memory.allocation;

import lombok.Data;
import newOs.kernel.memory.model.PhysicalAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 空闲块链表
 * 管理物理内存中的空闲块
 */
@Data
public class FreeBlockList {
    // 空闲内存块列表，按地址排序
    private List<MemoryBlock> freeBlocks;
    
    // 总内存大小
    private final int totalMemorySize;
    
    /**
     * 构造空闲块链表
     * @param totalMemorySize 总内存大小（字节）
     */
    public FreeBlockList(int totalMemorySize) {
        this.totalMemorySize = totalMemorySize;
        this.freeBlocks = new ArrayList<>();
        
        // 初始时整个内存都是空闲的
        MemoryBlock initialBlock = new MemoryBlock(new PhysicalAddress(0), totalMemorySize, false, -1);
        freeBlocks.add(initialBlock);
    }
    
    /**
     * 获取空闲内存块总数
     * @return 空闲块数量
     */
    public int getFreeBlockCount() {
        return freeBlocks.size();
    }
    
    /**
     * 获取总空闲内存大小
     * @return 总空闲内存大小（字节）
     */
    public int getTotalFreeSize() {
        int total = 0;
        for (MemoryBlock block : freeBlocks) {
            total += block.getSize();
        }
        return total;
    }
    
    /**
     * 获取最大连续空闲内存块大小
     * @return 最大连续空闲块大小（字节）
     */
    public int getLargestFreeBlockSize() {
        int largest = 0;
        for (MemoryBlock block : freeBlocks) {
            largest = Math.max(largest, block.getSize());
        }
        return largest;
    }
    
    /**
     * 根据First Fit算法查找适合大小的空闲块
     * @param size 请求的内存大小
     * @return 找到的空闲块，如果没有找到则返回null
     */
    public MemoryBlock findFirstFit(int size) {
        for (MemoryBlock block : freeBlocks) {
            if (block.getSize() >= size) {
                return block;
            }
        }
        return null;
    }
    
    /**
     * 分配内存块
     * @param block 待分配的内存块
     * @param size 分配的大小
     * @param pid 进程ID
     * @return 分配的内存块
     */
    public MemoryBlock allocateBlock(MemoryBlock block, int size, int pid) {
        if (block == null || block.isAllocated() || block.getSize() < size) {
            return null;
        }
        
        // 从空闲列表中移除这个块
        freeBlocks.remove(block);
        
        // 如果块恰好等于请求的大小，直接分配
        if (block.getSize() == size) {
            block.allocate(pid);
            return block;
        }
        
        // 否则需要分割
        MemoryBlock remainingBlock = block.split(size);
        block.allocate(pid);
        
        // 将剩余部分加回空闲列表
        if (remainingBlock != null) {
            addFreeBlock(remainingBlock);
        }
        
        return block;
    }
    
    /**
     * 释放内存块
     * @param block 要释放的内存块
     */
    public void freeBlock(MemoryBlock block) {
        if (block == null) {
            return;
        }
        
        // 标记为未分配
        block.free();
        
        // 添加到空闲列表
        addFreeBlock(block);
    }
    
    /**
     * 添加内存块到空闲列表
     * @param block 要添加的空闲块
     */
    public void addFreeBlock(MemoryBlock block) {
        if (block == null || block.isAllocated()) {
            return;
        }
        
        freeBlocks.add(block);
        
        // 按地址排序
        Collections.sort(freeBlocks);
        
        // 尝试合并相邻的空闲块
        mergeAdjacentBlocks();
    }
    
    /**
     * 合并相邻的空闲块
     */
    private void mergeAdjacentBlocks() {
        boolean merged;
        do {
            merged = false;
            
            for (int i = 0; i < freeBlocks.size() - 1; i++) {
                MemoryBlock current = freeBlocks.get(i);
                MemoryBlock next = freeBlocks.get(i + 1);
                
                if (current.canMergeWith(next)) {
                    // 创建合并后的块
                    MemoryBlock mergedBlock = current.mergeWith(next);
                    
                    // 从列表中移除两个原始块
                    freeBlocks.remove(current);
                    freeBlocks.remove(next);
                    
                    // 添加合并后的块
                    freeBlocks.add(mergedBlock);
                    
                    // 重新排序
                    Collections.sort(freeBlocks);
                    
                    merged = true;
                    break;
                }
            }
        } while (merged);
    }
    
    /**
     * 获取内存块信息的字符串表示
     * @return 内存块信息字符串
     */
    public String getMemoryMapString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Memory Map (").append(freeBlocks.size()).append(" free blocks):\n");
        
        for (MemoryBlock block : freeBlocks) {
            sb.append(String.format("  0x%08X - 0x%08X: %d bytes\n", 
                    block.getStartAddress().getAddress(),
                    block.getEndAddress().getAddress(),
                    block.getSize()));
        }
        
        sb.append(String.format("Total Free: %d bytes / %d bytes (%.2f%%)\n", 
                getTotalFreeSize(), 
                totalMemorySize, 
                (double)getTotalFreeSize() / totalMemorySize * 100));
        
        return sb.toString();
    }
} 