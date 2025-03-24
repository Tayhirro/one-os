package newOs.kernel.memory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物理地址模型类
 * 表示系统中的物理内存地址
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalAddress {
    
    /**
     * 物理地址值
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
     * 页帧号的位数
     */
    public static final int FRAME_BITS = 52;  // 剩余的位数用于帧号
    
    /**
     * 偏移掩码，用于提取偏移量
     */
    public static final long OFFSET_MASK = (1L << OFFSET_BITS) - 1;
    
    /**
     * 页帧号掩码，用于提取页帧号
     */
    public static final long FRAME_MASK = ~OFFSET_MASK;
    
    /**
     * 从页帧号和偏移量构造物理地址
     * @param frameNumber 页帧号
     * @param offset 页内偏移量
     */
    public PhysicalAddress(long frameNumber, int offset) {
        this.value = (frameNumber << OFFSET_BITS) | (offset & OFFSET_MASK);
    }
    
    /**
     * 获取物理地址值
     * @return 物理地址值
     */
    public long getValue() {
        return value;
    }
    
    /**
     * 获取物理地址值（别名方法，与getValue相同）
     * @return 物理地址值
     */
    public long getAddress() {
        return value;
    }
    
    /**
     * 设置物理地址值
     * @param value 新的物理地址值
     */
    public void setValue(long value) {
        this.value = value;
    }
    
    /**
     * 获取页帧号
     * @return 页帧号
     */
    public long getFrameNumber() {
        return (value & FRAME_MASK) >>> OFFSET_BITS;
    }
    
    /**
     * 获取页内偏移量
     * @return 页内偏移量
     */
    public int getOffset() {
        return (int)(value & OFFSET_MASK);
    }
    
    /**
     * 设置页帧号
     * @param frameNumber 新的页帧号
     */
    public void setFrameNumber(long frameNumber) {
        this.value = (this.value & OFFSET_MASK) | (frameNumber << OFFSET_BITS);
    }
    
    /**
     * 设置页内偏移量
     * @param offset 新的页内偏移量
     */
    public void setOffset(int offset) {
        this.value = (this.value & FRAME_MASK) | (offset & OFFSET_MASK);
    }
    
    /**
     * 检查地址是否在指定范围内
     * @param start 范围起始地址
     * @param size 范围大小（字节）
     * @return 是否在范围内
     */
    public boolean isInRange(PhysicalAddress start, long size) {
        return this.value >= start.value && this.value < (start.value + size);
    }
    
    /**
     * 在当前地址上增加偏移量
     * @param offset 偏移量（字节）
     * @return 新的物理地址实例
     */
    public PhysicalAddress add(long offset) {
        return new PhysicalAddress(this.value + offset);
    }
    
    /**
     * 计算两个地址之间的偏移量
     * @param other 另一个物理地址
     * @return 偏移量（字节）
     */
    public long offsetFrom(PhysicalAddress other) {
        return this.value - other.value;
    }
    
    @Override
    public String toString() {
        return String.format("0x%016X", value);
    }
    
    /**
     * 从十六进制字符串创建物理地址
     * @param hexAddress 十六进制地址字符串（带或不带0x前缀）
     * @return 物理地址实例
     */
    public static PhysicalAddress fromHexString(String hexAddress) {
        String normalizedHex = hexAddress.startsWith("0x") 
            ? hexAddress.substring(2) 
            : hexAddress;
        return new PhysicalAddress(Long.parseLong(normalizedHex, 16));
    }
    
    /**
     * 检查物理地址是否有效
     * @return 如果地址有效返回true，否则返回false
     */
    public boolean isValid() {
        // 物理地址不能为负数
        if (value < 0) {
            return false;
        }
        
        // 如果系统定义了最大物理地址，可以在这里检查
        // 这里简单假设64位系统，物理地址小于2^48是有效的
        long maxPhysicalAddress = 1L << 48;
        return value < maxPhysicalAddress;
    }
} 