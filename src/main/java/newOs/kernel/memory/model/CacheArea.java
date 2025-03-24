package newOs.kernel.memory.model;

import lombok.Data;

/**
 * 缓存区域类
 * 用于表示文件系统或其他子系统在内存中的缓存区域
 */
@Data
public class CacheArea {
    // 缓存区标识符
    private String cacheId;
    // 缓存区物理地址
    private PhysicalAddress physicalAddress;
    // 缓存区大小(字节)
    private int size;
    // 缓存区是否脏（是否被修改过）
    private boolean dirty;
    // 上次访问时间
    private long lastAccessTime;
    // 所属系统（如"文件系统"、"设备缓存"等）
    private String ownerSystem;
    
    /**
     * 构造缓存区域
     * @param cacheId 缓存区标识符
     * @param physicalAddress 缓存区物理地址
     * @param size 缓存区大小
     * @param ownerSystem 所属系统
     */
    public CacheArea(String cacheId, PhysicalAddress physicalAddress, int size, String ownerSystem) {
        this.cacheId = cacheId;
        this.physicalAddress = physicalAddress;
        this.size = size;
        this.dirty = false;
        this.lastAccessTime = System.currentTimeMillis();
        this.ownerSystem = ownerSystem;
    }
    
    /**
     * 更新访问时间
     */
    public void updateAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    /**
     * 标记为脏
     */
    public void markDirty() {
        this.dirty = true;
    }
    
    /**
     * 清除脏标志
     */
    public void clearDirty() {
        this.dirty = false;
    }
    
    /**
     * 判断缓存区域是否有效
     * @return 是否有效
     */
    public boolean isValid() {
        return cacheId != null && !cacheId.isEmpty() && 
               physicalAddress != null && physicalAddress.isValid() && 
               size > 0;
    }
    
    @Override
    public String toString() {
        return String.format("Cache[ID=%s, Address=%s, Size=%d, Dirty=%b, Owner=%s]", 
                cacheId, physicalAddress, size, dirty, ownerSystem);
    }
} 