package newOs.kernel.memory.model;

import lombok.Data;

/**
 * 设备缓冲区地址
 * 用于表示设备内部的缓冲区地址
 */
@Data
public class BufferAddress {
    // 设备名称
    private String deviceName;
    // 缓冲区在设备内的起始地址
    private int offset;
    // 缓冲区大小
    private int size;
    
    /**
     * 构造设备缓冲区地址
     * @param deviceName 设备名称
     * @param offset 缓冲区在设备内的起始地址
     * @param size 缓冲区大小
     */
    public BufferAddress(String deviceName, int offset, int size) {
        this.deviceName = deviceName;
        this.offset = offset;
        this.size = size;
    }
    
    /**
     * 判断缓冲区地址是否有效
     * @return 缓冲区地址是否有效
     */
    public boolean isValid() {
        return deviceName != null && !deviceName.isEmpty() && offset >= 0 && size > 0;
    }
    
    /**
     * 判断缓冲区是否包含指定偏移
     * @param bufferOffset 指定的偏移
     * @return 是否包含指定偏移
     */
    public boolean containsOffset(int bufferOffset) {
        return bufferOffset >= offset && bufferOffset < offset + size;
    }
    
    /**
     * 计算相对于缓冲区起始的偏移量
     * @param absoluteOffset 绝对偏移量
     * @return 相对偏移量
     */
    public int getRelativeOffset(int absoluteOffset) {
        if (!containsOffset(absoluteOffset)) {
            throw new IllegalArgumentException("Offset not in buffer range");
        }
        return absoluteOffset - offset;
    }
    
    @Override
    public String toString() {
        return String.format("Buffer[Device=%s, Offset=0x%X, Size=%d]", 
                deviceName, offset, size);
    }
} 