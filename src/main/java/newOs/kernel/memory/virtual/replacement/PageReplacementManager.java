package newOs.kernel.memory.virtual.replacement;

import lombok.extern.slf4j.Slf4j;
import newOs.exception.MemoryException;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.virtual.paging.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 页面置换管理器接口
 * 负责处理缺页异常和页面置换
 */
public interface PageReplacementManager {
    
    /**
     * 处理缺页异常
     * @param processId 进程ID
     * @param virtualAddress 产生缺页的虚拟地址
     * @param isWrite 是否是写操作产生的缺页
     * @return 是否是次缺页（minor page fault）
     * @throws MemoryException 内存异常
     */
    boolean handlePageFault(int processId, VirtualAddress virtualAddress, boolean isWrite) throws MemoryException;
    
    /**
     * 设置页面置换算法
     * @param algorithm 算法名称（如LRU、FIFO、Clock等）
     * @throws MemoryException 如果算法不支持
     */
    void setReplacementAlgorithm(String algorithm) throws MemoryException;
    
    /**
     * 获取当前页面置换算法
     * @return 当前算法名称
     */
    String getCurrentAlgorithm();
    
    /**
     * 获取缺页率
     * @return 缺页率（0.0-1.0）
     */
    double getPageFaultRate();
    
    /**
     * 获取主缺页率（major page fault）
     * @return 主缺页率（0.0-1.0）
     */
    double getMajorPageFaultRate();
    
    /**
     * 获取次缺页率（minor page fault）
     * @return 次缺页率（0.0-1.0）
     */
    double getMinorPageFaultRate();
    
    /**
     * 重置统计数据
     */
    void resetStatistics();
    
    /**
     * 记录页面访问
     * @param page 被访问的页面
     */
    void recordPageAccess(Page page);
    
    /**
     * 选择牺牲页面（要被替换的页面）
     * @return 选择的牺牲页面，如果没有可替换页面则返回null
     */
    Page selectVictimPage();
}

/**
 * 页面替换管理器
 * 负责实现页面替换算法，选择要替换的页面
 */
@Component
@Slf4j
class PageReplacementManagerImpl implements PageReplacementManager {

    // 页面访问记录队列
    private final Queue<Page> accessQueue = new LinkedList<>();
    
    // 页面引用列表（保存的页面引用）
    private final List<Page> pageReferences = new ArrayList<>();
    
    // 算法名称
    private String algorithm = "CLOCK";
    
    // 统计数据
    private long pageFaults = 0;
    private long majorFaults = 0;
    private long minorFaults = 0;
    private long memoryAccesses = 0;
    
    /**
     * 记录页面访问
     * @param page 被访问的页面
     */
    @Override
    public void recordPageAccess(Page page) {
        if (page == null) {
            return;
        }
        
        // 标记页面被访问
        page.setAccessed(true);
        
        // 记录访问顺序，用于LRU算法
        accessQueue.add(page);
        
        // 如果队列太长，删除旧的记录
        if (accessQueue.size() > 1000) {
            accessQueue.poll();
        }
        
        // 记录内存访问
        memoryAccesses++;
        
        log.debug("记录页面访问: {}", page);
    }
    
    /**
     * 选择牺牲页面（要被替换的页面）
     * @return 选择的牺牲页面，如果没有可替换页面则返回null
     */
    @Override
    public Page selectVictimPage() {
        // 实现改进的时钟算法 (Enhanced Clock Algorithm)
        
        // 第一次扫描：寻找未访问且未修改的页面 (A=0, D=0)
        for (Page page : pageReferences) {
            if (page.isPresent() && !page.isAccessed() && !page.isDirty()) {
                log.debug("选择牺牲页面(未访问未修改): {}", page);
                return page;
            }
        }
        
        // 第二次扫描：寻找未访问但已修改的页面 (A=0, D=1)
        for (Page page : pageReferences) {
            if (page.isPresent() && !page.isAccessed() && page.isDirty()) {
                log.debug("选择牺牲页面(未访问已修改): {}", page);
                return page;
            }
        }
        
        // 第三次扫描：寻找已访问但未修改的页面 (A=1, D=0)
        // 同时重置访问位
        for (Page page : pageReferences) {
            if (page.isPresent() && page.isAccessed() && !page.isDirty()) {
                page.resetAccessed(); // 重置访问位
                log.debug("选择牺牲页面(已访问未修改): {}", page);
                return page;
            }
        }
        
        // 第四次扫描：寻找已访问且已修改的页面 (A=1, D=1)
        // 同时重置访问位
        for (Page page : pageReferences) {
            if (page.isPresent() && page.isAccessed() && page.isDirty()) {
                page.resetAccessed(); // 重置访问位
                log.debug("选择牺牲页面(已访问已修改): {}", page);
                return page;
            }
        }
        
        // 如果列表为空，尝试从访问队列中获取最早的页面（LRU备选）
        if (!accessQueue.isEmpty()) {
            Page oldestPage = accessQueue.poll();
            if (oldestPage != null && oldestPage.isPresent()) {
                log.debug("选择牺牲页面(LRU): {}", oldestPage);
                return oldestPage;
            }
        }
        
        log.warn("无法选择牺牲页面");
        return null;
    }
    
    /**
     * 添加页面引用到管理器
     * @param page 页面对象
     */
    public void addPageReference(Page page) {
        if (page != null && !pageReferences.contains(page)) {
            pageReferences.add(page);
        }
    }
    
    /**
     * 移除页面引用
     * @param page 页面对象
     */
    public void removePageReference(Page page) {
        if (page != null) {
            pageReferences.remove(page);
        }
    }
    
    /**
     * 获取管理的页面数量
     * @return 管理的页面数量
     */
    public int getPageCount() {
        return pageReferences.size();
    }
    
    @Override
    public boolean handlePageFault(int processId, VirtualAddress virtualAddress, boolean isWrite) throws MemoryException {
        // 增加缺页计数
        pageFaults++;
        log.info("【页面置换】处理进程{}的缺页异常，虚拟地址=0x{}，是否为写操作={}", 
                processId, Long.toHexString(virtualAddress.getValue()), isWrite);
        
        try {
            // 确定页面的虚拟地址范围和属性
            int pageNumber = virtualAddress.getPageNumber();
            int offset = virtualAddress.getOffset();
            
            log.debug("【页面置换】缺页详情：页号={}，偏移量={}", 
                    pageNumber, offset);
            
            // 检查页面是否已经存在于页表中
            Page page = null;
            for (Page p : pageReferences) {
                if (p.getPid() == processId && p.getPageNumber() == pageNumber) {
                    page = p;
                    break;
                }
            }
            
            boolean isMinor = false;
            
            // 如果页面不存在，需要创建新页面
            if (page == null) {
                log.info("【页面置换】页面不存在，创建新页面: 进程={}，页号={}", 
                        processId, pageNumber);
                
                // 创建新页面
                page = new Page(pageNumber, processId);
                page.setPresent(false);
                page.setProtection(true, isWrite, false); // 设置为可读，根据isWrite设置可写权限，不可执行
                page.setShared(false);
                
                // 添加到页面引用列表
                addPageReference(page);
                
                // 主要缺页 - 首次访问
                isMinor = false;
                majorFaults++;
            } else if (page.hasSwapLocation()) {
                // 页面在交换区中 - 次要缺页
                log.info("【页面置换】页面在交换区中: 进程={}，页号={}，交换位置={}", 
                        processId, pageNumber, page.getSwapLocation());
                isMinor = true;
                minorFaults++;
            } else {
                // 页面不在内存也不在交换区 - 主要缺页
                log.info("【页面置换】页面不在内存也不在交换区: 进程={}，页号={}", 
                        processId, pageNumber);
                isMinor = false;
                majorFaults++;
            }
            
            // 记录页面访问
            recordPageAccess(page);
            
            // 分配页帧
            int frameNumber = allocateFrame(page);
            if (frameNumber < 0) {
                // 没有空闲页帧，需要进行页面置换
                log.info("【页面置换】没有空闲页帧，执行页面置换");
                Page victimPage = selectVictimPage();
                if (victimPage == null) {
                    log.error("【页面置换】无法选择牺牲页面，缺页处理失败");
                    throw new MemoryException("无法选择牺牲页面");
                }
                
                // 处理受害页面
                frameNumber = handleVictimPage(victimPage, page);
                if (frameNumber < 0) {
                    log.error("【页面置换】处理牺牲页面失败，缺页处理失败");
                    throw new MemoryException("处理牺牲页面失败");
                }
            }
            
            // 更新页表
            page.assignFrame(frameNumber);
            page.resetAccessed();
            page.resetDirty();
            
            log.info("【页面置换】缺页处理成功: 进程={}，页号={}，页帧号={}", 
                    processId, pageNumber, frameNumber);
            
            // 返回是否是次要缺页
            return isMinor;
        } catch (Exception e) {
            log.error("【页面置换】缺页处理异常: {}", e.getMessage(), e);
            throw new MemoryException("缺页处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 分配页帧
     * @param page 要分配页帧的页面
     * @return 分配的页帧号，如果分配失败返回-1
     */
    private int allocateFrame(Page page) {
        // 简化实现：随机返回一个页帧号
        // 实际实现应该查询页帧表，分配空闲页帧
        return (int)(Math.random() * 100);
    }
    
    /**
     * 处理牺牲页面
     * @param victimPage 选择的牺牲页面
     * @param newPage 新页面
     * @return 释放的页帧号，处理失败返回-1
     */
    private int handleVictimPage(Page victimPage, Page newPage) {
        // 获取牺牲页面的页帧号
        int frameNumber = victimPage.getFrameNumber();
        
        // 如果页面被修改过，需要写入交换区
        if (victimPage.isDirty()) {
            // 在实际实现中，这里应该调用SwapManager写入交换区
            long swapLocation = allocateSwapLocation(victimPage);
            victimPage.swapOut(swapLocation);
            
            log.info("【页面置换】牺牲页面被修改过，写入交换区: 进程={}，页号={}，交换位置={}", 
                    victimPage.getPid(), victimPage.getPageNumber(), swapLocation);
        } else {
            // 页面没有被修改，直接移出内存
            victimPage.setPresent(false);
            victimPage.setFrameNumber(-1);
        }
        
        return frameNumber;
    }
    
    /**
     * 分配交换区位置
     * @param page 页面
     * @return 交换区位置
     */
    private long allocateSwapLocation(Page page) {
        // 简化实现：返回一个随机位置
        // 实际实现应该调用SwapManager分配交换区空间
        return System.currentTimeMillis();
    }
    
    @Override
    public void setReplacementAlgorithm(String algorithm) throws MemoryException {
        if (algorithm == null) {
            throw new MemoryException("算法名称不能为空");
        }
        
        // 验证是否支持该算法
        switch (algorithm.toUpperCase()) {
            case "CLOCK":
            case "LRU":
            case "FIFO":
            case "SECOND_CHANCE":
            case "NRU":
                this.algorithm = algorithm.toUpperCase();
                log.info("页面置换算法已设置为: {}", this.algorithm);
                break;
            default:
                throw new MemoryException("不支持的页面置换算法: " + algorithm);
        }
    }
    
    @Override
    public String getCurrentAlgorithm() {
        return algorithm;
    }
    
    @Override
    public double getPageFaultRate() {
        return memoryAccesses > 0 ? (double) pageFaults / memoryAccesses : 0.0;
    }
    
    @Override
    public double getMajorPageFaultRate() {
        return memoryAccesses > 0 ? (double) majorFaults / memoryAccesses : 0.0;
    }
    
    @Override
    public double getMinorPageFaultRate() {
        return memoryAccesses > 0 ? (double) minorFaults / memoryAccesses : 0.0;
    }
    
    @Override
    public void resetStatistics() {
        pageFaults = 0;
        majorFaults = 0;
        minorFaults = 0;
        memoryAccesses = 0;
        log.info("页面置换统计信息已重置");
    }
} 