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
 * TLB统计类
 * 用于收集和分析TLB缓存性能相关统计信息
 */
@Component
@Slf4j
public class TLBStats {

    /**
     * 总访问次数
     */
    @Getter
    private final AtomicLong totalAccesses = new AtomicLong(0);

    /**
     * 总命中次数
     */
    @Getter
    private final AtomicLong totalHits = new AtomicLong(0);

    /**
     * 总未命中次数
     */
    @Getter
    private final AtomicLong totalMisses = new AtomicLong(0);

    /**
     * L1 TLB访问次数
     */
    @Getter
    private final AtomicLong l1Accesses = new AtomicLong(0);

    /**
     * L1 TLB命中次数
     */
    @Getter
    private final AtomicLong l1Hits = new AtomicLong(0);

    /**
     * L1 TLB未命中次数
     */
    @Getter
    private final AtomicLong l1Misses = new AtomicLong(0);

    /**
     * L2 TLB访问次数
     */
    @Getter
    private final AtomicLong l2Accesses = new AtomicLong(0);

    /**
     * L2 TLB命中次数
     */
    @Getter
    private final AtomicLong l2Hits = new AtomicLong(0);

    /**
     * L2 TLB未命中次数
     */
    @Getter
    private final AtomicLong l2Misses = new AtomicLong(0);

    /**
     * TLB条目更新次数
     */
    @Getter
    private final AtomicLong entryUpdates = new AtomicLong(0);

    /**
     * TLB条目过期次数
     */
    @Getter
    private final AtomicLong entryExpirations = new AtomicLong(0);

    /**
     * TLB条目驱逐次数
     */
    @Getter
    private final AtomicLong entryEvictions = new AtomicLong(0);

    /**
     * TLB刷新次数
     */
    @Getter
    private final AtomicLong flushCount = new AtomicLong(0);

    /**
     * TLB部分刷新次数
     */
    @Getter
    private final AtomicLong partialFlushCount = new AtomicLong(0);

    /**
     * TLB统计开始时间
     */
    @Getter
    private long startTimeMillis = System.currentTimeMillis();

    /**
     * 上次访问时间戳
     */
    @Getter
    private volatile long lastAccessTimestamp = 0;

    /**
     * 每秒访问率（动态计算）
     */
    @Getter
    private volatile double accessRate = 0.0;

    /**
     * 统计窗口时间（毫秒）
     */
    private static final long RATE_WINDOW_MILLIS = 10000; // 10秒窗口

    /**
     * 窗口内的访问计数
     */
    private final AtomicLong windowAccesses = new AtomicLong(0);

    /**
     * 窗口开始时间
     */
    private volatile long windowStartTime = System.currentTimeMillis();

    /**
     * 进程TLB统计映射表
     */
    private final Map<Integer, ProcessTLBStat> processStats = new HashMap<>();

    /**
     * 构造函数
     */
    public TLBStats() {
        log.info("TLB统计组件初始化");
    }

    /**
     * 记录TLB访问
     * @param isL1 是否为L1 TLB
     */
    public void recordAccess(boolean isL1) {
        totalAccesses.incrementAndGet();
        
        if (isL1) {
            l1Accesses.incrementAndGet();
        } else {
            l2Accesses.incrementAndGet();
        }
        
        lastAccessTimestamp = System.currentTimeMillis();
        updateAccessRate();
    }

    /**
     * 记录TLB命中
     * @param isL1 是否为L1 TLB
     */
    public void recordHit(boolean isL1) {
        totalHits.incrementAndGet();
        
        if (isL1) {
            l1Hits.incrementAndGet();
        } else {
            l2Hits.incrementAndGet();
        }
    }

    /**
     * 记录TLB未命中
     * @param isL1 是否为L1 TLB
     */
    public void recordMiss(boolean isL1) {
        totalMisses.incrementAndGet();
        
        if (isL1) {
            l1Misses.incrementAndGet();
        } else {
            l2Misses.incrementAndGet();
        }
    }

    /**
     * 记录TLB条目更新
     */
    public void recordEntryUpdate() {
        entryUpdates.incrementAndGet();
    }

    /**
     * 记录TLB条目过期
     */
    public void recordEntryExpiration() {
        entryExpirations.incrementAndGet();
    }

    /**
     * 记录TLB条目驱逐
     */
    public void recordEntryEviction() {
        entryEvictions.incrementAndGet();
    }

    /**
     * 记录TLB完全刷新
     */
    public void recordFlush() {
        flushCount.incrementAndGet();
    }

    /**
     * 记录TLB部分刷新
     */
    public void recordPartialFlush() {
        partialFlushCount.incrementAndGet();
    }

    /**
     * 记录进程TLB访问
     * @param processId 进程ID
     * @param isHit 是否命中
     * @param isL1 是否为L1 TLB
     */
    public void recordProcessAccess(int processId, boolean isHit, boolean isL1) {
        synchronized (processStats) {
            ProcessTLBStat stat = processStats.computeIfAbsent(
                    processId, pid -> new ProcessTLBStat(pid));
            
            stat.incrementAccesses();
            
            if (isHit) {
                stat.incrementHits();
            } else {
                stat.incrementMisses();
            }
        }
    }

    /**
     * 更新访问率（每秒）
     */
    private void updateAccessRate() {
        long currentTime = System.currentTimeMillis();
        windowAccesses.incrementAndGet();
        
        // 检查是否需要计算新的速率（每100ms检查一次）
        if (currentTime - windowStartTime >= 100) {
            synchronized (this) {
                currentTime = System.currentTimeMillis();
                long windowDuration = currentTime - windowStartTime;
                
                // 如果窗口时间超过设定值，重新计算访问率
                if (windowDuration >= RATE_WINDOW_MILLIS) {
                    long accesses = windowAccesses.getAndSet(0);
                    // 计算每秒访问率
                    accessRate = (double) accesses / (windowDuration / 1000.0);
                    // 重置窗口开始时间
                    windowStartTime = currentTime;
                    
                    if (log.isDebugEnabled()) {
                        log.debug("TLB访问率更新: {}/秒 (窗口: {}秒, 访问数: {})", 
                                String.format("%.2f", accessRate),
                                String.format("%.1f", windowDuration / 1000.0),
                                accesses);
                    }
                }
            }
        }
    }

    /**
     * 计算总体命中率
     * @return 命中率（0-1之间的小数）
     */
    public double calculateHitRatio() {
        long accesses = totalAccesses.get();
        return accesses > 0 ? (double) totalHits.get() / accesses : 0;
    }

    /**
     * 计算L1 TLB命中率
     * @return L1命中率
     */
    public double calculateL1HitRatio() {
        long accesses = l1Accesses.get();
        return accesses > 0 ? (double) l1Hits.get() / accesses : 0;
    }

    /**
     * 计算L2 TLB命中率
     * @return L2命中率
     */
    public double calculateL2HitRatio() {
        long accesses = l2Accesses.get();
        return accesses > 0 ? (double) l2Hits.get() / accesses : 0;
    }

    /**
     * 获取L1到L2的访问率（L1未命中率）
     * @return L1到L2的访问比率
     */
    public double getL1ToL2Ratio() {
        long l1Access = l1Accesses.get();
        return l1Access > 0 ? (double) l1Misses.get() / l1Access : 0;
    }

    /**
     * 获取运行时间（秒）
     * @return 运行时间
     */
    public long getUpTimeSeconds() {
        return (System.currentTimeMillis() - startTimeMillis) / 1000;
    }

    /**
     * 获取进程TLB统计列表
     * @return 进程TLB统计列表
     */
    public List<ProcessTLBStat> getProcessStats() {
        synchronized (processStats) {
            return new ArrayList<>(processStats.values());
        }
    }

    /**
     * 获取指定进程的TLB统计
     * @param processId 进程ID
     * @return 进程TLB统计对象
     */
    public ProcessTLBStat getProcessStat(int processId) {
        synchronized (processStats) {
            return processStats.get(processId);
        }
    }

    /**
     * 获取TLB统计摘要
     * @return 统计摘要字符串
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder("TLB统计摘要:\n");
        
        // 添加基本统计
        long upTime = getUpTimeSeconds();
        sb.append("运行时间: ").append(formatDuration(upTime)).append("\n");
        sb.append("总访问数: ").append(totalAccesses.get())
          .append(" (").append(String.format("%.2f", accessRate)).append("/秒)\n");
        
        // 命中率统计
        sb.append("总命中率: ").append(String.format("%.2f%%", calculateHitRatio() * 100)).append(" (");
        sb.append("命中: ").append(totalHits.get()).append(", 未命中: ").append(totalMisses.get()).append(")\n");
        
        // L1和L2统计
        sb.append("L1 TLB命中率: ").append(String.format("%.2f%%", calculateL1HitRatio() * 100));
        sb.append(" (访问: ").append(l1Accesses.get());
        sb.append(", 命中: ").append(l1Hits.get());
        sb.append(", 未命中: ").append(l1Misses.get()).append(")\n");
        
        sb.append("L2 TLB命中率: ").append(String.format("%.2f%%", calculateL2HitRatio() * 100));
        sb.append(" (访问: ").append(l2Accesses.get());
        sb.append(", 命中: ").append(l2Hits.get());
        sb.append(", 未命中: ").append(l2Misses.get()).append(")\n");
        
        // L1到L2访问率
        sb.append("L1->L2访问率: ").append(String.format("%.2f%%", getL1ToL2Ratio() * 100)).append("\n");
        
        // 条目管理统计
        sb.append("条目更新: ").append(entryUpdates.get());
        sb.append(", 条目过期: ").append(entryExpirations.get());
        sb.append(", 条目驱逐: ").append(entryEvictions.get()).append("\n");
        
        // 刷新统计
        sb.append("完全刷新: ").append(flushCount.get());
        sb.append(", 部分刷新: ").append(partialFlushCount.get()).append("\n");
        
        // 进程统计
        int processCount;
        synchronized (processStats) {
            processCount = processStats.size();
        }
        sb.append("进程TLB统计: ").append(processCount).append("个进程\n");
        
        return sb.toString();
    }

    /**
     * 重置所有统计数据
     */
    public void reset() {
        totalAccesses.set(0);
        totalHits.set(0);
        totalMisses.set(0);
        l1Accesses.set(0);
        l1Hits.set(0);
        l1Misses.set(0);
        l2Accesses.set(0);
        l2Hits.set(0);
        l2Misses.set(0);
        entryUpdates.set(0);
        entryExpirations.set(0);
        entryEvictions.set(0);
        flushCount.set(0);
        partialFlushCount.set(0);
        
        windowAccesses.set(0);
        accessRate = 0.0;
        startTimeMillis = System.currentTimeMillis();
        windowStartTime = startTimeMillis;
        lastAccessTimestamp = 0;
        
        synchronized (processStats) {
            processStats.clear();
        }
        
        log.info("TLB统计数据已重置");
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
     * 进程级别的TLB统计
     */
    @Getter
    public static class ProcessTLBStat {
        /**
         * 进程ID
         */
        private final int processId;
        
        /**
         * TLB访问次数
         */
        private final AtomicLong accessCount = new AtomicLong(0);
        
        /**
         * TLB命中次数
         */
        private final AtomicLong hitCount = new AtomicLong(0);
        
        /**
         * TLB未命中次数
         */
        private final AtomicLong missCount = new AtomicLong(0);
        
        /**
         * 创建时间
         */
        private final long createdTime = System.currentTimeMillis();
        
        /**
         * 构造函数
         * @param processId 进程ID
         */
        public ProcessTLBStat(int processId) {
            this.processId = processId;
        }
        
        /**
         * 增加访问计数
         */
        public void incrementAccesses() {
            accessCount.incrementAndGet();
        }
        
        /**
         * 增加命中计数
         */
        public void incrementHits() {
            hitCount.incrementAndGet();
        }
        
        /**
         * 增加未命中计数
         */
        public void incrementMisses() {
            missCount.incrementAndGet();
        }
        
        /**
         * 获取命中率
         * @return 命中率
         */
        public double getHitRatio() {
            long accesses = accessCount.get();
            return accesses > 0 ? (double) hitCount.get() / accesses : 0;
        }
        
        /**
         * 获取未命中率
         * @return 未命中率
         */
        public double getMissRatio() {
            long accesses = accessCount.get();
            return accesses > 0 ? (double) missCount.get() / accesses : 0;
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
     * 获取命中率
     * @return 命中率（0.0-1.0）
     */
    public double getHitRatio() {
        return calculateHitRatio();
    }

    /**
     * 获取命中次数
     * @return 命中次数
     */
    public long getHitCount() {
        return totalHits.get();
    }

    /**
     * 获取未命中次数
     * @return 未命中次数
     */
    public long getMissCount() {
        return totalMisses.get();
    }

    /**
     * 获取TLB条目数量
     * @return TLB条目数量
     */
    public long getEntryCount() {
        // 此处应返回实际的TLB条目数量
        // 假设各个级别的TLB条目总和
        return l1Hits.get() + l2Hits.get();
    }

    /**
     * 获取驱逐次数
     * @return 驱逐次数
     */
    public long getEvictionCount() {
        return entryEvictions.get();
    }

    /**
     * 获取平均访问时间（纳秒）
     * @return 平均访问时间
     */
    public double getAverageAccessTimeNanos() {
        // 实际实现应计算真实的平均访问时间
        // 这里返回一个基于命中率的估计值
        // 假设TLB命中需要1纳秒，未命中需要100纳秒
        double hitRatio = getHitRatio();
        return hitRatio * 1.0 + (1.0 - hitRatio) * 100.0;
    }
} 