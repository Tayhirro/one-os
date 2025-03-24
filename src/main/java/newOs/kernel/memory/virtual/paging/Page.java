package newOs.kernel.memory.virtual.paging;

import lombok.Data;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;

/**
 * 虚拟内存页面
 * 表示进程地址空间中的一个页面
 */
@Data
public class Page {
    
    // 页面大小（字节），默认4KB
    public static final int PAGE_SIZE = 4096;
    
    // 页面编号
    private final int pageNumber;
    
    // 所属进程ID
    private final int pid;
    
    // 在内存中的标志
    private boolean present;
    
    // 被修改标志
    private boolean dirty;
    
    // 被访问标志
    private boolean accessed;
    
    // 访问保护位（0-只读, 1-读写, 2-可执行）
    private int protection;
    
    // 对应的页帧号（如果在内存中）
    private int frameNumber;
    
    // 在交换文件中的位置（如果不在内存中）
    private long swapLocation;
    
    // 上次访问时间（用于页面置换算法）
    private long lastAccessTime;
    
    // 访问计数（用于页面置换算法）
    private int accessCount;
    
    // 页面是否被锁定（锁定页面不会被置换出内存）
    private boolean locked;
    
    // 页面是否是共享的
    private boolean shared;
    
    // 页面是否支持写时复制
    private boolean copyOnWrite;
    
    // 页面类型（代码页、数据页等）
    private PageType pageType = PageType.DATA;
    
    /**
     * 页面类型枚举
     */
    public enum PageType {
        CODE,   // 代码页
        DATA,   // 数据页
        STACK,  // 栈页
        HEAP    // 堆页
    }
    
    /**
     * 构造虚拟页面
     * @param pageNumber 页面编号
     * @param pid 进程ID
     */
    public Page(int pageNumber, int pid) {
        this.pageNumber = pageNumber;
        this.pid = pid;
        this.present = false;
        this.dirty = false;
        this.accessed = false;
        this.protection = 0;
        this.frameNumber = -1;
        this.swapLocation = -1;
        this.lastAccessTime = System.currentTimeMillis();
        this.accessCount = 0;
        this.locked = false;
        this.shared = false;
        this.copyOnWrite = false;
    }
    
    /**
     * 检查页面是否可读
     * @return 是否可读
     */
    public boolean isReadable() {
        return protection >= 0;
    }
    
    /**
     * 检查页面是否可写
     * @return 是否可写
     */
    public boolean isWritable() {
        return protection >= 1;
    }
    
    /**
     * 检查页面是否可执行
     * @return 是否可执行
     */
    public boolean isExecutable() {
        return protection >= 2;
    }
    
    /**
     * 设置页面的保护级别
     * @param canWrite 是否可写
     * @param canExecute 是否可执行
     */
    public void setProtection(boolean canWrite, boolean canExecute) {
        if (canExecute) {
            protection = 2;
        } else if (canWrite) {
            protection = 1;
        } else {
            protection = 0;
        }
    }
    
    /**
     * 设置页面的保护级别
     * @param canRead 是否可读
     * @param canWrite 是否可写
     * @param canExecute 是否可执行
     */
    public void setProtection(boolean canRead, boolean canWrite, boolean canExecute) {
        if (canExecute) {
            protection = 2;
        } else if (canWrite) {
            protection = 1;
        } else if (canRead) {
            protection = 0;
        } else {
            protection = -1; // 完全无访问权限
        }
    }
    
    /**
     * 设置页面锁定状态
     * @param locked 是否锁定页面
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
    
    /**
     * 检查页面是否被锁定
     * @return 是否锁定
     */
    public boolean isLocked() {
        return locked;
    }
    
    /**
     * 检查页面是否共享
     * @return 是否共享
     */
    public boolean isShared() {
        return shared;
    }
    
    /**
     * 设置页面共享状态
     * @param shared 是否共享
     */
    public void setShared(boolean shared) {
        this.shared = shared;
    }
    
    /**
     * 检查页面是否支持写时复制
     * @return 是否支持写时复制
     */
    public boolean isCopyOnWrite() {
        return copyOnWrite;
    }
    
    /**
     * 设置页面写时复制状态
     * @param copyOnWrite 是否支持写时复制
     */
    public void setCopyOnWrite(boolean copyOnWrite) {
        this.copyOnWrite = copyOnWrite;
    }
    
    /**
     * 检查是否为代码页
     * @return 是否为代码页
     */
    public boolean isCodePage() {
        return pageType == PageType.CODE;
    }
    
    /**
     * 检查是否为数据页
     * @return 是否为数据页
     */
    public boolean isDataPage() {
        return pageType == PageType.DATA;
    }
    
    /**
     * 设置页面为代码页
     * @param isCodePage 是否为代码页
     */
    public void setCodePage(boolean isCodePage) {
        if (isCodePage) {
            this.pageType = PageType.CODE;
        } else if (this.pageType == PageType.CODE) {
            this.pageType = PageType.DATA;
        }
    }
    
    /**
     * 设置页面为数据页
     * @param isDataPage 是否为数据页
     */
    public void setDataPage(boolean isDataPage) {
        if (isDataPage) {
            this.pageType = PageType.DATA;
        } else if (this.pageType == PageType.DATA) {
            this.pageType = PageType.CODE;
        }
    }
    
    /**
     * 设置页面可写状态
     * @param writable 是否可写
     */
    public void setWritable(boolean writable) {
        if (writable) {
            if (protection < 1) {
                protection = 1;
            }
        } else {
            if (protection > 0) {
                protection = 0;
            }
        }
    }
    
    /**
     * 设置页面可执行状态
     * @param executable 是否可执行
     */
    public void setExecutable(boolean executable) {
        if (executable) {
            protection = 2;
        } else if (protection > 1) {
            protection = 1;
        }
    }
    
    /**
     * 设置页面类型
     * @param pageType 页面类型
     */
    public void setPageType(PageType pageType) {
        this.pageType = pageType;
    }
    
    /**
     * 获取页面类型
     * @return 页面类型
     */
    public PageType getPageType() {
        return pageType;
    }
    
    /**
     * 重置访问标志
     */
    public void resetAccessed() {
        this.accessed = false;
    }
    
    /**
     * 重置脏标志
     */
    public void resetDirty() {
        this.dirty = false;
    }
    
    /**
     * 检查页面是否有交换区位置
     * @return 是否有交换区位置
     */
    public boolean hasSwapLocation() {
        return swapLocation != -1;
    }
    
    /**
     * 获取进程ID
     * @return 进程ID
     */
    public int getProcessId() {
        return pid;
    }
    
    /**
     * 记录页面访问
     * @param isWrite 是否为写操作
     */
    public void recordAccess(boolean isWrite) {
        this.accessed = true;
        if (isWrite) {
            this.dirty = true;
        }
        this.lastAccessTime = System.currentTimeMillis();
        this.accessCount++;
    }
    
    /**
     * 分配物理页帧给此页面
     * @param frameNumber 页帧号
     */
    public void assignFrame(int frameNumber) {
        this.frameNumber = frameNumber;
        this.present = true;
        this.swapLocation = -1; // 重置交换位置
    }
    
    /**
     * 从内存中移出此页面
     * @param swapLocation 在交换文件中的位置
     */
    public void swapOut(long swapLocation) {
        this.swapLocation = swapLocation;
        this.present = false;
        this.frameNumber = -1;
    }
    
    /**
     * 重置访问和修改标志
     */
    public void resetFlags() {
        this.accessed = false;
        this.dirty = false;
    }
    
    /**
     * 获取此页面在进程地址空间中的起始虚拟地址
     * @return 起始虚拟地址
     */
    public VirtualAddress getStartVirtualAddress() {
        return new VirtualAddress(pageNumber, 0);
    }
    
    /**
     * 获取此页面对应的物理地址（如果在内存中）
     * @param offset 页内偏移
     * @return 物理地址，如果页面不在内存中则返回null
     */
    public PhysicalAddress getPhysicalAddress(int offset) {
        if (!present || offset < 0 || offset >= PAGE_SIZE) {
            return null;
        }
        
        int physicalAddress = frameNumber * PAGE_SIZE + offset;
        return new PhysicalAddress(physicalAddress);
    }
    
    /**
     * 检查地址是否在当前页面范围内
     * @param virtualAddress 虚拟地址
     * @return 是否在范围内
     */
    public boolean containsAddress(VirtualAddress virtualAddress) {
        return virtualAddress.getPageNumber() == this.pageNumber;
    }
    
    @Override
    public String toString() {
        return String.format("Page[pid=%d, page=%d, present=%b, frame=%d, dirty=%b, prot=%d]",
                pid, pageNumber, present, frameNumber, dirty, protection);
    }
} 