package newOs.kernel.interrupt.hardwareHandler;

import lombok.extern.slf4j.Slf4j;
import newOs.exception.PageFaultException;
import newOs.kernel.interrupt.HardwareInterruptHandler;
import newOs.kernel.interrupt.InterruptContext;
import newOs.kernel.interrupt.InterruptType;
import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.virtual.PageTable;
import newOs.kernel.memory.virtual.PageFrameTable;
import newOs.kernel.memory.virtual.paging.SwapManager;
import newOs.kernel.memory.virtual.paging.Page;
import newOs.kernel.memory.virtual.paging.PageFrame;
import newOs.kernel.memory.virtual.replacement.PageReplacementManager;
import newOs.kernel.process.ProcessManager;
import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.MemoryInterruptInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 页面错误中断处理器
 * 负责处理程序访问不在物理内存中的页面时产生的页面错误异常
 */
@Component
@Slf4j
public class PageFaultHandler implements HardwareInterruptHandler {

    /**
     * 内存管理器
     */
    private final MemoryManager memoryManager;
    
    /**
     * 进程管理器
     */
    private ProcessManager processManager;
    
    /**
     * 页表
     */
    private final PageTable pageTable;
    
    /**
     * 页帧表
     */
    private final PageFrameTable pageFrameTable;
    
    /**
     * 交换管理器
     */
    private final SwapManager swapManager;
    
    /**
     * 页面替换管理器
     */
    private final PageReplacementManager pageReplacementManager;
    
    /**
     * 页面错误计数器
     */
    private final AtomicLong pageFaultCount = new AtomicLong(0);
    
    /**
     * 主要页面错误计数器（第一次访问）
     */
    private final AtomicLong majorFaultCount = new AtomicLong(0);
    
    /**
     * 次要页面错误计数器（已在交换区）
     */
    private final AtomicLong minorFaultCount = new AtomicLong(0);
    
    /**
     * 页面换入计数器
     */
    private final AtomicLong pageInCount = new AtomicLong(0);
    
    /**
     * 页面换出计数器
     */
    private final AtomicLong pageOutCount = new AtomicLong(0);
    
    /**
     * 页错误处理时间统计（毫秒）
     */
    private long totalHandlingTime = 0;
    private long handlingCount = 0;
    
    /**
     * 构造页面错误中断处理器
     * @param memoryManager 内存管理器
     * @param pageTable 页表
     * @param pageFrameTable 页帧表
     * @param swapManager 交换管理器
     * @param pageReplacementManager 页面替换管理器
     */
    @Autowired
    public PageFaultHandler(MemoryManager memoryManager,
                           PageTable pageTable,
                           PageFrameTable pageFrameTable,
                           SwapManager swapManager,
                           PageReplacementManager pageReplacementManager) {
        this.memoryManager = memoryManager;
        this.pageTable = pageTable;
        this.pageFrameTable = pageFrameTable;
        this.swapManager = swapManager;
        this.pageReplacementManager = pageReplacementManager;
        log.info("页面错误中断处理器初始化完成");
    }
    
    /**
     * 设置进程管理器
     * @param processManager 进程管理器
     */
    @Autowired
    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

    @Override
    public newOs.common.InterruptConstant.InterruptType getType() {
        return newOs.common.InterruptConstant.InterruptType.PAGE_FAULT;
    }

    @Override
    public boolean handle(InterruptContext context) {
        if (context == null) {
            log.error("中断上下文为空，无法处理页面错误中断");
            return false;
        }
        
        // 统计开始时间
        long startTime = System.currentTimeMillis();
        
        // 增加页面错误计数
        pageFaultCount.incrementAndGet();
        
        try {
            // 从中断上下文中获取进程ID和虚拟地址
            int pid = context.getProcessId();
            long faultAddress = context.getFaultAddress();
            VirtualAddress virtualAddress = new VirtualAddress((int)faultAddress);
            
            log.debug("处理页面错误中断: 进程={}, 虚拟地址={}", pid, faultAddress);
            
            // 获取虚拟地址对应的页面 - 修正参数顺序
            Page page = pageTable.getPage(virtualAddress, pid);
            
            // 检查页面是否存在
            if (page == null) {
                log.debug("页面不存在，可能是非法地址访问: 进程={}, 虚拟地址={}", pid, faultAddress);
                // 向进程发送非法内存访问异常
                processManager.sendSignal(pid, "SIGSEGV", "非法内存地址访问");
                return true;
            }
            
            // 检查页面是否在物理内存中
            if (page.isPresent()) {
                log.debug("页面已在内存中，但产生了页错误，可能是权限问题: 进程={}, 页面={}", pid, page);
                // 如果页面已经在内存中，可能是权限问题，交给内存保护处理器处理
                return false;
            }
            
            // 检查是否是由于写时复制导致的页面错误
            if (context.isWriteAccess() && page.isShared() && page.isCopyOnWrite()) {
                return handleCopyOnWrite(pid, page, virtualAddress);
            }
            
            // 记录页面访问
            pageReplacementManager.recordPageAccess(page);
            
            // 检查页面是否在交换区
            if (page.hasSwapLocation()) {
                // 次要页面错误（页面在交换区）
                minorFaultCount.incrementAndGet();
                return handleMinorPageFault(pid, page, virtualAddress);
            } else {
                // 主要页面错误（页面首次访问）
                majorFaultCount.incrementAndGet();
                return handleMajorPageFault(pid, page, virtualAddress);
            }
        } catch (Exception e) {
            log.error("处理页面错误中断时发生异常: {}", e.getMessage(), e);
            return false;
        } finally {
            // 统计处理时间
            long endTime = System.currentTimeMillis();
            long handlingTime = endTime - startTime;
            totalHandlingTime += handlingTime;
            handlingCount++;
            
            if (handlingTime > 100) {
                log.warn("页面错误处理时间过长: {}ms", handlingTime);
            }
        }
    }
    
    /**
     * 处理写时复制情况
     * @param pid 进程ID
     * @param page 页面
     * @param virtualAddress 虚拟地址
     * @return 处理是否成功
     */
    private boolean handleCopyOnWrite(int pid, Page page, VirtualAddress virtualAddress) {
        try {
            log.debug("处理写时复制页面错误: 进程={}, 页面={}", pid, page);
            
            // 创建页面的私有副本
            boolean success = memoryManager.createPageCopy(pid, page, virtualAddress);
            
            if (success) {
                log.debug("写时复制成功: 进程={}, 页面={}", pid, page);
                return true;
            } else {
                log.error("写时复制失败: 进程={}, 页面={}", pid, page);
                return false;
            }
        } catch (Exception e) {
            log.error("处理写时复制时发生异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 处理次要页面错误（页面在交换区）
     * @param pid 进程ID
     * @param page 页面
     * @param virtualAddress 虚拟地址
     * @return 处理是否成功
     */
    private boolean handleMinorPageFault(int pid, Page page, VirtualAddress virtualAddress) {
        try {
            log.debug("处理次要页面错误: 进程={}, 页面={}", pid, page);
            
            // 分配页帧
            PageFrame frame = allocatePageFrame(pid, page);
            if (frame == null) {
                log.error("无法分配页帧: 进程={}, 页面={}", pid, page);
                return false;
            }
            
            // 从交换区加载页面内容
            long swapLocation = page.getSwapLocation();
            boolean success = swapManager.swapIn(swapLocation, frame.getFrameNumber());
            
            if (success) {
                // 更新页表
                page.setFrameNumber(frame.getFrameNumber());
                page.setPresent(true);
                page.resetAccessed();
                page.resetDirty();
                
                // 统计页面换入
                pageInCount.incrementAndGet();
                
                log.debug("页面从交换区加载成功: 进程={}, 页面={}, 页帧={}", 
                        pid, page, frame.getFrameNumber());
                return true;
            } else {
                log.error("从交换区加载页面失败: 进程={}, 页面={}, 交换位置={}", 
                        pid, page, swapLocation);
                
                // 释放分配的页帧
                pageFrameTable.freeFrame(frame.getFrameNumber());
                return false;
            }
        } catch (Exception e) {
            log.error("处理次要页面错误时发生异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 处理主要页面错误（页面首次访问）
     * @param pid 进程ID
     * @param page 页面
     * @param virtualAddress 虚拟地址
     * @return 处理是否成功
     */
    private boolean handleMajorPageFault(int pid, Page page, VirtualAddress virtualAddress) {
        try {
            log.debug("处理重型缺页: 进程={}, 页面={}, 地址={}", pid, page, virtualAddress);
            majorFaultCount.incrementAndGet();
            
            // 首先检查页面是否已经在交换区中
            if (page.hasSwapLocation()) {
                log.debug("页面在交换区中，位置: {}", page.getSwapLocation());
                
                // 分配页帧
                PageFrame frame = allocatePageFrame(pid, page);
                if (frame == null) {
                    log.error("无法分配页帧: 进程={}, 页面={}", pid, page);
                    return false;
                }
                
                // 从交换区加载页面内容
                long swapLocation = page.getSwapLocation();
                boolean success = swapManager.swapIn(swapLocation, frame.getFrameNumber());
                
                if (success) {
                    // 更新页表
                    page.setFrameNumber(frame.getFrameNumber());
                    page.setPresent(true);
                    page.resetAccessed();
                    page.resetDirty();
                    
                    log.debug("从交换区加载页面成功: 进程={}, 页面={}, 交换区位置={}, 页帧={}",
                            pid, page, swapLocation, frame.getFrameNumber());
                    pageInCount.incrementAndGet();
                    return true;
                } else {
                    log.error("从交换区加载页面失败: 进程={}, 页面={}, 交换区位置={}",
                            pid, page, swapLocation);
                    return false;
                }
            } else {
                log.debug("页面不在交换区中，需要初始化新页面");
                
                // 分配页帧
                PageFrame frame = allocatePageFrame(pid, page);
                if (frame == null) {
                    log.error("无法分配页帧: 进程={}, 页面={}", pid, page);
                    return false;
                }
                
                // 初始化页帧内容
                frame.clear();
                
                // 判断页面类型并进行相应处理
                if (page.isCodePage()) {
                    // 加载代码页
                    boolean success = memoryManager.loadCodePage(pid, page, frame);
                    if (!success) {
                        log.error("加载代码页失败: 进程={}, 页面={}", pid, page);
                        return false;
                    }
                } else if (page.isDataPage()) {
                    // 加载数据页
                    boolean success = memoryManager.loadDataPage(pid, page, frame);
                    if (!success) {
                        log.error("加载数据页失败: 进程={}, 页面={}", pid, page);
                        return false;
                    }
                } else {
                    // 默认为零页
                    log.debug("初始化零页: 进程={}, 页面={}", pid, page);
                }
                
                // 更新页表
                page.setFrameNumber(frame.getFrameNumber());
                page.setPresent(true);
                page.resetAccessed();
                
                log.debug("新页面初始化成功: 进程={}, 页面={}, 页帧={}",
                        pid, page, frame.getFrameNumber());
                return true;
            }
        } catch (Exception e) {
            log.error("处理重型缺页异常时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 分配页帧
     * @param pid 进程ID
     * @param page 页面
     * @return 分配的页帧
     */
    private PageFrame allocatePageFrame(int pid, Page page) {
        try {
            // 尝试分配空闲页帧
            PageFrame frame = pageFrameTable.allocateFrame(pid, page.getPageNumber());
            
            if (frame != null) {
                log.debug("成功分配页帧: 帧号={}, 进程={}, 页面={}", 
                        frame.getFrameNumber(), pid, page);
                return frame;
            }
            
            // 如果无法直接分配，尝试页面置换
            log.debug("无法直接分配页帧，尝试页面置换");
            
            // 选择牺牲页面
            Page victimPage = pageReplacementManager.selectVictimPage();
            
            if (victimPage == null) {
                log.error("页面置换失败：无法选择牺牲页面");
                return null;
            }
            
            // 处理受害页面
            return handleVictimPage(victimPage, pid, page);
        } catch (Exception e) {
            log.error("分配页帧时发生异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 处理受害页面
     * @param victimPage 受害页面
     * @param newPid 新进程ID
     * @param newPage 新页面
     * @return 分配的页帧
     */
    private PageFrame handleVictimPage(Page victimPage, int newPid, Page newPage) {
        try {
            int victimPid = victimPage.getPid();
            PageFrame frame = pageFrameTable.getFrame(victimPage.getFrameNumber());
            
            log.debug("处理受害页面: 进程={}, 页面={}, 页帧={}",
                    victimPid, victimPage, frame.getFrameNumber());
            
            // 如果页面被修改过，需要将其写入交换区
            if (victimPage.isDirty()) {
                log.debug("页面被修改过，需要写入交换区: 进程={}, 页面={}",
                        victimPid, victimPage);
                
                // 分配交换区空间
                int pageNumber = victimPage.getPageNumber();
                long swapLocation = swapManager.allocateSwapArea(victimPid, pageNumber);
                if (swapLocation < 0) {
                    log.error("无法分配交换区空间");
                    return null;
                }
                
                // 将页面写入交换区
                boolean success = swapManager.swapOutPage(victimPid, pageNumber, frame.getFrameNumber());
                if (!success) {
                    log.error("写入交换区失败: 进程={}, 页面={}, 交换区位置={}",
                            victimPid, victimPage, swapLocation);
                    return null;
                }
                
                // 更新页表
                pageTable.markPageNotPresent(
                        victimPid, 
                        victimPage.getPageNumber(), 
                        swapLocation);
                
                pageOutCount.incrementAndGet();
                log.debug("页面成功写入交换区: 进程={}, 页面={}, 交换区位置={}",
                        victimPid, victimPage, swapLocation);
            } else {
                // 如果页面没有修改过，直接标记为不在内存中
                pageTable.markPageNotPresent(
                        victimPid, 
                        victimPage.getPageNumber(), 
                        victimPage.getSwapLocation());
            }
            
            // 重新初始化页帧
            frame.clear();
            
            // 重新分配给新进程的页面
            frame.setProcessId(newPid);
            frame.setPageNumber(newPage.getPageNumber());
            
            return frame;
        } catch (Exception e) {
            log.error("处理受害页面时发生异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取页面错误总数
     * @return 页面错误总数
     */
    public long getPageFaultCount() {
        return pageFaultCount.get();
    }
    
    /**
     * 获取主要页面错误计数
     * @return 主要页面错误计数
     */
    public long getMajorFaultCount() {
        return majorFaultCount.get();
    }
    
    /**
     * 获取次要页面错误计数
     * @return 次要页面错误计数
     */
    public long getMinorFaultCount() {
        return minorFaultCount.get();
    }
    
    /**
     * 获取页面换入计数
     * @return 页面换入计数
     */
    public long getPageInCount() {
        return pageInCount.get();
    }
    
    /**
     * 获取页面换出计数
     * @return 页面换出计数
     */
    public long getPageOutCount() {
        return pageOutCount.get();
    }
    
    /**
     * 获取平均处理时间（毫秒）
     * @return 平均处理时间
     */
    public double getAverageHandlingTime() {
        if (handlingCount == 0) {
            return 0;
        }
        return (double) totalHandlingTime / handlingCount;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        pageFaultCount.set(0);
        majorFaultCount.set(0);
        minorFaultCount.set(0);
        pageInCount.set(0);
        pageOutCount.set(0);
        totalHandlingTime = 0;
        handlingCount = 0;
    }
    
    /**
     * 获取统计信息
     * @return 统计信息字符串
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("页面错误统计:\n");
        sb.append("总页面错误数: ").append(pageFaultCount.get()).append("\n");
        sb.append("主要页面错误数: ").append(majorFaultCount.get()).append("\n");
        sb.append("次要页面错误数: ").append(minorFaultCount.get()).append("\n");
        sb.append("页面换入数: ").append(pageInCount.get()).append("\n");
        sb.append("页面换出数: ").append(pageOutCount.get()).append("\n");
        sb.append("平均处理时间: ").append(String.format("%.2f毫秒", getAverageHandlingTime())).append("\n");
        
        long total = pageFaultCount.get();
        if (total > 0) {
            double majorRate = (double) majorFaultCount.get() / total * 100;
            double minorRate = (double) minorFaultCount.get() / total * 100;
            
            sb.append("主要页面错误率: ").append(String.format("%.2f%%", majorRate)).append("\n");
            sb.append("次要页面错误率: ").append(String.format("%.2f%%", minorRate)).append("\n");
        }
        
        return sb.toString();
    }

    @Override
    public InterruptInfo handle(InterruptInfo info) {
        long startTime = System.nanoTime();
        pageFaultCount.incrementAndGet();
        
        try {
            // 将 InterruptInfo 转换为 MemoryInterruptInfo
            if (!(info instanceof MemoryInterruptInfo)) {
                log.warn("非页错误中断处理请求");
                return info;
            }
            
            MemoryInterruptInfo memoryInfo = (MemoryInterruptInfo) info;
            
            int processId = memoryInfo.getProcessId();
            String addressStr = memoryInfo.getAddress();
            long virtualAddress = Long.parseLong(addressStr.replace("0x", ""), 16);
            boolean isWrite = false; // 默认为读操作
            
            // 处理页错误
            boolean handled = handlePageFault(processId, virtualAddress, isWrite);
            
            long endTime = System.nanoTime();
            long handlingTime = endTime - startTime;
            
            // 更新统计信息
            totalHandlingTime += handlingTime;
            handlingCount++;
            
            log.debug("页面错误处理完成：进程={}, 地址={}, 写入={}, 处理结果={}, 耗时={}ns",
                    processId, virtualAddress, isWrite, handled, handlingTime);
            
            // 设置处理结果
            memoryInfo.setAdditionalInfo("success", handled);
            return memoryInfo;
        } catch (Exception e) {
            log.error("处理页面错误中断时发生异常: {}", e.getMessage(), e);
            return info;
        }
    }

    /**
     * 处理缺页异常
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param isWrite 是否是写操作
     * @return 是否处理成功
     */
    public boolean handlePageFault(int processId, long virtualAddressValue, boolean isWrite) {
        try {
            // 构造虚拟地址对象
            VirtualAddress virtualAddress = new VirtualAddress(virtualAddressValue);
            
            // 获取页面
            Page page = pageTable.getPage(virtualAddress, processId);
            if (page == null) {
                log.error("缺页处理失败：页面不存在，进程={}，页号={}", 
                         processId, virtualAddress.getPageNumber());
                return false;
            }
            
            // 处理缺页
            return handlePageFaultForProcess(processId, page, isWrite);
        } catch (Exception e) {
            log.error("缺页处理失败：{}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 处理进程的缺页异常
     * @param processId 进程ID
     * @param page 页面
     * @param isWrite 是否是写操作
     * @return 是否处理成功
     */
    private boolean handlePageFaultForProcess(int processId, Page page, boolean isWrite) {
        try {
            // 检查页面是否在交换区
            if (page.hasSwapLocation()) {
                // 分配页帧
                PageFrame frame = pageFrameTable.allocateFrame(processId, page.getPageNumber());
                if (frame == null) {
                    log.error("缺页处理失败：无法分配页帧，进程={}，页号={}", 
                             processId, page.getPageNumber());
                    return false;
                }
                
                // 从交换区加载页面
                long swapLocation = page.getSwapLocation();
                boolean success = swapManager.swapIn(swapLocation, frame.getFrameNumber());
                
                if (success) {
                    // 更新页面
                    page.assignFrame(frame.getFrameNumber());
                    page.setPresent(true);
                    page.setDirty(isWrite);
                    
                    log.info("缺页处理成功：从交换区加载页面，进程={}，页号={}，帧号={}", 
                             processId, page.getPageNumber(), frame.getFrameNumber());
                    
                    return true;
                } else {
                    log.error("缺页处理失败：无法从交换区加载页面，进程={}，页号={}", 
                             processId, page.getPageNumber());
                    return false;
                }
            }
            
            // 如果是写操作，需要创建页面副本
            if (isWrite && page.isShared()) {
                // 分配新页帧
                PageFrame newFrame = pageFrameTable.allocateFrame(processId, page.getPageNumber());
                if (newFrame == null) {
                    log.error("缺页处理失败：无法分配新页帧，进程={}，页号={}", 
                             processId, page.getPageNumber());
                    return false;
                }
                
                // 复制页面内容
                if (!memoryManager.createPageCopy(processId, page, new VirtualAddress(page.getPageNumber() * 4096))) {
                    log.error("缺页处理失败：无法创建页面副本，进程={}，页号={}", 
                             processId, page.getPageNumber());
                    return false;
                }
                
                log.info("缺页处理成功：创建页面副本，进程={}，页号={}，帧号={}", 
                         processId, page.getPageNumber(), newFrame.getFrameNumber());
                
                return true;
            }
            
            // 分配新页帧
            PageFrame frame = pageFrameTable.allocateFrame(processId, page.getPageNumber());
            if (frame == null) {
                log.error("缺页处理失败：无法分配页帧，进程={}，页号={}", 
                         processId, page.getPageNumber());
                return false;
            }
            
            // 初始化页面
            if (page.isCodePage()) {
                if (!memoryManager.loadCodePage(processId, page, frame)) {
                    log.error("缺页处理失败：无法加载代码页面，进程={}，页号={}", 
                             processId, page.getPageNumber());
                    return false;
                }
            } else {
                if (!memoryManager.loadDataPage(processId, page, frame)) {
                    log.error("缺页处理失败：无法加载数据页面，进程={}，页号={}", 
                             processId, page.getPageNumber());
                    return false;
                }
            }
            
            // 更新页面
            page.assignFrame(frame.getFrameNumber());
            page.setPresent(true);
            page.setDirty(isWrite);
            
            log.info("缺页处理成功：加载新页面，进程={}，页号={}，帧号={}", 
                     processId, page.getPageNumber(), frame.getFrameNumber());
            
            return true;
        } catch (Exception e) {
            log.error("缺页处理失败：{}", e.getMessage(), e);
            return false;
        }
    }
} 