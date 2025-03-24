package newOs.kernel.memory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 虚拟地址模型类
 * 表示进程地址空间中的虚拟内存地址
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VirtualAddress {
    
    /**
     * 虚拟地址值
     */
    private long value;
    
    /**
     * 页大小，默认4KB
     */
    public static final int PAGE_SIZE = 4096;
    
    /**
     * 页内偏移的位数，2^12 = 4096，所以偏移量占12位
     */
    public static final int OFFSET_BITS = 12;
    
    /**
     * 页号的位数，默认占20位，支持1M个页
     */
    public static final int PAGE_BITS = 20;
    
    /**
     * 段号的位数，默认占32位，支持4G个段
     */
    public static final int SEGMENT_BITS = 32;
    
    /**
     * 偏移掩码，用于提取偏移量
     */
    public static final long OFFSET_MASK = (1L << OFFSET_BITS) - 1;
    
    /**
     * 页号掩码，用于提取页号
     */
    public static final long PAGE_MASK = ((1L << PAGE_BITS) - 1) << OFFSET_BITS;
    
    /**
     * 段号掩码，用于提取段号
     */
    public static final long SEGMENT_MASK = ((1L << SEGMENT_BITS) - 1) << (OFFSET_BITS + PAGE_BITS);
    
    /**
     * 构造函数，使用给定的页号和偏移量创建虚拟地址
     * @param pageNumber 页号
     * @param offset 偏移量
     */
    public VirtualAddress(int pageNumber, int offset) {
        this.value = ((long)pageNumber << OFFSET_BITS) |
                     (offset & OFFSET_MASK);
    }
    
    /**
     * 获取虚拟地址值
     * @return 虚拟地址值
     */
    public long getValue() {
        return value;
    }
    
    /**
     * 设置虚拟地址值
     * @param value 新的虚拟地址值
     */
    public void setValue(long value) {
        this.value = value;
    }
    
    /**
     * 获取页号
     * @return 页号
     */
    public int getPageNumber() {
        return (int)((value & PAGE_MASK) >>> OFFSET_BITS);
    }
    
    /**
     * 获取页内偏移量
     * @return 页内偏移量
     */
    public int getOffset() {
        return (int)(value & OFFSET_MASK);
    }
    
    /**
     * 设置页号
     * @param pageNumber 新的页号
     */
    public void setPageNumber(int pageNumber) {
        this.value = (this.value & ~PAGE_MASK) | 
                     ((long)pageNumber << OFFSET_BITS);
    }
    
    /**
     * 设置页内偏移量
     * @param offset 新的页内偏移量
     */
    public void setOffset(int offset) {
        this.value = (this.value & ~OFFSET_MASK) | (offset & OFFSET_MASK);
    }
    
    /**
     * 检查地址是否在指定范围内
     * @param start 范围起始地址
     * @param size 范围大小（字节）
     * @return 是否在范围内
     */
    public boolean isInRange(VirtualAddress start, long size) {
        return this.value >= start.value && this.value < (start.value + size);
    }
    
    /**
     * 在当前地址上增加偏移量
     * @param offset 偏移量（字节）
     * @return 新的虚拟地址实例
     */
    public VirtualAddress add(long offset) {
        return new VirtualAddress(this.value + offset);
    }
    
    /**
     * 计算两个地址之间的偏移量
     * @param other 另一个虚拟地址
     * @return 偏移量（字节）
     */
    public long offsetFrom(VirtualAddress other) {
        return this.value - other.value;
    }
    
    /**
     * 判断地址是否为用户空间地址
     * 假设低48位用于用户空间，高位全为0
     * @return 是否为用户空间地址
     */
    public boolean isUserAddress() {
        // 检查最高16位是否全为0
        return (value >>> 48) == 0;
    }
    
    /**
     * 判断地址是否为内核空间地址
     * 假设高16位全为1的地址为内核空间
     * @return 是否为内核空间地址
     */
    public boolean isKernelAddress() {
        // 检查最高16位是否全为1
        return (value >>> 48) == 0xFFFF;
    }
    
    @Override
    public String toString() {
        return String.format("0x%016X", value);
    }
    
    /**
     * 从十六进制字符串创建虚拟地址
     * @param hexAddress 十六进制地址字符串（带或不带0x前缀）
     * @return 虚拟地址实例
     */
    public static VirtualAddress fromHexString(String hexAddress) {
        String normalizedHex = hexAddress.startsWith("0x") 
            ? hexAddress.substring(2) 
            : hexAddress;
        return new VirtualAddress(Long.parseLong(normalizedHex, 16));
    }
} 