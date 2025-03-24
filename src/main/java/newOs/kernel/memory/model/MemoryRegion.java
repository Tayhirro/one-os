package newOs.kernel.memory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内存区域模型类
 * 表示系统中的一块连续内存区域
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryRegion {
    
    /**
     * 内存区域类型枚举
     */
    public enum RegionType {
        /** 用户空间内存 */
        USER_SPACE,
        /** 内核空间内存 */
        KERNEL_SPACE,
        /** 保留内存 */
        RESERVED,
        /** 设备内存 */
        DEVICE_MEMORY,
        /** 共享内存 */
        SHARED_MEMORY,
        /** 文件映射内存 */
        FILE_MAPPED,
        /** 普通分配内存 */
        REGULAR
    }
    
    /**
     * 区域起始物理地址
     */
    private PhysicalAddress startAddress;
    
    /**
     * 区域大小（字节）
     */
    private long size;
    
    /**
     * 区域类型
     */
    private RegionType type;
    
    /**
     * 区域描述
     */
    private String description;
    
    /**
     * 区域所属进程ID，如果为系统区域则为0
     */
    private int processId;
    
    /**
     * 区域创建时间
     */
    private long creationTime;
    
    /**
     * 是否可读
     */
    private boolean readable;
    
    /**
     * 是否可写
     */
    private boolean writable;
    
    /**
     * 是否可执行
     */
    private boolean executable;
    
    /**
     * 是否已锁定（不可交换出）
     */
    private boolean locked;

    /**
     * 创建一个具有基本信息的内存区域
     * @param startAddress 起始物理地址
     * @param size 区域大小（字节）
     * @param description 区域描述
     */
    public MemoryRegion(PhysicalAddress startAddress, long size, String description) {
        this.startAddress = startAddress;
        this.size = size;
        this.description = description;
        this.type = RegionType.REGULAR;
        this.processId = 0; // 默认为系统区域
        this.creationTime = System.currentTimeMillis();
        this.readable = true;
        this.writable = true;
        this.executable = false;
        this.locked = false;
    }

    /**
     * 判断给定物理地址是否在此区域内
     * @param address 待检查的物理地址
     * @return 是否在区域内
     */
    public boolean contains(PhysicalAddress address) {
        long addrValue = address.getValue();
        long startValue = startAddress.getValue();
        return addrValue >= startValue && addrValue < (startValue + size);
    }
    
    /**
     * 计算地址相对于区域起始的偏移量
     * @param address 物理地址
     * @return 偏移量（字节）
     * @throws IllegalArgumentException 如果地址不在区域内
     */
    public long offsetOf(PhysicalAddress address) {
        if (!contains(address)) {
            throw new IllegalArgumentException("Address is not within this memory region");
        }
        return address.getValue() - startAddress.getValue();
    }
    
    /**
     * 创建一个地址，该地址位于本区域起始位置偏移指定字节数的位置
     * @param offset 偏移量（字节）
     * @return 计算得到的物理地址
     * @throws IllegalArgumentException 如果偏移超出区域范围
     */
    public PhysicalAddress addressAtOffset(long offset) {
        if (offset < 0 || offset >= size) {
            throw new IllegalArgumentException("Offset is out of region bounds");
        }
        return new PhysicalAddress(startAddress.getValue() + offset);
    }
} 