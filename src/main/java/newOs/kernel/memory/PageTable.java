package newOs.kernel.memory;

import newOs.exception.AddressTranslationException;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 页表类，用于维护虚拟地址到物理地址的映射关系
 */
public class PageTable {
    private static final Logger LOGGER = LogManager.getLogger(PageTable.class);
    
    private Map<Integer, PageTableEntry> pageTable;
    private final int processId;  // 进程ID
    
    /**
     * 构造一个系统页表
     */
    public PageTable() {
        this.pageTable = new HashMap<>();
        this.processId = -1;  // 系统页表进程ID为-1
    }
    
    /**
     * 构造一个特定进程的页表
     * @param processId 进程ID
     */
    public PageTable(int processId) {
        this.pageTable = new HashMap<>();
        this.processId = processId;
    }
    
    /**
     * 获取页表条目
     * @param pageNumber 页号
     * @return 页表条目，如果不存在则返回null
     */
    public PageTableEntry getEntry(int pageNumber) {
        return pageTable.get(pageNumber);
    }
    
    /**
     * 添加页表条目
     * @param pageNumber 页号
     * @param frameNumber 页帧号
     * @param readable 是否可读
     * @param writable 是否可写
     * @param executable 是否可执行
     */
    public void addEntry(int pageNumber, int frameNumber, 
                        boolean readable, boolean writable, boolean executable) {
        PageTableEntry entry = new PageTableEntry(frameNumber, readable, writable, executable);
        pageTable.put(pageNumber, entry);
        LOGGER.debug("添加页表条目 - 进程: {}, 页: {}, 帧: {}, 权限: [r={}, w={}, x={}]", 
                   processId, pageNumber, frameNumber, readable, writable, executable);
    }
    
    /**
     * 移除页表条目
     * @param pageNumber 页号
     * @return 被移除的页表条目，如果不存在则返回null
     */
    public PageTableEntry removeEntry(int pageNumber) {
        PageTableEntry removed = pageTable.remove(pageNumber);
        if (removed != null) {
            LOGGER.debug("移除页表条目 - 进程: {}, 页: {}", processId, pageNumber);
        }
        return removed;
    }
    
    /**
     * 翻译虚拟地址到物理地址
     * @param virtualAddress 虚拟地址
     * @return 物理地址
     * @throws AddressTranslationException 如果翻译失败
     */
    public PhysicalAddress translate(VirtualAddress virtualAddress) throws AddressTranslationException {
        int pageNumber = virtualAddress.getPageNumber();
        int offset = virtualAddress.getOffset();
        
        PageTableEntry entry = getEntry(pageNumber);
        if (entry == null) {
            throw new AddressTranslationException("页表条目不存在", processId, virtualAddress);
        }
        
        return new PhysicalAddress((long)entry.getFrameNumber() << 12 | (offset & 0xFFF));
    }
    
    /**
     * 检查地址是否有读权限
     * @param virtualAddress 虚拟地址
     * @return 是否有读权限
     * @throws AddressTranslationException 如果地址无效
     */
    public boolean hasReadPermission(VirtualAddress virtualAddress) throws AddressTranslationException {
        PageTableEntry entry = getEntry(virtualAddress.getPageNumber());
        if (entry == null) {
            throw new AddressTranslationException("页表条目不存在", processId, virtualAddress);
        }
        return entry.isReadable();
    }
    
    /**
     * 检查地址是否有写权限
     * @param virtualAddress 虚拟地址
     * @return 是否有写权限
     * @throws AddressTranslationException 如果地址无效
     */
    public boolean hasWritePermission(VirtualAddress virtualAddress) throws AddressTranslationException {
        PageTableEntry entry = getEntry(virtualAddress.getPageNumber());
        if (entry == null) {
            throw new AddressTranslationException("页表条目不存在", processId, virtualAddress);
        }
        return entry.isWritable();
    }
    
    /**
     * 检查地址是否有执行权限
     * @param virtualAddress 虚拟地址
     * @return 是否有执行权限
     * @throws AddressTranslationException 如果地址无效
     */
    public boolean hasExecutePermission(VirtualAddress virtualAddress) throws AddressTranslationException {
        PageTableEntry entry = getEntry(virtualAddress.getPageNumber());
        if (entry == null) {
            throw new AddressTranslationException("页表条目不存在", processId, virtualAddress);
        }
        return entry.isExecutable();
    }
    
    /**
     * 获取进程ID
     * @return 进程ID
     */
    public int getProcessId() {
        return processId;
    }
    
    /**
     * 清空页表
     */
    public void clear() {
        pageTable.clear();
        LOGGER.debug("清空页表 - 进程: {}", processId);
    }
    
    /**
     * 获取页表大小（条目数）
     * @return 页表大小
     */
    public int size() {
        return pageTable.size();
    }
    
    /**
     * 页表条目内部类
     */
    public static class PageTableEntry {
        private int frameNumber;
        private boolean readable;
        private boolean writable;
        private boolean executable;
        
        public PageTableEntry(int frameNumber, boolean readable, boolean writable, boolean executable) {
            this.frameNumber = frameNumber;
            this.readable = readable;
            this.writable = writable;
            this.executable = executable;
        }
        
        public int getFrameNumber() {
            return frameNumber;
        }
        
        public boolean isReadable() {
            return readable;
        }
        
        public boolean isWritable() {
            return writable;
        }
        
        public boolean isExecutable() {
            return executable;
        }
        
        public void setFrameNumber(int frameNumber) {
            this.frameNumber = frameNumber;
        }
        
        public void setReadable(boolean readable) {
            this.readable = readable;
        }
        
        public void setWritable(boolean writable) {
            this.writable = writable;
        }
        
        public void setExecutable(boolean executable) {
            this.executable = executable;
        }
    }
} 