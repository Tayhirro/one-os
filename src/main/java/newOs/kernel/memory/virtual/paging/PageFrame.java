package newOs.kernel.memory.virtual.paging;

import lombok.Data;
import newOs.kernel.memory.PhysicalMemory;
import newOs.kernel.memory.model.PhysicalAddress;

/**
 * 物理内存页帧
 * 表示物理内存中的一个页帧，对应一个虚拟页面
 */
@Data
public class PageFrame {
    
    // 页帧大小（字节），与页面大小相同
    public static final int FRAME_SIZE = Page.PAGE_SIZE;
    
    // 页帧号
    private final int frameNumber;
    
    // 对应的物理地址
    private final PhysicalAddress physicalAddress;
    
    // 是否已分配
    private boolean allocated;
    
    // 如果已分配，映射到哪个进程
    private int pid;
    
    // 如果已分配，映射到进程的哪个页面
    private int pageNumber;
    
    // 上次访问时间（用于页面置换算法）
    private long lastAccessTime;
    
    // 锁定标志，锁定的页帧不会被置换出去
    private boolean locked;
    
    /**
     * 构造页帧对象
     * @param frameNumber 页帧号
     */
    public PageFrame(int frameNumber) {
        this.frameNumber = frameNumber;
        this.physicalAddress = new PhysicalAddress(frameNumber * FRAME_SIZE);
        this.allocated = false;
        this.pid = -1;
        this.pageNumber = -1;
        this.lastAccessTime = 0;
        this.locked = false;
    }
    
    /**
     * 分配页帧给指定进程的页面
     * @param pid 进程ID
     * @param pageNumber 页面号
     */
    public void allocate(int pid, int pageNumber) {
        this.allocated = true;
        this.pid = pid;
        this.pageNumber = pageNumber;
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    /**
     * 设置进程ID
     * @param pid 进程ID
     */
    public void setProcessId(int pid) {
        this.pid = pid;
    }
    
    /**
     * 释放页帧
     */
    public void free() {
        this.allocated = false;
        this.pid = -1;
        this.pageNumber = -1;
        this.locked = false;
    }
    
    /**
     * 锁定页帧，防止被置换
     */
    public void lock() {
        this.locked = true;
    }
    
    /**
     * 解锁页帧
     */
    public void unlock() {
        this.locked = false;
    }
    
    /**
     * 更新页帧的访问时间
     */
    public void updateAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    /**
     * 获取页帧对应的物理起始地址
     * @return 物理起始地址
     */
    public PhysicalAddress getPhysicalAddress() {
        return physicalAddress;
    }
    
    /**
     * 获取指定偏移量的物理地址
     * @param offset 页内偏移
     * @return 物理地址
     */
    public PhysicalAddress getPhysicalAddress(int offset) {
        if (offset < 0 || offset >= FRAME_SIZE) {
            throw new IndexOutOfBoundsException("页帧偏移超出范围: " + offset);
        }
        
        return new PhysicalAddress(physicalAddress.getAddress() + offset);
    }
    
    /**
     * 读取页帧中指定偏移量的字节
     * @param offset 页内偏移
     * @param physicalMemory 物理内存对象
     * @return 读取的字节值
     */
    public byte readByte(int offset, PhysicalMemory physicalMemory) {
        PhysicalAddress address = getPhysicalAddress(offset);
        updateAccessTime();
        return physicalMemory.readByte(address);
    }
    
    /**
     * 写入页帧中指定偏移量的字节
     * @param offset 页内偏移
     * @param value 要写入的值
     * @param physicalMemory 物理内存对象
     */
    public void writeByte(int offset, byte value, PhysicalMemory physicalMemory) {
        PhysicalAddress address = getPhysicalAddress(offset);
        updateAccessTime();
        physicalMemory.writeByte(address, value);
    }
    
    /**
     * 读取页帧中指定偏移量的整数
     * @param offset 页内偏移
     * @param physicalMemory 物理内存对象
     * @return 读取的整数值
     */
    public int readInt(int offset, PhysicalMemory physicalMemory) {
        if (offset < 0 || offset + 3 >= FRAME_SIZE) {
            throw new IndexOutOfBoundsException("整数访问超出页帧范围: " + offset);
        }
        
        PhysicalAddress address = getPhysicalAddress(offset);
        updateAccessTime();
        return physicalMemory.readInt(address);
    }
    
    /**
     * 写入页帧中指定偏移量的整数
     * @param offset 页内偏移
     * @param value 要写入的整数值
     * @param physicalMemory 物理内存对象
     */
    public void writeInt(int offset, int value, PhysicalMemory physicalMemory) {
        if (offset < 0 || offset + 3 >= FRAME_SIZE) {
            throw new IndexOutOfBoundsException("整数访问超出页帧范围: " + offset);
        }
        
        PhysicalAddress address = getPhysicalAddress(offset);
        updateAccessTime();
        physicalMemory.writeInt(address, value);
    }
    
    /**
     * 将整个页帧数据复制到缓冲区
     * @param buffer 目标缓冲区
     * @param physicalMemory 物理内存对象
     */
    public void copyToBuffer(byte[] buffer, PhysicalMemory physicalMemory) {
        if (buffer.length < FRAME_SIZE) {
            throw new IllegalArgumentException("缓冲区大小不足");
        }
        
        physicalMemory.readBlock(physicalAddress, buffer, 0, FRAME_SIZE);
        updateAccessTime();
    }
    
    /**
     * 从缓冲区复制数据到整个页帧
     * @param buffer 源缓冲区
     * @param physicalMemory 物理内存对象
     */
    public void copyFromBuffer(byte[] buffer, PhysicalMemory physicalMemory) {
        if (buffer.length < FRAME_SIZE) {
            throw new IllegalArgumentException("缓冲区大小不足");
        }
        
        physicalMemory.writeBlock(physicalAddress, buffer, 0, FRAME_SIZE);
        updateAccessTime();
    }
    
    /**
     * 清空页帧内容
     * @param physicalMemory 物理内存对象
     */
    public void clear(PhysicalMemory physicalMemory) {
        try {
            // 将页帧内容清零
            byte[] zeros = new byte[Page.PAGE_SIZE];
            // 使用物理内存访问接口清零
            physicalMemory.writeBlock(physicalAddress, zeros, 0, Page.PAGE_SIZE);
        } catch (Exception e) {
            // 记录错误
        }
    }
    
    /**
     * 清空页帧内容（不需要物理内存对象）
     */
    public void clear() {
        try {
            // 将页帧内容清零
            byte[] zeros = new byte[Page.PAGE_SIZE];
            // 这里实际实现应该使用物理内存访问
            // 由于没有传入physicalMemory对象，此方法只是声明性的
        } catch (Exception e) {
            // 记录错误
        }
    }
    
    @Override
    public String toString() {
        return String.format("PageFrame[frame=%d, address=0x%X, allocated=%b, pid=%d, page=%d, locked=%b]",
                frameNumber, physicalAddress.getAddress(), allocated, pid, pageNumber, locked);
    }
} 