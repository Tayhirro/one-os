package newOs.kernel.interrupt.hardwareHandler;

import lombok.extern.slf4j.Slf4j;
import newOs.kernel.interrupt.ISR;
import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.virtual.PageTable;
import newOs.kernel.memory.virtual.PageFrameTable;
import newOs.kernel.memory.virtual.paging.SwapManager;
import newOs.kernel.memory.virtual.paging.Page;
import newOs.kernel.memory.virtual.paging.PageFrame;
import newOs.kernel.memory.virtual.replacement.PageReplacementManager;
import newOs.kernel.process.ProcessManager;
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
public class PageFaultHandler implements ISR<MemoryInterruptInfo> {

    /**
     * 内存管理器
     */
    private final MemoryManager memoryManager;
    
    /**
     * 进程管理器
     */
    private final ProcessManager processManager;
    
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
     * @param processManager 进程管理器
     */
    @Autowired
    public PageFaultHandler(MemoryManager memoryManager,
                           PageTable pageTable,
                           PageFrameTable pageFrameTable,
                           SwapManager swapManager,
                           PageReplacementManager pageReplacementManager,
                           ProcessManager processManager) {
        this.memoryManager = memoryManager;
        this.pageTable = pageTable;
        this.pageFrameTable = pageFrameTable;
        this.swapManager = swapManager;
        this.pageReplacementManager = pageReplacementManager;
        this.processManager = processManager;
        log.info("页面错误中断处理器初始化完成");
    }

    @Override
    public InterruptInfo execute(MemoryInterruptInfo info) {
        if (info == null) {
            log.error("中断信息为空，无法处理页面错误中断");
            return null;
        }
        
        // 统计开始时间
        long startTime = System.currentTimeMillis();
        
        // 增加页面错误计数
        pageFaultCount.incrementAndGet();
        
        try {
            // 从中断信息中获取进程ID和虚拟地址
            int pid = info.getProcessId();
            String addressStr = info.getAddress();
            long faultAddress = Long.parseLong(addressStr.replace("0x", ""), 16);
            VirtualAddress virtualAddress = info.getVirtualAddress();
            if (virtualAddress == null) {
                virtualAddress = new VirtualAddress((int)faultAddress);
            }
            
            // 获取访问类型
            String accessType = (String) info.getAdditionalInfo("accessType");
            boolean isWrite = "WRITE".equals(accessType);
            
            log.debug("处理页面错误中断: 进程={}, 虚拟地址={}", pid, faultAddress);
            
            // 获取虚拟地址对应的页面
            Page page = pageTable.getPage(virtualAddress, pid);
            
            // 如果页面不存在
            if (page == null) {
                log.debug("页面不存在，可能是非法地址访问: 进程={}, 虚拟地址={}", pid, faultAddress);
                // 向进程发送非法内存访问异常
                processManager.sendSignal(pid, "SIGSEGV", "非法内存地址访问");
                info.addAdditionalInfo("success", false);
                info.addAdditionalInfo("error", "非法地址访问");
                return info;
            }
            
            // 处理页面错误逻辑
            boolean success = handlePageFaultForProcess(pid, page, isWrite);
            
            // 设置处理结果
            info.addAdditionalInfo("success", success);
            
            // 统计处理时间
            long endTime = System.currentTimeMillis();
            long handlingTime = endTime - startTime;
            totalHandlingTime += handlingTime;
            handlingCount++;
            
            if (handlingTime > 100) {
                log.warn("页面错误处理时间过长: {}ms", handlingTime);
            }
            
            return info;
        } catch (Exception e) {
            log.error("处理页面错误中断时发生异常: {}", e.getMessage(), e);
            info.addAdditionalInfo("error", e.getMessage());
            info.addAdditionalInfo("success", false);
            return info;
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