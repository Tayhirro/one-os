package newOs.kernel.memory.monitor;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存统计类
 * 用于收集和聚合系统内存使用统计信息
 */
@Component
@Slf4j
public class MemoryStats {
    
    /**
     * 物理内存总量（字节）
     */
    @Getter
    @Setter
    private long totalPhysicalMemory = 0;
    
    /**
     * 已用物理内存（字节）
     */
    @Getter
    @Setter
    private long usedPhysicalMemory = 0;
    
    /**
     * 空闲物理内存（字节）
     */
    @Getter
    @Setter
    private long freePhysicalMemory = 0;
    
    /**
     * 交换空间总量（字节）
     */
    @Getter
    @Setter
    private long totalSwapSpace = 0;
    
    /**
     * 已用交换空间（字节）
     */
    @Getter
    @Setter
    private long usedSwapSpace = 0;
    
    /**
     * 空闲交换空间（字节）
     */
    @Getter
    @Setter
    private long freeSwapSpace = 0;
    
    /**
     * 页帧总数
     */
    @Getter
    @Setter
    private int pageFramesCount = 0;
    
    /**
     * 已用页帧数
     */
    @Getter
    @Setter
    private int usedPageFramesCount = 0;
    
    /**
     * 空闲页帧数
     */
    @Getter
    @Setter
    private int freePageFramesCount = 0;
    
    /**
     * 内存分配请求总数
     */
    @Getter
    private final AtomicLong allocationRequests = new AtomicLong(0);
    
    /**
     * 内存释放请求总数
     */
    @Getter
    private final AtomicLong freeRequests = new AtomicLong(0);
    
    /**
     * 内存分配失败次数
     */
    @Getter
    private final AtomicLong allocationFailures = new AtomicLong(0);
    
    /**
     * 大块内存分配次数（大于1页）
     */
    @Getter
    private final AtomicLong largeAllocations = new AtomicLong(0);
    
    /**
     * 小块内存分配次数（小于等于1页）
     */
    @Getter
    private final AtomicLong smallAllocations = new AtomicLong(0);
    
    /**
     * 当前分配的内存块总数
     */
    @Getter
    private final AtomicLong allocatedBlocksCount = new AtomicLong(0);
    
    /**
     * 碎片化指数（0-1之间，越接近1表示碎片化程度越高）
     */
    @Getter
    @Setter
    private double fragmentationIndex = 0.0;
    
    /**
     * 内存分配的总字节数
     */
    @Getter
    private final AtomicLong totalBytesAllocated = new AtomicLong(0);
    
    /**
     * 内存释放的总字节数
     */
    @Getter
    private final AtomicLong totalBytesFreed = new AtomicLong(0);
    
    /**
     * 进程内存统计映射表
     */
    private final Map<Integer, ProcessMemoryStat> processStats = new HashMap<>();
    
    /**
     * 页面置换次数
     */
    @Getter
    private final AtomicLong pageReplacements = new AtomicLong(0);
    
    /**
     * 统计开始时间
     */
    @Getter
    private long startTimeMillis = System.currentTimeMillis();
    
    /**
     * 构造函数
     */
    public MemoryStats() {
        log.info("内存统计组件初始化");
    }
    
    /**
     * 记录内存分配请求
     * @param bytes 分配的字节数
     * @param isSuccess 是否成功
     */
    public void recordAllocationRequest(long bytes, boolean isSuccess) {
        allocationRequests.incrementAndGet();
        
        if (isSuccess) {
            totalBytesAllocated.addAndGet(bytes);
            allocatedBlocksCount.incrementAndGet();
            
            if (bytes > 4096) { // 假设一页为4KB
                largeAllocations.incrementAndGet();
            } else {
                smallAllocations.incrementAndGet();
            }
        } else {
            allocationFailures.incrementAndGet();
        }
    }
    
    /**
     * 记录内存释放请求
     * @param bytes 释放的字节数
     */
    public void recordFreeRequest(long bytes) {
        freeRequests.incrementAndGet();
        totalBytesFreed.addAndGet(bytes);
        allocatedBlocksCount.decrementAndGet();
    }
    
    /**
     * 记录页面置换
     */
    public void recordPageReplacement() {
        pageReplacements.incrementAndGet();
    }
    
    /**
     * 记录进程内存分配
     * @param processId 进程ID
     * @param bytes 分配的字节数
     */
    public void recordProcessAllocation(int processId, long bytes) {
        synchronized (processStats) {
            ProcessMemoryStat stat = processStats.computeIfAbsent(
                    processId, pid -> new ProcessMemoryStat(pid));
            
            stat.recordAllocation(bytes);
        }
    }
    
    /**
     * 记录进程内存释放
     * @param processId 进程ID
     * @param bytes 释放的字节数
     */
    public void recordProcessFree(int processId, long bytes) {
        synchronized (processStats) {
            ProcessMemoryStat stat = processStats.get(processId);
            if (stat != null) {
                stat.recordFree(bytes);
            }
        }
    }
    
    /**
     * 获取物理内存使用率
     * @return 物理内存使用率（0-1之间）
     */
    public double getPhysicalMemoryUsageRatio() {
        return totalPhysicalMemory > 0 ? (double) usedPhysicalMemory / totalPhysicalMemory : 0;
    }
    
    /**
     * 获取交换空间使用率
     * @return 交换空间使用率（0-1之间）
     */
    public double getSwapUsageRatio() {
        return totalSwapSpace > 0 ? (double) usedSwapSpace / totalSwapSpace : 0;
    }
    
    /**
     * 获取页帧使用率
     * @return 页帧使用率（0-1之间）
     */
    public double getPageFrameUsageRatio() {
        return pageFramesCount > 0 ? (double) usedPageFramesCount / pageFramesCount : 0;
    }
    
    /**
     * 获取内存分配失败率
     * @return 内存分配失败率（0-1之间）
     */
    public double getAllocationFailureRatio() {
        long total = allocationRequests.get();
        return total > 0 ? (double) allocationFailures.get() / total : 0;
    }
    
    /**
     * 获取进程内存统计列表
     * @return 进程内存统计列表
     */
    public List<ProcessMemoryStat> getProcessStats() {
        synchronized (processStats) {
            return new ArrayList<>(processStats.values());
        }
    }
    
    /**
     * 获取指定进程的内存统计
     * @param processId 进程ID
     * @return 进程内存统计对象
     */
    public ProcessMemoryStat getProcessStat(int processId) {
        synchronized (processStats) {
            return processStats.get(processId);
        }
    }
    
    /**
     * 获取运行时间（秒）
     * @return 运行时间
     */
    public long getUpTimeSeconds() {
        return (System.currentTimeMillis() - startTimeMillis) / 1000;
    }
    
    /**
     * 获取内存统计摘要
     * @return 统计摘要字符串
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder("内存统计摘要:\n");
        
        // 添加物理内存统计
        sb.append("物理内存: ").append(formatBytes(usedPhysicalMemory))
          .append("/").append(formatBytes(totalPhysicalMemory))
          .append(" (").append(String.format("%.1f%%", getPhysicalMemoryUsageRatio() * 100))
          .append(")\n");
        
        // 添加交换空间统计
        sb.append("交换空间: ").append(formatBytes(usedSwapSpace))
          .append("/").append(formatBytes(totalSwapSpace))
          .append(" (").append(String.format("%.1f%%", getSwapUsageRatio() * 100))
          .append(")\n");
        
        // 添加页帧统计
        sb.append("页帧: ").append(usedPageFramesCount)
          .append("/").append(pageFramesCount)
          .append(" (").append(String.format("%.1f%%", getPageFrameUsageRatio() * 100))
          .append(")\n");
        
        // 添加分配统计
        sb.append("内存分配: 总请求=").append(allocationRequests.get());
        
        long failures = allocationFailures.get();
        if (failures > 0) {
            sb.append(", 失败=").append(failures)
              .append(" (").append(String.format("%.2f%%", getAllocationFailureRatio() * 100))
              .append(")");
        }
        sb.append("\n");
        
        // 添加内存块统计
        sb.append("当前分配块: ").append(allocatedBlocksCount.get())
          .append(" (大块=").append(largeAllocations.get())
          .append(", 小块=").append(smallAllocations.get())
          .append(")\n");
        
        // 添加流量统计
        sb.append("内存流量: 已分配=").append(formatBytes(totalBytesAllocated.get()))
          .append(", 已释放=").append(formatBytes(totalBytesFreed.get()))
          .append("\n");
        
        // 添加碎片化统计
        sb.append("碎片化指数: ").append(String.format("%.3f", fragmentationIndex))
          .append("\n");
        
        // 添加进程统计
        int processCount;
        synchronized (processStats) {
            processCount = processStats.size();
        }
        sb.append("进程内存统计: ").append(processCount).append("个进程\n");
        
        return sb.toString();
    }
    
    /**
     * 将字节数格式化为易读的字符串
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2fGB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 重置所有统计数据
     */
    public void reset() {
        // 重置内存使用数据
        usedPhysicalMemory = 0;
        freePhysicalMemory = 0;
        usedSwapSpace = 0;
        freeSwapSpace = 0;
        usedPageFramesCount = 0;
        freePageFramesCount = 0;
        fragmentationIndex = 0.0;
        
        // 重置计数器
        allocationRequests.set(0);
        freeRequests.set(0);
        allocationFailures.set(0);
        largeAllocations.set(0);
        smallAllocations.set(0);
        allocatedBlocksCount.set(0);
        totalBytesAllocated.set(0);
        totalBytesFreed.set(0);
        pageReplacements.set(0);
        
        // 重置时间戳
        startTimeMillis = System.currentTimeMillis();
        
        // 清除进程统计
        synchronized (processStats) {
            processStats.clear();
        }
        
        log.info("内存统计数据已重置");
    }
    
    /**
     * 进程级别的内存统计
     */
    @Getter
    public static class ProcessMemoryStat {
        /**
         * 进程ID
         */
        private final int processId;
        
        /**
         * 当前分配的内存（字节）
         */
        private final AtomicLong currentMemoryUsage = new AtomicLong(0);
        
        /**
         * 峰值内存使用量（字节）
         */
        private final AtomicLong peakMemoryUsage = new AtomicLong(0);
        
        /**
         * 总分配内存（字节）
         */
        private final AtomicLong totalAllocated = new AtomicLong(0);
        
        /**
         * 总释放内存（字节）
         */
        private final AtomicLong totalFreed = new AtomicLong(0);
        
        /**
         * 分配次数
         */
        private final AtomicLong allocationCount = new AtomicLong(0);
        
        /**
         * 释放次数
         */
        private final AtomicLong freeCount = new AtomicLong(0);
        
        /**
         * 创建时间
         */
        private final long createdTime = System.currentTimeMillis();
        
        /**
         * 构造函数
         * @param processId 进程ID
         */
        public ProcessMemoryStat(int processId) {
            this.processId = processId;
        }
        
        /**
         * 记录内存分配
         * @param bytes 分配的字节数
         */
        public void recordAllocation(long bytes) {
            allocationCount.incrementAndGet();
            totalAllocated.addAndGet(bytes);
            
            long newUsage = currentMemoryUsage.addAndGet(bytes);
            updatePeakMemory(newUsage);
        }
        
        /**
         * 记录内存释放
         * @param bytes 释放的字节数
         */
        public void recordFree(long bytes) {
            freeCount.incrementAndGet();
            totalFreed.addAndGet(bytes);
            
            // 确保不会变成负数
            long currentUsage = currentMemoryUsage.get();
            if (bytes > currentUsage) {
                currentMemoryUsage.set(0);
            } else {
                currentMemoryUsage.addAndGet(-bytes);
            }
        }
        
        /**
         * 更新峰值内存
         * @param currentUsage 当前使用量
         */
        private void updatePeakMemory(long currentUsage) {
            long peak = peakMemoryUsage.get();
            while (currentUsage > peak) {
                if (peakMemoryUsage.compareAndSet(peak, currentUsage)) {
                    break;
                }
                peak = peakMemoryUsage.get();
            }
        }
        
        /**
         * 获取内存周转率（总分配/当前使用）
         * @return 内存周转率
         */
        public double getMemoryTurnoverRatio() {
            long current = currentMemoryUsage.get();
            return current > 0 ? (double) totalAllocated.get() / current : 0;
        }
        
        /**
         * 获取存在时间（秒）
         * @return 存在时间
         */
        public long getAgeSeconds() {
            return (System.currentTimeMillis() - createdTime) / 1000;
        }
    }

    /**
     * 获取内存碎片化指数
     * @return 碎片化指数，0到1之间的浮点数
     */
    public double getFragmentationIndex() {
        return fragmentationIndex;
    }
} 