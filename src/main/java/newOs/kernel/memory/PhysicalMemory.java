package newOs.kernel.memory;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import newOs.exception.MemoryAllocationException;
import newOs.kernel.memory.allocation.FirstFitAllocator;
import newOs.kernel.memory.allocation.MemoryAllocator;
import newOs.kernel.memory.allocation.MemoryBlock;
import newOs.kernel.memory.allocation.MemoryReclaimer;
import newOs.kernel.memory.model.PhysicalAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 物理内存管理类
 * 负责管理整个物理内存，提供读写操作，以及物理内存的分配和释放
 */
@Component
@Data
@Slf4j
public class PhysicalMemory {
    
    /**
     * 内存大小（字节）
     */
    @Value("${memory.physical.size:1073741824}")
    private long totalSize = 1073741824; // 默认1GB
    
    /**
     * 内存数据
     */
    private byte[] memory;
    
    // 内存分配器
    private final MemoryAllocator memoryAllocator;
    
    // 内存回收器
    private MemoryReclaimer memoryReclaimer;
    
    // 内存访问统计
    private long readCount = 0;
    private long writeCount = 0;
    
    // 进程关联的内存块缓存，提高查找效率
    private final Map<Integer, Map<Integer, MemoryBlock>> processMemoryCache;
    
    /**
     * 构造物理内存对象
     */
    public PhysicalMemory() {
        this.memoryAllocator = new FirstFitAllocator((int)totalSize);
        this.processMemoryCache = new HashMap<>();
    }
    
    /**
     * 设置内存回收器
     * @param memoryReclaimer 内存回收器
     */
    @Autowired
    public void setMemoryReclaimer(MemoryReclaimer memoryReclaimer) {
        this.memoryReclaimer = memoryReclaimer;
    }
    
    /**
     * 初始化物理内存
     */
    @PostConstruct
    public void init() {
        try {
            memory = new byte[(int)totalSize];
            log.info("物理内存初始化完成，大小: {}", formatBytes(totalSize));
        } catch (OutOfMemoryError e) {
            totalSize = 268435456; // 降至256MB
            memory = new byte[(int)totalSize];
            log.warn("物理内存分配失败，降低至{}后重试成功", formatBytes(totalSize));
        }
    }
    
    /**
     * 从物理内存读取数据
     * @param address 物理地址
     * @param size 读取大小（字节）
     * @return 读取的字节数组
     */
    public byte[] read(long address, int size) {
        if (address < 0 || address + size > totalSize) {
            throw new IllegalArgumentException("物理地址越界: " + address + ", size=" + size);
        }
        
        readCount++;
        byte[] data = new byte[size];
        System.arraycopy(memory, (int)address, data, 0, size);
        return data;
    }
    
    /**
     * 向物理内存写入数据
     * @param address 物理地址
     * @param data 要写入的数据
     */
    public void write(long address, byte[] data) {
        if (address < 0 || address + data.length > totalSize) {
            throw new IllegalArgumentException("物理地址越界: " + address + ", size=" + data.length);
        }
        
        writeCount++;
        System.arraycopy(data, 0, memory, (int)address, data.length);
    }
    
    /**
     * 获取物理内存总大小
     * @return 物理内存大小（字节）
     */
    public long getTotalSize() {
        return totalSize;
    }
    
    /**
     * 清空指定范围的内存
     * @param address 起始地址
     * @param size 大小（字节）
     */
    public void clear(long address, int size) {
        if (address < 0 || address + size > totalSize) {
            throw new IllegalArgumentException("物理地址越界: " + address + ", size=" + size);
        }
        
        Arrays.fill(memory, (int)address, (int)(address + size), (byte)0);
    }
    
    /**
     * 格式化字节数为可读字符串
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2fGB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 读取物理内存中的一个字节
     * @param address 物理地址
     * @return 读取的字节值
     */
    public byte readByte(PhysicalAddress address) {
        long addr = address.getValue();
        if (addr < 0 || addr >= totalSize) {
            throw new IndexOutOfBoundsException("无效的物理地址: " + addr);
        }
        
        readCount++;
        return memory[(int) addr];
    }
    
    /**
     * 写入一个字节到物理内存
     * @param address 物理地址
     * @param value 要写入的字节值
     */
    public void writeByte(PhysicalAddress address, byte value) {
        long addr = address.getValue();
        if (addr < 0 || addr >= totalSize) {
            throw new IndexOutOfBoundsException("无效的物理地址: " + addr);
        }
        
        writeCount++;
        memory[(int) addr] = value;
    }
    
    /**
     * 读取物理内存中的一个整数（4字节）
     * @param address 物理地址
     * @return 读取的整数值
     */
    public int readInt(PhysicalAddress address) {
        long addr = address.getValue();
        if (addr < 0 || addr + 3 >= totalSize) {
            throw new IndexOutOfBoundsException("无效的物理地址范围: " + addr + " - " + (addr + 3));
        }
        
        readCount++;
        return (memory[(int) addr] & 0xFF) |
               ((memory[(int) addr + 1] & 0xFF) << 8) |
               ((memory[(int) addr + 2] & 0xFF) << 16) |
               ((memory[(int) addr + 3] & 0xFF) << 24);
    }
    
    /**
     * 写入一个整数（4字节）到物理内存
     * @param address 物理地址
     * @param value 要写入的整数值
     */
    public void writeInt(PhysicalAddress address, int value) {
        long addr = address.getValue();
        if (addr < 0 || addr + 3 >= totalSize) {
            throw new IndexOutOfBoundsException("无效的物理地址范围: " + addr + " - " + (addr + 3));
        }
        
        writeCount++;
        memory[(int) addr] = (byte) (value & 0xFF);
        memory[(int) addr + 1] = (byte) ((value >> 8) & 0xFF);
        memory[(int) addr + 2] = (byte) ((value >> 16) & 0xFF);
        memory[(int) addr + 3] = (byte) ((value >> 24) & 0xFF);
    }
    
    /**
     * 读取物理内存中的一块数据
     * @param address 起始物理地址
     * @param buffer 目标缓冲区
     * @param offset 目标缓冲区的偏移量
     * @param length 要读取的长度
     */
    public void readBlock(PhysicalAddress address, byte[] buffer, int offset, int length) {
        long addr = address.getValue();
        if (addr < 0 || addr + length > totalSize) {
            throw new IndexOutOfBoundsException("无效的物理地址范围: " + addr + " - " + (addr + length - 1));
        }
        
        readCount++;
        System.arraycopy(memory, (int) addr, buffer, offset, length);
    }
    
    /**
     * 写入一块数据到物理内存
     * @param address 起始物理地址
     * @param buffer 源数据缓冲区
     * @param offset 源缓冲区的偏移量
     * @param length 要写入的长度
     */
    public void writeBlock(PhysicalAddress address, byte[] buffer, int offset, int length) {
        long addr = address.getValue();
        if (addr < 0 || addr + length > totalSize) {
            throw new IndexOutOfBoundsException("无效的物理地址范围: " + addr + " - " + (addr + length - 1));
        }
        
        writeCount++;
        System.arraycopy(buffer, offset, memory, (int) addr, length);
    }
    
    /**
     * 分配指定大小的物理内存
     * @param size 需要的内存大小（字节）
     * @param pid 进程ID
     * @return 分配的内存的起始物理地址
     * @throws MemoryAllocationException 如果内存分配失败
     */
    public PhysicalAddress allocate(int size, int pid) throws MemoryAllocationException {
        try {
            PhysicalAddress address = memoryAllocator.allocate(size, pid);
            
            // 清空分配的内存区域
            long addr = address.getValue();
            Arrays.fill(memory, (int)addr, (int)(addr + size), (byte) 0);
            
            log.debug("为进程{}分配物理内存: 地址=0x{}, 大小={} 字节", 
                    pid, Long.toHexString(addr), size);
            
            return address;
        } catch (MemoryAllocationException e) {
            // 如果分配失败，尝试回收内存后再次尝试
            log.warn("内存分配失败，尝试回收内存...");
            int reclaimed = memoryReclaimer.forceMemoryReclaim();
            
            if (reclaimed > 0) {
                log.info("回收了 {} 块内存，重新尝试分配", reclaimed);
                return memoryAllocator.allocate(size, pid);
            } else {
                log.error("内存分配失败，无法回收足够内存");
                throw e;
            }
        }
    }
    
    /**
     * 释放指定地址的内存
     * @param address 要释放的内存的起始物理地址
     * @param pid 进程ID
     * @return 是否成功释放
     */
    public boolean free(PhysicalAddress address, int pid) {
        boolean result = memoryAllocator.free(address, pid);
        
        if (result) {
            log.debug("释放进程{}的物理内存: 地址=0x{}", 
                    pid, Long.toHexString(address.getAddress()));
        }
        
        return result;
    }
    
    /**
     * 释放指定进程的所有内存
     * @param pid 进程ID
     * @return 释放的内存块数量
     */
    public int freeAll(int pid) {
        int count = memoryAllocator.freeAll(pid);
        
        if (count > 0) {
            log.debug("释放进程{}的所有物理内存: {} 块", pid, count);
        }
        
        return count;
    }
    
    /**
     * 获取内存使用情况的字符串表示
     * @return 内存使用情况字符串
     */
    public String getMemoryUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 物理内存使用情况 =====\n");
        sb.append(String.format("总内存: %d 字节\n", totalSize));
        sb.append(String.format("已分配: %d 字节 (%.2f%%)\n", 
                memoryAllocator.getAllocatedMemorySize(),
                memoryAllocator.getUsageRatio() * 100));
        sb.append(String.format("空闲: %d 字节 (%.2f%%)\n", 
                memoryAllocator.getFreeMemorySize(),
                100 - memoryAllocator.getUsageRatio() * 100));
        sb.append(String.format("最大连续空闲块: %d 字节\n", memoryAllocator.getLargestFreeBlockSize()));
        sb.append(String.format("内存碎片率: %.2f%%\n", memoryAllocator.getFragmentationRatio() * 100));
        sb.append(String.format("累计读操作: %d 次\n", readCount));
        sb.append(String.format("累计写操作: %d 次\n", writeCount));
        return sb.toString();
    }
    
    /**
     * 检查指定地址是否在指定进程的分配范围内
     * @param address 物理地址
     * @param pid 进程ID
     * @return 是否在进程的分配范围内
     */
    public boolean isAddressInProcessRange(PhysicalAddress address, int pid) {
        return memoryAllocator.isAddressInProcessRange(address, pid);
    }
    
    /**
     * 获取内存分配情况的详细信息
     * @return 内存分配情况字符串
     */
    public String getMemoryMapString() {
        return memoryAllocator.getMemoryMapString();
    }
    
    /**
     * 获取进程的内存使用情况
     * @param processId 进程ID
     * @return 进程内存使用情况概览
     */
    public String getProcessMemoryInfo(int processId) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("===== 进程 %d 内存使用情况 =====\n", processId));
        
        if (!processMemoryCache.containsKey(processId)) {
            sb.append("该进程未分配内存\n");
            return sb.toString();
        }
        
        Map<Integer, MemoryBlock> blocks = processMemoryCache.get(processId);
        sb.append(String.format("总分配: %d 字节\n", calculateTotalAllocatedSize(blocks)));
        sb.append("分配块:\n");
        
        for (MemoryBlock block : blocks.values()) {
            PhysicalAddress addr = block.getStartAddress();
            long physicalAddr = addr.getValue();
            long size = block.getSize();
            sb.append(String.format("  地址: %s, 大小: %d 字节\n", addr, size));
        }
        
        return sb.toString();
    }
    
    /**
     * 计算总分配大小
     * @param blocks 内存块映射
     * @return 总分配大小
     */
    private long calculateTotalAllocatedSize(Map<Integer, MemoryBlock> blocks) {
        return blocks.values().stream().mapToLong(MemoryBlock::getSize).sum();
    }
    
    /**
     * 释放指定进程的所有内存
     * @param processId 进程ID
     * @return 已释放的内存块数量
     */
    public int releaseProcessMemory(int processId) {
        if (!processMemoryCache.containsKey(processId)) {
            return 0;
        }
        
        Map<Integer, MemoryBlock> blocks = processMemoryCache.get(processId);
        int blockCount = blocks.size();
        
        for (MemoryBlock block : blocks.values()) {
            PhysicalAddress addr = block.getStartAddress();
            long physicalAddr = addr.getValue();
            long size = block.getSize();
            
            log.debug("释放进程 {} 的内存块: 地址={}, 大小={} 字节", processId, addr, size);
            memoryAllocator.free(addr, (int)size);
        }
        
        processMemoryCache.remove(processId);
        log.info("进程 {} 的所有内存已释放, 共 {} 块", processId, blockCount);
        
        return blockCount;
    }
    
    /**
     * 获取已使用的物理内存大小
     * @return 已使用内存大小（字节）
     */
    public long getUsedSize() {
        return memoryAllocator.getAllocatedMemorySize();
    }
    
    /**
     * 获取可用内存大小
     * @return 可用内存大小（字节）
     */
    public long getFreeSize() {
        return totalSize - memoryAllocator.getAllocatedMemorySize();
    }
    
    /**
     * 初始化物理内存
     * @param size 内存大小
     */
    public void initialize(long size) {
        this.totalSize = size;
        this.memory = new byte[(int)size];
        log.info("物理内存初始化完成，大小: {}", formatBytes(size));
    }
    
    /**
     * 复制一个页帧到另一个页帧
     * @param sourceFrameNumber 源页帧号
     * @param targetFrameNumber 目标页帧号
     */
    public void copyFrame(int sourceFrameNumber, int targetFrameNumber) {
        int frameSize = 4096; // 假设一个页帧大小为4KB
        long sourceAddress = (long)sourceFrameNumber * frameSize;
        long targetAddress = (long)targetFrameNumber * frameSize;
        
        byte[] data = read(sourceAddress, frameSize);
        write(targetAddress, data);
        
        log.debug("页帧复制完成: 从{} -> {}", sourceFrameNumber, targetFrameNumber);
    }
} 