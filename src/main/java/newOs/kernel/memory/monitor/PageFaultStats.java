package newOs.kernel.memory.monitor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缺页统计类
 * 用于收集和分析与缺页相关的性能统计信息
 */
@Component
@Slf4j
public class PageFaultStats {

    /**
     * 总缺页次数
     */
    @Getter
    private final AtomicLong totalPageFaults = new AtomicLong(0);

    /**
     * 主缺页次数（需要从磁盘加载）
     */
    @Getter
    private final AtomicLong majorPageFaults = new AtomicLong(0);

    /**
     * 次缺页次数（在内存中但未映射）
     */
    @Getter
    private final AtomicLong minorPageFaults = new AtomicLong(0);

    /**
     * 缺页处理总时间（纳秒）
     */
    @Getter
    private final AtomicLong totalHandlingTimeNanos = new AtomicLong(0);

    /**
     * 主缺页处理总时间（纳秒）
     */
    @Getter
    private final AtomicLong majorFaultHandlingTimeNanos = new AtomicLong(0);

    /**
     * 次缺页处理总时间（纳秒）
     */
    @Getter
    private final AtomicLong minorFaultHandlingTimeNanos = new AtomicLong(0);

    /**
     * 页面调入次数（从磁盘加载到内存）
     */
    @Getter
    private final AtomicLong pageInsCount = new AtomicLong(0);

    /**
     * 页面调出次数（从内存写回到磁盘）
     */
    @Getter
    private final AtomicLong pageOutsCount = new AtomicLong(0);

    /**
     * 复制写入操作次数
     */
    @Getter
    private final AtomicLong copyOnWriteCount = new AtomicLong(0);

    /**
     * 栈增长缺页次数
     */
    @Getter
    private final AtomicLong stackGrowthCount = new AtomicLong(0);

    /**
     * 缺页统计开始时间
     */
    @Getter
    private long startTimeMillis = System.currentTimeMillis();

    /**
     * 上次缺页发生的时间戳
     */
    @Getter
    private volatile long lastPageFaultTimestamp = 0;

    /**
     * 每秒缺页率（动态计算）
     */
    @Getter
    private volatile double pageFaultRate = 0.0;

    /**
     * 统计窗口时间（毫秒）
     */
    private static final long RATE_WINDOW_MILLIS = 10000; // 10秒窗口

    /**
     * 窗口内的缺页计数
     */
    private final AtomicLong windowPageFaults = new AtomicLong(0);

    /**
     * 窗口开始时间
     */
    private volatile long windowStartTime = System.currentTimeMillis();

    /**
     * 进程缺页统计映射表
     */
    private final Map<Integer, ProcessPageFaultStat> processStats = new HashMap<>();

    /**
     * 构造函数
     */
    public PageFaultStats() {
        log.info("缺页统计组件初始化");
    }

    /**
     * 记录缺页故障
     * @param isMinor 是否为次缺页
     * @param handlingTimeNanos 处理时间（纳秒）
     */
    public void recordPageFault(boolean isMinor, long handlingTimeNanos) {
        totalPageFaults.incrementAndGet();
        totalHandlingTimeNanos.addAndGet(handlingTimeNanos);
        
        if (isMinor) {
            minorPageFaults.incrementAndGet();
            minorFaultHandlingTimeNanos.addAndGet(handlingTimeNanos);
        } else {
            majorPageFaults.incrementAndGet();
            majorFaultHandlingTimeNanos.addAndGet(handlingTimeNanos);
        }
        
        lastPageFaultTimestamp = System.currentTimeMillis();
        updatePageFaultRate();
    }

    /**
     * 记录主缺页（需要从磁盘加载）
     * @param handlingTimeNanos 处理时间（纳秒）
     */
    public void recordMajorPageFault(long handlingTimeNanos) {
        recordPageFault(false, handlingTimeNanos);
        pageInsCount.incrementAndGet();
    }

    /**
     * 记录次缺页（在内存中但未映射）
     * @param handlingTimeNanos 处理时间（纳秒）
     */
    public void recordMinorPageFault(long handlingTimeNanos) {
        recordPageFault(true, handlingTimeNanos);
    }

    /**
     * 记录页面调出操作
     */
    public void recordPageOut() {
        pageOutsCount.incrementAndGet();
    }

    /**
     * 记录复制写入操作
     * @param handlingTimeNanos 处理时间（纳秒）
     */
    public void recordCopyOnWrite(long handlingTimeNanos) {
        copyOnWriteCount.incrementAndGet();
        recordMinorPageFault(handlingTimeNanos);
    }

    /**
     * 记录栈增长操作
     * @param handlingTimeNanos 处理时间（纳秒）
     */
    public void recordStackGrowth(long handlingTimeNanos) {
        stackGrowthCount.incrementAndGet();
        recordMinorPageFault(handlingTimeNanos);
    }

    /**
     * 记录进程缺页
     * @param processId 进程ID
     * @param isMinor 是否为次缺页
     */
    public void recordProcessPageFault(int processId, boolean isMinor) {
        synchronized (processStats) {
            ProcessPageFaultStat stat = processStats.computeIfAbsent(
                    processId, pid -> new ProcessPageFaultStat(pid));
            
            if (isMinor) {
                stat.incrementMinorFaults();
            } else {
                stat.incrementMajorFaults();
            }
        }
    }

    /**
     * 更新缺页率（每秒）
     */
    private void updatePageFaultRate() {
        long currentTime = System.currentTimeMillis();
        windowPageFaults.incrementAndGet();
        
        // 检查是否需要计算新的速率（每100ms检查一次）
        if (currentTime - windowStartTime >= 100) {
            synchronized (this) {
                currentTime = System.currentTimeMillis();
                long windowDuration = currentTime - windowStartTime;
                
                // 如果窗口时间超过设定值，重新计算缺页率
                if (windowDuration >= RATE_WINDOW_MILLIS) {
                    long faults = windowPageFaults.getAndSet(0);
                    // 计算每秒缺页率
                    pageFaultRate = (double) faults / (windowDuration / 1000.0);
                    // 重置窗口开始时间
                    windowStartTime = currentTime;
                    
                    if (log.isDebugEnabled()) {
                        log.debug("缺页率更新: {}/秒 (窗口: {}秒, 缺页数: {})", 
                                String.format("%.2f", pageFaultRate),
                                String.format("%.1f", windowDuration / 1000.0),
                                faults);
                    }
                }
            }
        }
    }

    /**
     * 获取平均缺页处理时间（毫秒）
     * @return 平均处理时间
     */
    public double getAverageHandlingTimeMs() {
        long total = totalPageFaults.get();
        return total > 0 ? 
                TimeUnit.NANOSECONDS.toMicros(totalHandlingTimeNanos.get()) / (double) total / 1000.0 : 0;
    }

    /**
     * 获取平均主缺页处理时间（毫秒）
     * @return 平均主缺页处理时间
     */
    public double getAverageMajorFaultHandlingTimeMs() {
        long major = majorPageFaults.get();
        return major > 0 ? 
                TimeUnit.NANOSECONDS.toMicros(majorFaultHandlingTimeNanos.get()) / (double) major / 1000.0 : 0;
    }

    /**
     * 获取平均次缺页处理时间（毫秒）
     * @return 平均次缺页处理时间
     */
    public double getAverageMinorFaultHandlingTimeMs() {
        long minor = minorPageFaults.get();
        return minor > 0 ? 
                TimeUnit.NANOSECONDS.toMicros(minorFaultHandlingTimeNanos.get()) / (double) minor / 1000.0 : 0;
    }

    /**
     * 获取主缺页与总缺页的比率
     * @return 主缺页比率
     */
    public double getMajorFaultRatio() {
        long total = totalPageFaults.get();
        return total > 0 ? (double) majorPageFaults.get() / total : 0;
    }

    /**
     * 获取次缺页与总缺页的比率
     * @return 次缺页比率
     */
    public double getMinorFaultRatio() {
        long total = totalPageFaults.get();
        return total > 0 ? (double) minorPageFaults.get() / total : 0;
    }

    /**
     * 获取页面调出与调入的比率
     * @return 页面调出/调入比率
     */
    public double getPageOutInRatio() {
        long ins = pageInsCount.get();
        return ins > 0 ? (double) pageOutsCount.get() / ins : 0;
    }

    /**
     * 获取运行时间（秒）
     * @return 运行时间
     */
    public long getUpTimeSeconds() {
        return (System.currentTimeMillis() - startTimeMillis) / 1000;
    }

    /**
     * 获取进程缺页统计列表
     * @return 进程缺页统计列表
     */
    public List<ProcessPageFaultStat> getProcessStats() {
        synchronized (processStats) {
            return new ArrayList<>(processStats.values());
        }
    }

    /**
     * 获取指定进程的缺页统计
     * @param processId 进程ID
     * @return 进程缺页统计对象
     */
    public ProcessPageFaultStat getProcessStat(int processId) {
        synchronized (processStats) {
            return processStats.get(processId);
        }
    }

    /**
     * 获取缺页统计摘要
     * @return 统计摘要字符串
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder("缺页统计摘要:\n");
        
        // 添加基本统计
        long upTime = getUpTimeSeconds();
        sb.append("运行时间: ").append(formatDuration(upTime)).append("\n");
        sb.append("总缺页数: ").append(totalPageFaults.get())
          .append(" (").append(String.format("%.2f", pageFaultRate)).append("/秒)\n");
        
        // 主缺页和次缺页统计
        sb.append("主缺页: ").append(majorPageFaults.get())
          .append(" (").append(String.format("%.1f%%", getMajorFaultRatio() * 100)).append(")");
        sb.append(", 次缺页: ").append(minorPageFaults.get())
          .append(" (").append(String.format("%.1f%%", getMinorFaultRatio() * 100)).append(")\n");
        
        // 处理时间统计
        sb.append("平均处理时间: ").append(String.format("%.3fms", getAverageHandlingTimeMs())).append(" (");
        sb.append("主缺页: ").append(String.format("%.3fms", getAverageMajorFaultHandlingTimeMs())).append(", ");
        sb.append("次缺页: ").append(String.format("%.3fms", getAverageMinorFaultHandlingTimeMs())).append(")\n");
        
        // 特殊操作统计
        sb.append("页面调入/调出: ").append(pageInsCount.get()).append("/").append(pageOutsCount.get());
        sb.append(" (比率: ").append(String.format("%.2f", getPageOutInRatio())).append(")\n");
        sb.append("写时复制: ").append(copyOnWriteCount.get());
        sb.append(", 栈增长: ").append(stackGrowthCount.get()).append("\n");
        
        // 进程统计
        int processCount;
        synchronized (processStats) {
            processCount = processStats.size();
        }
        sb.append("进程缺页统计: ").append(processCount).append("个进程\n");
        
        return sb.toString();
    }

    /**
     * 重置所有统计数据
     */
    public void reset() {
        totalPageFaults.set(0);
        majorPageFaults.set(0);
        minorPageFaults.set(0);
        totalHandlingTimeNanos.set(0);
        majorFaultHandlingTimeNanos.set(0);
        minorFaultHandlingTimeNanos.set(0);
        pageInsCount.set(0);
        pageOutsCount.set(0);
        copyOnWriteCount.set(0);
        stackGrowthCount.set(0);
        
        windowPageFaults.set(0);
        pageFaultRate = 0.0;
        startTimeMillis = System.currentTimeMillis();
        windowStartTime = startTimeMillis;
        lastPageFaultTimestamp = 0;
        
        synchronized (processStats) {
            processStats.clear();
        }
        
        log.info("缺页统计数据已重置");
    }

    /**
     * 格式化时间持续时间
     * @param seconds 秒数
     * @return 格式化后的时间字符串
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return String.format("%d分%d秒", seconds / 60, seconds % 60);
        } else {
            return String.format("%d时%d分%d秒", 
                    seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        }
    }

    /**
     * 进程级别的缺页统计
     */
    @Getter
    public static class ProcessPageFaultStat {
        /**
         * 进程ID
         */
        private final int processId;
        
        /**
         * 总缺页次数
         */
        private final AtomicLong totalFaults = new AtomicLong(0);
        
        /**
         * 主缺页次数
         */
        private final AtomicLong majorFaults = new AtomicLong(0);
        
        /**
         * 次缺页次数
         */
        private final AtomicLong minorFaults = new AtomicLong(0);
        
        /**
         * 创建时间
         */
        private final long createdTime = System.currentTimeMillis();
        
        /**
         * 构造函数
         * @param processId 进程ID
         */
        public ProcessPageFaultStat(int processId) {
            this.processId = processId;
        }
        
        /**
         * 增加主缺页计数
         */
        public void incrementMajorFaults() {
            majorFaults.incrementAndGet();
            totalFaults.incrementAndGet();
        }
        
        /**
         * 增加次缺页计数
         */
        public void incrementMinorFaults() {
            minorFaults.incrementAndGet();
            totalFaults.incrementAndGet();
        }
        
        /**
         * 获取主缺页比率
         * @return 主缺页比率
         */
        public double getMajorFaultRatio() {
            long total = totalFaults.get();
            return total > 0 ? (double) majorFaults.get() / total : 0;
        }
        
        /**
         * 获取次缺页比率
         * @return 次缺页比率
         */
        public double getMinorFaultRatio() {
            long total = totalFaults.get();
            return total > 0 ? (double) minorFaults.get() / total : 0;
        }
        
        /**
         * 获取存在时间（秒）
         * @return 存在时间
         */
        public long getAgeSeconds() {
            return (System.currentTimeMillis() - createdTime) / 1000;
        }
    }
} 