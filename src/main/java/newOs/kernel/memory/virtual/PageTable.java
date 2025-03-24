package newOs.kernel.memory.virtual;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import newOs.exception.AddressTranslationException;
import newOs.exception.PageFaultException;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.virtual.paging.Page;
import org.springframework.stereotype.Component;
import newOs.kernel.memory.allocation.MemoryBlock;
import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.PhysicalMemory;
import newOs.kernel.memory.virtual.paging.PageFrame;
import newOs.kernel.memory.virtual.PageFrameTable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 页表类
 * 维护进程的虚拟页面到物理页帧的映射关系
 */
@Component
@Data
@Slf4j
public class PageTable {
    
    // 按进程ID组织的页表
    private final Map<Integer, Map<Integer, Page>> processPageTables;
    
    // 页表项总数
    private int pageTableEntryCount;
    
    /**
     * 进程ID
     */
    private final int processId;
    
    /**
     * 页面映射：虚拟地址 -> 内存块
     */
    private final Map<Long, MemoryBlock> pageMapping = new ConcurrentHashMap<>();
    
    /**
     * 下一个可用的虚拟地址
     */
    private long nextAvailableAddress = 0x10000000; // 从256MB开始
    
    private final PageFrameTable pageFrameTable;
    private final PhysicalMemory physicalMemory;
    private final MemoryManager memoryManager;
    
    /**
     * 构造页表
     */
    public PageTable() {
        this.processPageTables = new ConcurrentHashMap<>();
        this.pageTableEntryCount = 0;
        this.processId = -1; // 使用-1表示系统页表
        this.pageFrameTable = null;
        this.physicalMemory = null;
        this.memoryManager = null;
    }
    
    /**
     * 构造函数
     * @param processId 进程ID
     */
    public PageTable(int processId) {
        this.processPageTables = new ConcurrentHashMap<>();
        this.pageTableEntryCount = 0;
        this.processId = processId;
        log.debug("创建进程{}的页表", processId);
        this.pageFrameTable = null;
        this.physicalMemory = null;
        this.memoryManager = null;
    }
    
    /**
     * 构造函数
     * @param processId 进程ID
     * @param memoryManager 内存管理器
     * @param pageFrameTable 页帧表
     * @param physicalMemory 物理内存
     */
    public PageTable(int processId, MemoryManager memoryManager, PageFrameTable pageFrameTable, PhysicalMemory physicalMemory) {
        this.processPageTables = new ConcurrentHashMap<>();
        this.pageTableEntryCount = 0;
        this.processId = processId;
        this.memoryManager = memoryManager;
        this.pageFrameTable = pageFrameTable;
        this.physicalMemory = physicalMemory;
        log.debug("创建进程{}的页表", processId);
    }
    
    /**
     * 创建进程的页表
     * @param pid 进程ID
     */
    public void createProcessPageTable(int pid) {
        if (!processPageTables.containsKey(pid)) {
            processPageTables.put(pid, new ConcurrentHashMap<>());
            log.debug("创建进程{}的页表", pid);
        }
    }
    
    /**
     * 删除进程的页表
     * @param pid 进程ID
     * @return 删除的页表项数量
     */
    public int removeProcessPageTable(int pid) {
        Map<Integer, Page> pageMap = processPageTables.remove(pid);
        if (pageMap == null) {
            return 0;
        }
        
        int count = pageMap.size();
        pageTableEntryCount -= count;
        log.debug("删除进程{}的页表，共{}项", pid, count);
        return count;
    }
    
    /**
     * 添加页表项
     * @param page 页面对象
     */
    public void addPageEntry(Page page) {
        int pid = page.getPid();
        int pageNumber = page.getPageNumber();
        
        // 确保进程页表存在
        createProcessPageTable(pid);
        
        // 添加页表项
        Map<Integer, Page> pageMap = processPageTables.get(pid);
        if (!pageMap.containsKey(pageNumber)) {
            pageMap.put(pageNumber, page);
            pageTableEntryCount++;
        } else {
            pageMap.put(pageNumber, page); // 更新已有页表项
        }
    }
    
    /**
     * 删除页表项
     * @param pid 进程ID
     * @param pageNumber 页号
     * @return 是否成功删除
     */
    public boolean removePageEntry(int pid, int pageNumber) {
        Map<Integer, Page> pageMap = processPageTables.get(pid);
        if (pageMap == null) {
            return false;
        }
        
        Page removedPage = pageMap.remove(pageNumber);
        if (removedPage != null) {
            pageTableEntryCount--;
            
            // 如果进程页表为空，删除进程表项
            if (pageMap.isEmpty()) {
                processPageTables.remove(pid);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取页表项
     * @param pid 进程ID
     * @param pageNumber 页号
     * @return 页面对象，不存在则返回null
     */
    public Page getPage(int pid, int pageNumber) {
        Map<Integer, Page> pageMap = processPageTables.get(pid);
        if (pageMap == null) {
            return null;
        }
        
        return pageMap.get(pageNumber);
    }
    
    /**
     * 获取虚拟地址对应的页表项
     * @param virtualAddress 虚拟地址
     * @param pid 进程ID
     * @return 页面对象，不存在则返回null
     */
    public Page getPage(VirtualAddress virtualAddress, int pid) {
        return getPage(pid, virtualAddress.getPageNumber());
    }
    
    /**
     * 更新页表项的帧号
     * @param pid 进程ID
     * @param pageNumber 页号
     * @param frameNumber 帧号
     * @return 是否成功更新
     */
    public boolean updateFrameNumber(int pid, int pageNumber, int frameNumber) {
        Page page = getPage(pid, pageNumber);
        if (page == null) {
            return false;
        }
        
        page.setFrameNumber(frameNumber);
        page.setPresent(true);
        return true;
    }
    
    /**
     * 标记页面不在内存中
     * @param pid 进程ID
     * @param pageNumber 页号
     * @param swapLocation 在交换空间的位置
     * @return 是否成功标记
     */
    public boolean markPageNotPresent(int pid, int pageNumber, long swapLocation) {
        Page page = getPage(pid, pageNumber);
        if (page == null) {
            return false;
        }
        
        page.setPresent(false);
        page.setSwapLocation(swapLocation);
        return true;
    }
    
    /**
     * 地址转换：虚拟地址转物理地址
     * @param virtualAddress 虚拟地址
     * @param pid 进程ID
     * @param isWrite 是否为写操作
     * @return 对应的物理地址
     * @throws PageFaultException 如果页面不在内存中
     * @throws AddressTranslationException 如果地址转换失败
     */
    public PhysicalAddress translateAddress(VirtualAddress virtualAddress, int pid, boolean isWrite) 
            throws PageFaultException, AddressTranslationException {
        
        int pageNumber = virtualAddress.getPageNumber();
        int offset = virtualAddress.getOffset();
        
        // 获取页表项
        Page page = getPage(pid, pageNumber);
        if (page == null) {
            throw new AddressTranslationException("页表项不存在", pid, virtualAddress);
        }
        
        // 检查页面是否在内存中
        if (!page.isPresent()) {
            throw new PageFaultException(virtualAddress, isWrite ? "WRITE" : "READ", pid, "页面不在内存中");
        }
        
        // 检查访问权限
        if (isWrite && !page.isWritable()) {
            throw new AddressTranslationException("页面不可写", pid, virtualAddress);
        }
        
        // 记录访问
        page.recordAccess(isWrite);
        
        // 计算物理地址
        int frameNumber = page.getFrameNumber();
        int physicalAddress = frameNumber * Page.PAGE_SIZE + offset;
        
        return new PhysicalAddress(physicalAddress);
    }
    
    /**
     * 获取进程的所有页面
     * @param pid 进程ID
     * @return 页面映射表
     */
    public Map<Integer, Page> getProcessPages(int pid) {
        return processPageTables.get(pid);
    }
    
    /**
     * 获取进程的页表项数量
     * @param pid 进程ID
     * @return 页表项数量
     */
    public int getProcessPageCount(int pid) {
        Map<Integer, Page> pageMap = processPageTables.get(pid);
        if (pageMap == null) {
            return 0;
        }
        
        return pageMap.size();
    }
    
    /**
     * 获取进程已分配页帧的数量
     * @param pid 进程ID
     * @return 已分配页帧数量
     */
    public int getProcessAllocatedFrameCount(int pid) {
        Map<Integer, Page> pageMap = processPageTables.get(pid);
        if (pageMap == null) {
            return 0;
        }
        
        int count = 0;
        for (Page page : pageMap.values()) {
            if (page.isPresent()) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 重置所有页面的访问位
     */
    public void resetAccessFlags() {
        for (Map<Integer, Page> pageMap : processPageTables.values()) {
            for (Page page : pageMap.values()) {
                page.setAccessed(false);
            }
        }
    }
    
    /**
     * 获取所有在内存中的页面数量
     * @return 在内存中的页面数量
     */
    public int getResidentPageCount() {
        int count = 0;
        for (Map<Integer, Page> pageMap : processPageTables.values()) {
            for (Page page : pageMap.values()) {
                if (page.isPresent()) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * 将虚拟地址转换为物理地址
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @return 物理地址
     * @throws AddressTranslationException 地址转换异常
     * @throws PageFaultException 页错误异常
     */
    public PhysicalAddress translate(int pid, VirtualAddress virtualAddress) 
            throws AddressTranslationException, PageFaultException {
        return translateAddress(virtualAddress, pid, false);
    }
    
    /**
     * 检查对虚拟地址的读权限
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否有读权限
     */
    public boolean hasReadPermission(int pid, VirtualAddress virtualAddress) {
        Page page = getPage(pid, virtualAddress.getPageNumber());
        return page != null && page.isReadable();
    }
    
    /**
     * 检查对虚拟地址的写权限
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否有写权限
     */
    public boolean hasWritePermission(int pid, VirtualAddress virtualAddress) {
        Page page = getPage(pid, virtualAddress.getPageNumber());
        return page != null && page.isWritable();
    }
    
    /**
     * 检查对虚拟地址的执行权限
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否有执行权限
     */
    public boolean hasExecutePermission(int pid, VirtualAddress virtualAddress) {
        Page page = getPage(pid, virtualAddress.getPageNumber());
        return page != null && page.isExecutable();
    }
    
    /**
     * 获取页表信息的字符串表示
     * @return 页表信息字符串
     */
    public String getPageTableInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 页表信息 =====\n");
        sb.append(String.format("总进程数: %d\n", processPageTables.size()));
        sb.append(String.format("总页表项数: %d\n", pageTableEntryCount));
        sb.append(String.format("在内存中的页面数: %d\n", getResidentPageCount()));
        
        for (Map.Entry<Integer, Map<Integer, Page>> processEntry : processPageTables.entrySet()) {
            int pid = processEntry.getKey();
            Map<Integer, Page> pageMap = processEntry.getValue();
            
            int processPageCount = 0;
            int processResidentPageCount = 0;
            
            for (Page page : pageMap.values()) {
                processPageCount++;
                if (page.isPresent()) {
                    processResidentPageCount++;
                }
            }
            
            sb.append(String.format("\n进程 %d:\n", pid));
            sb.append(String.format("  页表项数: %d\n", processPageCount));
            sb.append(String.format("  在内存中的页面数: %d\n", processResidentPageCount));
            
            // 仅显示部分页表信息，避免输出过多
            int displayLimit = 5;
            int displayed = 0;
            
            sb.append("  页表项示例:\n");
            for (Map.Entry<Integer, Page> pageEntry : pageMap.entrySet()) {
                if (displayed >= displayLimit) {
                    break;
                }
                
                int pageNumber = pageEntry.getKey();
                Page page = pageEntry.getValue();
                
                sb.append(String.format("    [页=%d]: %s\n", 
                        pageNumber, page.isPresent() ? 
                                String.format("在内存, 帧=%d", page.getFrameNumber()) : 
                                String.format("在交换空间, 位置=%d", page.getSwapLocation())));
                
                displayed++;
            }
            
            if (displayed >= displayLimit) {
                sb.append("    ...(更多页表项)\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 添加内存块映射
     * @param virtualAddress 虚拟地址
     * @param block 内存块
     */
    public void addMapping(VirtualAddress virtualAddress, MemoryBlock block) {
        pageMapping.put(virtualAddress.getValue(), block);
        log.debug("进程{}添加映射: 虚拟地址={}, 物理地址={}, 大小={}",
                processId, virtualAddress.getValue(), block.getStartAddress(), block.getSize());
    }
    
    /**
     * 移除内存块映射
     * @param virtualAddress 虚拟地址
     * @return 被移除的内存块，如果不存在则返回null
     */
    public MemoryBlock removeMapping(VirtualAddress virtualAddress) {
        MemoryBlock block = pageMapping.remove(virtualAddress.getValue());
        if (block != null) {
            log.debug("进程{}移除映射: 虚拟地址={}, 物理地址={}, 大小={}",
                    processId, virtualAddress.getValue(), block.getStartAddress(), block.getSize());
        }
        return block;
    }
    
    /**
     * 获取虚拟地址对应的内存块
     * @param virtualAddress 虚拟地址
     * @return 内存块，如果不存在则返回null
     */
    public MemoryBlock getMemoryBlock(VirtualAddress virtualAddress) {
        // 尝试精确匹配
        MemoryBlock block = pageMapping.get(virtualAddress.getValue());
        if (block != null) {
            return block;
        }
        
        // 如果精确匹配失败，尝试查找包含此地址的页
        return pageMapping.get(virtualAddress.getValue() & ~(Page.PAGE_SIZE - 1));
    }
    
    /**
     * 获取所有内存块
     * @return 所有内存块的集合
     */
    public Collection<MemoryBlock> getAllBlocks() {
        return pageMapping.values();
    }
    
    /**
     * 获取页表大小（映射数量）
     * @return 映射数量
     */
    public int getSize() {
        return pageMapping.size();
    }
    
    /**
     * 清空页表（移除所有映射）
     */
    public void clear() {
        pageMapping.clear();
        log.debug("清空进程{}的页表", processId);
    }
    
    /**
     * 获取下一个可用的虚拟地址
     * @return 虚拟地址
     */
    public long getNextAvailableAddress() {
        // 只返回当前值，不自动递增
        return nextAvailableAddress;
    }
    
    /**
     * 更新下一个可用的虚拟地址
     * @param size 已分配的大小（字节）
     */
    public void updateNextAvailableAddress(int size) {
        // 计算需要的页数
        int pageSize = Page.PAGE_SIZE;
        int pageCount = (size + pageSize - 1) / pageSize; // 向上取整
        
        // 增加地址，确保页对齐
        nextAvailableAddress = (nextAvailableAddress + pageCount * pageSize);
        
        // 确保页面对齐
        if ((nextAvailableAddress % pageSize) != 0) {
            nextAvailableAddress = (nextAvailableAddress + pageSize) & ~(pageSize - 1);
        }
        
        log.debug("更新下一个可用虚拟地址：0x{}", Long.toHexString(nextAvailableAddress));
    }
    
    /**
     * 获取虚拟地址映射的大小
     * @param virtualAddress 虚拟地址
     * @return 映射大小，如果不存在则返回0
     */
    public long getMappingSize(VirtualAddress virtualAddress) {
        // 从内存块映射中获取大小
        // 如果没有明确的大小信息，尝试从MemoryBlock获取
        MemoryBlock block = getMemoryBlock(virtualAddress);
        if (block != null && block.getSize() > 0) {
            return block.getSize();
        }
        
        // 如果无法获取到内存块大小信息，则查询页表映射中该地址的页面数量
        // 假设连续页面属于同一个分配块
        long baseAddress = virtualAddress.getValue() & ~(Page.PAGE_SIZE - 1);
        int count = 0;
        
        // 向前查找页面
        for (long addr = baseAddress; pageMapping.containsKey(addr); addr += Page.PAGE_SIZE) {
            count++;
        }
        
        // 如果至少有一个页面映射，返回页面大小
        if (count > 0) {
            return count * Page.PAGE_SIZE;
        }
        
        // 默认返回0
        return 0;
    }
    
    /**
     * 创建页面到物理帧的映射
     * @param virtualAddr 虚拟地址
     * @param physicalAddr 物理地址
     * @param readable 是否可读
     * @param writable 是否可写
     * @param executable 是否可执行
     */
    public void mapPage(long virtualAddr, long physicalAddr, boolean readable,
                        boolean writable, boolean executable) {
        int pageNumber = (int)((virtualAddr >>> VirtualAddress.OFFSET_BITS) 
                & ((1 << VirtualAddress.PAGE_BITS) - 1));
        int frameNumber = (int)(physicalAddr >>> PhysicalAddress.OFFSET_BITS);
        
        Page page = new Page(pageNumber, processId);
        page.setPresent(true);
        page.setProtection(writable, executable);
        page.setFrameNumber(frameNumber);
        
        addPageEntry(page);
    }
    
    /**
     * 分配虚拟地址空间
     * @param size 需要分配的大小（字节）
     * @return 分配的起始虚拟地址
     */
    public long allocateVirtualAddress(int size) {
        long addr = getNextAvailableAddress();
        // 确保地址对齐并且分配足够的空间
        nextAvailableAddress = (addr + size + Page.PAGE_SIZE - 1) & ~(Page.PAGE_SIZE - 1);
        return addr;
    }
    
    /**
     * 添加copyOnWrite方法，用于处理写时复制机制
     * 当进程尝试写入只读的共享页面时，为进程创建该页面的一个副本
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否成功处理写时复制
     */
    public boolean copyOnWrite(int processId, VirtualAddress virtualAddress) {
        try {
            // 获取原始页面
            Page page = getPage(virtualAddress, processId);
            if (page == null || !page.isShared() || !page.isCopyOnWrite() || !page.isReadable()) {
                log.error("无法执行写时复制: 页面不存在或不满足条件, processId={}, address={}", 
                        processId, virtualAddress.getValue());
                return false;
            }
            
            // 直接使用类的pageFrameTable字段
            if (pageFrameTable == null) {
                log.error("页帧表不可用");
                return false;
            }
            
            // 分配新的页帧
            int pageNumber = virtualAddress.getPageNumber();
            PageFrame newFrame = pageFrameTable.allocateFrame(processId, pageNumber);
            if (newFrame == null) {
                log.error("无法执行写时复制: 无法分配新页帧, processId={}, address={}", 
                        processId, virtualAddress.getValue());
                return false;
            }
            
            // 获取原页面的物理页帧
            int oldFrameNumber = page.getFrameNumber();
            PageFrame oldFrame = pageFrameTable.getFrame(oldFrameNumber);
            
            // 复制页帧内容
            byte[] content = new byte[Page.PAGE_SIZE];
            oldFrame.copyToBuffer(content, physicalMemory);
            newFrame.copyFromBuffer(content, physicalMemory);
            
            // 更新页面信息
            page.assignFrame(newFrame.getFrameNumber());
            page.setShared(false);
            page.setCopyOnWrite(false);
            page.setProtection(true, true, page.isExecutable()); // 设置为可写
            page.setDirty(true);
            
            log.info("成功执行写时复制: processId={}, address={}, 新页帧号={}", 
                    processId, virtualAddress.getValue(), newFrame.getFrameNumber());
            
            return true;
        } catch (Exception e) {
            log.error("执行写时复制时发生异常: processId={}, address={}", 
                    processId, virtualAddress.getValue(), e);
            return false;
        }
    }
    
    /**
     * 获取内存管理器
     * @return 内存管理器
     */
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }
    
    /**
     * 获取物理内存
     * @return 物理内存
     */
    public PhysicalMemory getPhysicalMemory() {
        return physicalMemory;
    }
    
    /**
     * 获取页帧表
     * @return 页帧表
     */
    public PageFrameTable getPageFrameTable() {
        return pageFrameTable;
    }
} 