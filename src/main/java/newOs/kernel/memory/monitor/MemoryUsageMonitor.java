package newOs.kernel.memory.monitor;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.PhysicalMemory;
import newOs.kernel.memory.virtual.PageFrameTable;
import newOs.kernel.memory.virtual.paging.SwapManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 内存使用监控器
 * 用于定期收集和分析系统内存使用情况，发现异常并进行预警
 */
@Component
@Slf4j
public class MemoryUsageMonitor {
    
    /**
     * 内存管理器
     */
    private final MemoryManager memoryManager;
    
    /**
     * 物理内存
     */
    private final PhysicalMemory physicalMemory;
    
    /**
     * 页帧表
     */
    private final PageFrameTable pageFrameTable;
    
    /**
     * 交换管理器
     */
    private final SwapManager swapManager;
    
    /**
     * 内存统计
     */
    private final MemoryStats memoryStats;
    
    /**
     * 缺页统计
     */
    private final PageFaultStats pageFaultStats;
    
    /**
     * TLB统计
     */
    private final TLBStats tlbStats;
    
    /**
     * 物理内存使用率预警阈值（百分比）
     */
    @Getter
    @Setter
    @Value("${memory.monitor.physical.threshold:80}")
    private int physicalMemoryThreshold = 80;
    
    /**
     * 交换空间使用率预警阈值（百分比）
     */
    @Getter
    @Setter
    @Value("${memory.monitor.swap.threshold:70}")
    private int swapSpaceThreshold = 70;
    
    /**
     * 缺页率预警阈值（每秒）
     */
    @Getter
    @Setter
    @Value("${memory.monitor.pagefault.threshold:100}")
    private int pageFaultRateThreshold = 100;
    
    /**
     * 内存泄漏检测阈值（连续增长次数）
     */
    @Getter
    @Setter
    @Value("${memory.monitor.leak.threshold:5}")
    private int memoryLeakThreshold = 5;
    
    /**
     * 碎片化预警阈值
     */
    @Getter
    @Setter
    @Value("${memory.monitor.fragmentation.threshold:0.6}")
    private double fragmentationThreshold = 0.6;
    
    /**
     * 是否启用监控
     */
    @Getter
    @Setter
    @Value("${memory.monitor.enabled:true}")
    private boolean monitoringEnabled = true;
    
    /**
     * 是否启用预警
     */
    @Getter
    @Setter
    @Value("${memory.monitor.alerts.enabled:true}")
    private boolean alertsEnabled = true;
    
    /**
     * 当前是否存在物理内存预警
     */
    @Getter
    private final AtomicBoolean physicalMemoryAlert = new AtomicBoolean(false);
    
    /**
     * 当前是否存在交换空间预警
     */
    @Getter
    private final AtomicBoolean swapSpaceAlert = new AtomicBoolean(false);
    
    /**
     * 当前是否存在缺页率预警
     */
    @Getter
    private final AtomicBoolean pageFaultRateAlert = new AtomicBoolean(false);
    
    /**
     * 当前是否存在内存泄漏预警
     */
    @Getter
    private final AtomicBoolean memoryLeakAlert = new AtomicBoolean(false);
    
    /**
     * 当前是否存在碎片化预警
     */
    @Getter
    private final AtomicBoolean fragmentationAlert = new AtomicBoolean(false);
    
    /**
     * 历史内存使用记录
     */
    private final List<MemoryUsageRecord> usageHistory = new ArrayList<>();
    
    /**
     * 历史记录最大长度
     */
    private static final int MAX_HISTORY_SIZE = 100;
    
    /**
     * 连续增长计数（用于内存泄漏检测）
     */
    private int consecutiveGrowthCount = 0;
    
    /**
     * JVM内存管理Bean
     */
    private final MemoryMXBean memoryMXBean;
    
    /**
     * 构造函数
     * @param memoryManager 内存管理器
     * @param physicalMemory 物理内存
     * @param pageFrameTable 页帧表
     * @param swapManager 交换管理器
     * @param memoryStats 内存统计
     * @param pageFaultStats 缺页统计
     * @param tlbStats TLB统计
     */
    @Autowired
    public MemoryUsageMonitor(MemoryManager memoryManager,
                              PhysicalMemory physicalMemory,
                              PageFrameTable pageFrameTable,
                              SwapManager swapManager,
                              MemoryStats memoryStats,
                              PageFaultStats pageFaultStats,
                              TLBStats tlbStats) {
        this.memoryManager = memoryManager;
        this.physicalMemory = physicalMemory;
        this.pageFrameTable = pageFrameTable;
        this.swapManager = swapManager;
        this.memoryStats = memoryStats;
        this.pageFaultStats = pageFaultStats;
        this.tlbStats = tlbStats;
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        
        log.info("内存使用监控器初始化完成");
    }
    
    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        log.info("内存使用监控器启动，监控状态={}, 预警状态={}", monitoringEnabled, alertsEnabled);
        log.info("物理内存阈值={}%, 交换空间阈值={}%, 缺页率阈值={}/秒, 内存泄漏阈值={}, 碎片化阈值={}",
                physicalMemoryThreshold, swapSpaceThreshold, pageFaultRateThreshold,
                memoryLeakThreshold, fragmentationThreshold);
        
        // 初始收集一次数据
        collectMemoryStats();
    }
    
    /**
     * 定期收集内存使用统计数据
     */
    @Scheduled(fixedDelayString = "${memory.monitor.interval:60000}")
    public void collectMemoryStats() {
        if (!monitoringEnabled) {
            return;
        }
        
        try {
            // 收集JVM内存使用情况
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
            
            // 收集物理内存使用情况
            long totalPhysicalMemory = physicalMemory.getTotalSize();
            long usedPhysicalMemory = physicalMemory.getUsedSize();
            long freePhysicalMemory = physicalMemory.getFreeSize();
            
            // 收集交换空间使用情况
            long totalSwapSpace = swapManager.getSwapFileSize();
            // 估算使用情况
            long usedSwapEntries = swapManager.getSwapOutCount() - swapManager.getSwapInCount();
            if (usedSwapEntries < 0) usedSwapEntries = 0;
            int pageSize = 4096; // 页面大小 4KB
            long usedSwapSpace = usedSwapEntries * pageSize;
            long freeSwapSpace = totalSwapSpace - usedSwapSpace;
            
            // 收集页帧使用情况
            int totalPageFrames = pageFrameTable.getTotalFrames();
            int usedPageFrames = pageFrameTable.getUsedFrames();
            int freePageFrames = pageFrameTable.getFreeFrames();
            
            // 更新内存统计对象
            memoryStats.setTotalPhysicalMemory(totalPhysicalMemory);
            memoryStats.setUsedPhysicalMemory(usedPhysicalMemory);
            memoryStats.setFreePhysicalMemory(freePhysicalMemory);
            memoryStats.setTotalSwapSpace(totalSwapSpace);
            memoryStats.setUsedSwapSpace(usedSwapSpace);
            memoryStats.setFreeSwapSpace(freeSwapSpace);
            memoryStats.setPageFramesCount(totalPageFrames);
            memoryStats.setUsedPageFramesCount(usedPageFrames);
            memoryStats.setFreePageFramesCount(freePageFrames);
            
            // 计算碎片化指数
            double fragmentationIndex = memoryManager.calculateFragmentationIndex();
            memoryStats.setFragmentationIndex(fragmentationIndex);
            
            // 记录当前使用情况
            MemoryUsageRecord record = new MemoryUsageRecord(
                    System.currentTimeMillis(),
                    heapUsed + nonHeapUsed,
                    usedPhysicalMemory,
                    usedSwapSpace,
                    pageFaultStats.getPageFaultRate(),
                    fragmentationIndex
            );
            
            // 添加到历史记录
            addToHistory(record);
            
            // 检查预警情况
            if (alertsEnabled) {
                checkAlerts(record);
            }
            
            // 输出日志
            if (log.isDebugEnabled()) {
                log.debug("内存使用统计: 物理内存使用率={}%, 交换空间使用率={}%, 缺页率={}/秒, 碎片化指数={}",
                        String.format("%.2f", memoryStats.getPhysicalMemoryUsageRatio() * 100),
                        String.format("%.2f", memoryStats.getSwapUsageRatio() * 100),
                        String.format("%.2f", pageFaultStats.getPageFaultRate()),
                        String.format("%.2f", fragmentationIndex));
            }
        } catch (Exception e) {
            log.error("收集内存使用统计数据时发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 添加使用记录到历史列表
     * @param record 内存使用记录
     */
    private void addToHistory(MemoryUsageRecord record) {
        synchronized (usageHistory) {
            usageHistory.add(record);
            if (usageHistory.size() > MAX_HISTORY_SIZE) {
                usageHistory.remove(0);
            }
        }
    }
    
    /**
     * 检查各项预警情况
     * @param record 当前内存使用记录
     */
    private void checkAlerts(MemoryUsageRecord record) {
        // 检查物理内存使用率
        double physicalMemoryUsageRatio = memoryStats.getPhysicalMemoryUsageRatio() * 100;
        if (physicalMemoryUsageRatio >= physicalMemoryThreshold) {
            if (physicalMemoryAlert.compareAndSet(false, true)) {
                log.warn("物理内存使用率超过阈值: 当前={}%, 阈值={}%",
                        String.format("%.2f", physicalMemoryUsageRatio),
                        physicalMemoryThreshold);
            }
        } else {
            physicalMemoryAlert.set(false);
        }
        
        // 检查交换空间使用率
        double swapUsageRatio = memoryStats.getSwapUsageRatio() * 100;
        if (swapUsageRatio >= swapSpaceThreshold) {
            if (swapSpaceAlert.compareAndSet(false, true)) {
                log.warn("交换空间使用率超过阈值: 当前={}%, 阈值={}%",
                        String.format("%.2f", swapUsageRatio),
                        swapSpaceThreshold);
            }
        } else {
            swapSpaceAlert.set(false);
        }
        
        // 检查缺页率
        double pageFaultRate = pageFaultStats.getPageFaultRate();
        if (pageFaultRate >= pageFaultRateThreshold) {
            if (pageFaultRateAlert.compareAndSet(false, true)) {
                log.warn("缺页率超过阈值: 当前={}/秒, 阈值={}/秒",
                        String.format("%.2f", pageFaultRate),
                        pageFaultRateThreshold);
            }
        } else {
            pageFaultRateAlert.set(false);
        }
        
        // 检查碎片化情况
        double fragmentationIndex = memoryStats.getFragmentationIndex();
        if (fragmentationIndex >= fragmentationThreshold) {
            if (fragmentationAlert.compareAndSet(false, true)) {
                log.warn("内存碎片化超过阈值: 当前={}, 阈值={}",
                        String.format("%.2f", fragmentationIndex),
                        String.format("%.2f", fragmentationThreshold));
            }
        } else {
            fragmentationAlert.set(false);
        }
        
        // 检查内存泄漏（连续内存增长）
        checkMemoryLeak(record);
    }
    
    /**
     * 检查是否存在内存泄漏
     * @param currentRecord 当前内存使用记录
     */
    private void checkMemoryLeak(MemoryUsageRecord currentRecord) {
        if (usageHistory.size() < 2) {
            return;
        }
        
        MemoryUsageRecord previousRecord = usageHistory.get(usageHistory.size() - 2);
        
        // 检查物理内存是否持续增长
        if (currentRecord.getUsedPhysicalMemory() > previousRecord.getUsedPhysicalMemory()) {
            consecutiveGrowthCount++;
            
            if (consecutiveGrowthCount >= memoryLeakThreshold) {
                if (memoryLeakAlert.compareAndSet(false, true)) {
                    log.warn("检测到可能的内存泄漏: 内存使用量连续{}次增长", consecutiveGrowthCount);
                }
            }
        } else {
            consecutiveGrowthCount = 0;
            memoryLeakAlert.set(false);
        }
    }
    
    /**
     * 获取内存使用历史记录
     * @return 内存使用历史记录列表的副本
     */
    public List<MemoryUsageRecord> getUsageHistory() {
        synchronized (usageHistory) {
            return new ArrayList<>(usageHistory);
        }
    }
    
    /**
     * 获取内存使用摘要信息
     * @return 内存使用摘要字符串
     */
    public String getMemorySummary() {
        StringBuilder sb = new StringBuilder("内存使用摘要:\n");
        
        // 添加当前状态
        sb.append("监控状态: ").append(monitoringEnabled ? "启用" : "禁用")
          .append(", 预警状态: ").append(alertsEnabled ? "启用" : "禁用")
          .append("\n");
        
        // 添加预警情况
        sb.append("当前预警: ");
        if (physicalMemoryAlert.get() || swapSpaceAlert.get() || pageFaultRateAlert.get() || 
                memoryLeakAlert.get() || fragmentationAlert.get()) {
            if (physicalMemoryAlert.get()) sb.append("[物理内存] ");
            if (swapSpaceAlert.get()) sb.append("[交换空间] ");
            if (pageFaultRateAlert.get()) sb.append("[缺页率] ");
            if (memoryLeakAlert.get()) sb.append("[内存泄漏] ");
            if (fragmentationAlert.get()) sb.append("[内存碎片] ");
        } else {
            sb.append("无");
        }
        sb.append("\n\n");
        
        // 添加内存统计摘要
        sb.append(memoryStats.getSummary()).append("\n");
        
        // 添加缺页统计摘要
        sb.append(pageFaultStats.getSummary()).append("\n");
        
        // 添加TLB统计摘要
        sb.append(tlbStats.getSummary());
        
        return sb.toString();
    }
    
    /**
     * 重置所有监控统计数据
     */
    public void resetStats() {
        memoryStats.reset();
        pageFaultStats.reset();
        tlbStats.reset();
        
        synchronized (usageHistory) {
            usageHistory.clear();
        }
        
        consecutiveGrowthCount = 0;
        physicalMemoryAlert.set(false);
        swapSpaceAlert.set(false);
        pageFaultRateAlert.set(false);
        memoryLeakAlert.set(false);
        fragmentationAlert.set(false);
        
        log.info("内存使用监控统计数据已重置");
    }
    
    /**
     * 设置内存使用告警阈值
     * @param threshold 告警阈值（百分比）
     */
    public void setMemoryUsageAlertThreshold(int threshold) {
        log.info("设置内存使用告警阈值: {}%", threshold);
        if (threshold < 0 || threshold > 100) {
            log.warn("无效的阈值: {}，应在0-100之间。使用默认值: {}", threshold, physicalMemoryThreshold);
            return;
        }
        this.physicalMemoryThreshold = threshold;
        log.info("内存使用告警阈值已设置为: {}%", threshold);
    }
    
    /**
     * 获取交换空间使用情况
     * @return 交换空间使用信息
     */
    private String getSwapUsageInfo() {
        // 使用 SwapManager 类中已有的字段
        long totalSize = swapManager.getSwapFileSize();
        // 估算使用情况，因为SwapManager类已经不提供这些方法
        long usedSwapEntries = swapManager.getSwapOutCount() - swapManager.getSwapInCount();
        if (usedSwapEntries < 0) usedSwapEntries = 0;
        
        int pageSize = 4096; // 页面大小 4KB
        long usedSize = usedSwapEntries * pageSize;
        long freeSize = totalSize - usedSize;
        
        return String.format("交换空间使用情况: 总大小=%dMB, 已用=%dMB, 空闲=%dMB, 换出=%d次, 换入=%d次",
                totalSize / (1024 * 1024),
                usedSize / (1024 * 1024),
                freeSize / (1024 * 1024),
                swapManager.getSwapOutCount(),
                swapManager.getSwapInCount());
    }
    
    /**
     * 内存使用记录
     */
    @Getter
    public static class MemoryUsageRecord {
        /**
         * 记录时间戳
         */
        private final long timestamp;
        
        /**
         * JVM内存使用量（字节）
         */
        private final long jvmMemoryUsed;
        
        /**
         * 物理内存使用量（字节）
         */
        private final long usedPhysicalMemory;
        
        /**
         * 交换空间使用量（字节）
         */
        private final long usedSwapSpace;
        
        /**
         * 缺页率（每秒）
         */
        private final double pageFaultRate;
        
        /**
         * 碎片化指数
         */
        private final double fragmentationIndex;
        
        /**
         * 构造函数
         * @param timestamp 时间戳
         * @param jvmMemoryUsed JVM内存使用量
         * @param usedPhysicalMemory 物理内存使用量
         * @param usedSwapSpace 交换空间使用量
         * @param pageFaultRate 缺页率
         * @param fragmentationIndex 碎片化指数
         */
        public MemoryUsageRecord(long timestamp, long jvmMemoryUsed, long usedPhysicalMemory,
                                 long usedSwapSpace, double pageFaultRate, double fragmentationIndex) {
            this.timestamp = timestamp;
            this.jvmMemoryUsed = jvmMemoryUsed;
            this.usedPhysicalMemory = usedPhysicalMemory;
            this.usedSwapSpace = usedSwapSpace;
            this.pageFaultRate = pageFaultRate;
            this.fragmentationIndex = fragmentationIndex;
        }
    }
} 