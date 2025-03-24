package newOs.kernel.memory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内存段模型类
 * 表示进程地址空间中的一个内存段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemorySegment {
    
    /**
     * 内存段类型枚举
     */
    public enum SegmentType {
        /** 代码段 */
        CODE,
        /** 数据段 */
        DATA,
        /** 堆区 */
        HEAP,
        /** 栈区 */
        STACK,
        /** 共享段 */
        SHARED,
        /** 自定义段 */
        CUSTOM
    }
    
    /**
     * 段类型
     */
    private SegmentType type;
    
    /**
     * 起始虚拟地址
     */
    private VirtualAddress startAddress;
    
    /**
     * 结束虚拟地址
     */
    private VirtualAddress endAddress;
    
    /**
     * 段大小（字节）
     */
    private long size;
    
    /**
     * 段名称
     */
    private String name;
    
    /**
     * 所属进程ID
     */
    private int processId;
    
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
     * 创建一个具有基本信息的内存段
     * @param type 段类型
     * @param startAddress 起始虚拟地址
     * @param size 段大小（字节）
     * @param name 段名称
     * @param processId 进程ID
     */
    public MemorySegment(SegmentType type, VirtualAddress startAddress, long size, 
                         String name, int processId) {
        this.type = type;
        this.startAddress = startAddress;
        this.size = size;
        this.name = name;
        this.processId = processId;
        this.endAddress = new VirtualAddress(startAddress.getValue() + size - 1);
        
        // 根据段类型设置默认权限
        switch(type) {
            case CODE:
                this.readable = true;
                this.writable = false;
                this.executable = true;
                break;
            case STACK:
            case HEAP:
            case DATA:
                this.readable = true;
                this.writable = true;
                this.executable = false;
                break;
            case SHARED:
                this.readable = true;
                this.writable = true;
                this.executable = false;
                break;
            default:
                this.readable = true;
                this.writable = false;
                this.executable = false;
        }
    }
    
    /**
     * 判断给定虚拟地址是否在此段内
     * @param address 待检查的虚拟地址
     * @return 是否在段内
     */
    public boolean contains(VirtualAddress address) {
        long addrValue = address.getValue();
        long startValue = startAddress.getValue();
        long endValue = endAddress.getValue();
        return addrValue >= startValue && addrValue <= endValue;
    }
    
    /**
     * 计算地址相对于段起始的偏移量
     * @param address 虚拟地址
     * @return 偏移量（字节）
     * @throws IllegalArgumentException 如果地址不在段内
     */
    public long offsetOf(VirtualAddress address) {
        if (!contains(address)) {
            throw new IllegalArgumentException("Address is not within this memory segment");
        }
        return address.getValue() - startAddress.getValue();
    }
    
    /**
     * 创建一个地址，该地址位于本段起始位置偏移指定字节数的位置
     * @param offset 偏移量（字节）
     * @return 计算得到的虚拟地址
     * @throws IllegalArgumentException 如果偏移超出段范围
     */
    public VirtualAddress addressAtOffset(long offset) {
        if (offset < 0 || offset >= size) {
            throw new IllegalArgumentException("Offset is out of segment bounds");
        }
        return new VirtualAddress(startAddress.getValue() + offset);
    }
} 