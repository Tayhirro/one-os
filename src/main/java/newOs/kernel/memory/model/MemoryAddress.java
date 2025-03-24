package newOs.kernel.memory.model;

import lombok.Data;

/**
 * 内存地址的抽象基类
 * 作为虚拟地址和物理地址的父类
 */
@Data
public abstract class MemoryAddress {
    // 地址值
    protected int address;
    
    public MemoryAddress(int address) {
        this.address = address;
    }
    
    /**
     * 获取地址值
     * @return 地址值
     */
    public int getAddress() {
        return address;
    }
    
    /**
     * 设置地址值
     * @param address 新的地址值
     */
    public void setAddress(int address) {
        this.address = address;
    }
    
    /**
     * 判断地址是否有效
     * @return 地址是否有效
     */
    public abstract boolean isValid();
    
    @Override
    public String toString() {
        return "0x" + Integer.toHexString(address);
    }
} 