package newOs.dto.resp.MemoryManageDTOs;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * TLB统计信息响应DTO
 * 包含Translation Lookaside Buffer的统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TLBStatsRespDTO {
    
    /**
     * TLB命中次数
     */
    private Long hitCount;
    
    /**
     * TLB未命中次数
     */
    private Long missCount;
    
    /**
     * 总访问次数（命中+未命中）
     */
    private Long totalAccessCount;
    
    /**
     * 命中率（0.0-1.0）
     */
    private Double hitRatio;
    
    /**
     * 当前TLB条目数
     */
    private Integer entryCount;
    
    /**
     * 最大TLB条目数
     */
    private Integer maxEntryCount;
    
    /**
     * TLB驱逐次数
     */
    private Long evictionCount;
    
    /**
     * 平均访问时间（纳秒）
     */
    private Double averageAccessTimeNanos;
    
    /**
     * 最大访问时间（纳秒）
     */
    private Long maxAccessTimeNanos;
    
    /**
     * 最小访问时间（纳秒）
     */
    private Long minAccessTimeNanos;
    
    /**
     * TLB是否启用
     */
    private Boolean enabled;
    
    /**
     * 是否启用多级TLB
     */
    private Boolean multiLevelTLBEnabled;
    
    /**
     * 是否启用TLB预取
     */
    private Boolean prefetchEnabled;
    
    /**
     * 二级TLB命中次数（仅当多级TLB启用时有效）
     */
    private Long l2HitCount;
    
    /**
     * 二级TLB命中率（仅当多级TLB启用时有效）
     */
    private Double l2HitRatio;
    
    /**
     * 进程TLB统计信息列表，按访问次数排序
     */
    private List<ProcessTLBStat> processTLBStats;
    
    /**
     * TLB替换策略（如LRU, FIFO等）
     */
    private String replacementPolicy;
    
    /**
     * TLB写策略（如直写, 回写等）
     */
    private String writePolicy;
    
    /**
     * TLB覆盖范围（全局或每进程）
     */
    private String coverage;
    
    /**
     * 最近的TLB驱逐记录
     */
    private List<TLBEvictionRecord> recentEvictions;
    
    /**
     * 页面大小分布（页面大小 -> 计数）
     */
    private Map<Integer, Integer> pageSizeDistribution;
    
    /**
     * 进程TLB统计信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessTLBStat {
        /**
         * 进程ID
         */
        private Integer processId;
        
        /**
         * 进程名称
         */
        private String processName;
        
        /**
         * TLB命中次数
         */
        private Long hitCount;
        
        /**
         * TLB未命中次数
         */
        private Long missCount;
        
        /**
         * 总访问次数
         */
        private Long totalAccessCount;
        
        /**
         * 命中率
         */
        private Double hitRatio;
        
        /**
         * 平均访问时间（纳秒）
         */
        private Double averageAccessTimeNanos;
    }
    
    /**
     * TLB驱逐记录
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TLBEvictionRecord {
        /**
         * 时间戳（毫秒）
         */
        private Long timestamp;
        
        /**
         * 被驱逐的虚拟页号
         */
        private Long evictedVirtualPage;
        
        /**
         * 被驱逐的物理页号
         */
        private Long evictedPhysicalPage;
        
        /**
         * 所属进程ID
         */
        private Integer processId;
        
        /**
         * 驱逐原因
         */
        private String evictionReason;
        
        /**
         * 条目在TLB中的生存时间（毫秒）
         */
        private Long lifetimeMs;
        
        /**
         * 驱逐时的访问计数
         */
        private Integer accessCount;
    }
} 