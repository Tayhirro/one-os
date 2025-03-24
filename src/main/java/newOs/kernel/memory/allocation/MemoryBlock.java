package newOs.kernel.memory.allocation;

import lombok.Data;
import newOs.kernel.memory.model.PhysicalAddress;

/**
 * 内存块类
 * 表示一块连续的物理内存区域
 */
@Data
public class MemoryBlock implements Comparable<MemoryBlock> {
    // 内存块起始物理地址
    private PhysicalAddress startAddress;
    
    // 内存块大小（字节）
    private int size;
    
    // 是否已分配
    private boolean allocated;
    
    // 如果已分配，记录分配给的进程ID，否则为-1
    private int pid;
    
    /**
     * 构造内存块
     * @param startAddress 起始地址
     * @param size 大小
     * @param allocated 是否已分配
     * @param pid 进程ID（未分配为-1）
     */
    public MemoryBlock(PhysicalAddress startAddress, int size, boolean allocated, int pid) {
        this.startAddress = startAddress;
        this.size = size;
        this.allocated = allocated;
        this.pid = pid;
    }
    
    /**
     * 获取内存块结束地址（不含）
     * @return 结束地址
     */
    public PhysicalAddress getEndAddress() {
        return new PhysicalAddress(startAddress.getAddress() + size);
    }
    
    /**
     * 判断地址是否在本内存块范围内
     * @param address 物理地址
     * @return 是否在范围内
     */
    public boolean containsAddress(PhysicalAddress address) {
        long addr = address.getAddress();
        return addr >= startAddress.getAddress() && addr < startAddress.getAddress() + size;
    }
    
    /**
     * 判断内存块是否可以分配给指定大小的请求
     * @param requiredSize 请求的大小
     * @return 是否可分配
     */
    public boolean canAllocate(int requiredSize) {
        return !allocated && size >= requiredSize;
    }
    
    /**
     * 分配内存块
     * @param pid 进程ID
     */
    public void allocate(int pid) {
        this.allocated = true;
        this.pid = pid;
    }
    
    /**
     * 释放内存块
     */
    public void free() {
        this.allocated = false;
        this.pid = -1;
    }
    
    /**
     * 分割内存块
     * 将当前内存块分割为两块：一块大小为allocSize，另一块为剩余部分
     * @param allocSize 分配的大小
     * @return 剩余的内存块，如果没有剩余则返回null
     */
    public MemoryBlock split(int allocSize) {
        if (allocSize >= size) {
            return null; // 没有剩余空间
        }
        
        // 创建剩余部分的内存块
        PhysicalAddress newBlockAddress = new PhysicalAddress(startAddress.getAddress() + allocSize);
        MemoryBlock remainingBlock = new MemoryBlock(newBlockAddress, size - allocSize, false, -1);
        
        // 调整当前块的大小
        this.size = allocSize;
        
        return remainingBlock;
    }
    
    /**
     * 判断是否可以与另一个内存块合并
     * @param other 另一个内存块
     * @return 是否可合并
     */
    public boolean canMergeWith(MemoryBlock other) {
        // 只有未分配的块才能合并
        if (allocated || other.isAllocated()) {
            return false;
        }
        
        // 检查是否相邻
        return this.getEndAddress().getAddress() == other.getStartAddress().getAddress() || 
               other.getEndAddress().getAddress() == this.getStartAddress().getAddress();
    }
    
    /**
     * 与另一个内存块合并
     * @param other 另一个内存块
     * @return 合并后的内存块
     */
    public MemoryBlock mergeWith(MemoryBlock other) {
        if (!canMergeWith(other)) {
            throw new IllegalArgumentException("Cannot merge non-adjacent or allocated blocks");
        }
        
        PhysicalAddress newStart;
        int newSize;
        
        // 确定新块的起始地址和大小
        if (this.getStartAddress().getAddress() < other.getStartAddress().getAddress()) {
            newStart = this.startAddress;
            newSize = this.size + other.getSize();
        } else {
            newStart = other.getStartAddress();
            newSize = other.getSize() + this.size;
        }
        
        return new MemoryBlock(newStart, newSize, false, -1);
    }
    
    /**
     * 比较内存块，用于排序
     * 按起始地址排序
     */
    @Override
    public int compareTo(MemoryBlock other) {
        return Long.compare(this.startAddress.getAddress(), other.startAddress.getAddress());
    }
    
    @Override
    public String toString() {
        return String.format("MemoryBlock[start=0x%X, size=%d, allocated=%b, pid=%d]", 
                startAddress.getAddress(), size, allocated, pid);
    }
} 