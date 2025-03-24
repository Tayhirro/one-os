package newOs.dto.resp.MemoryManageDTOs;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 页面缺失统计响应DTO
 * 包含系统页面缺失的统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageFaultStatsRespDTO {
    
    /**
     * 页面缺失总数
     */
    private Long totalPageFaults;
    
    /**
     * 页面缺失率（每秒缺页次数）
     */
    private Double pageFaultRate;
    
    /**
     * 主要页面缺失数（需要磁盘访问）
     */
    private Long majorPageFaults;
    
    /**
     * 次要页面缺失数（不需要磁盘访问）
     */
    private Long minorPageFaults;
    
    /**
     * 页面调入次数（从磁盘加载到内存的页面数）
     */
    private Long pageInsCount;
    
    /**
     * 页面调出次数（从内存写回到磁盘的页面数）
     */
    private Long pageOutsCount;
    
    /**
     * 页面缺失处理平均时间（纳秒）
     */
    private Double averagePageFaultHandlingTimeNanos;
    
    /**
     * 页面缺失处理最大时间（纳秒）
     */
    private Long maxPageFaultHandlingTimeNanos;
    
    /**
     * 页面缺失处理最小时间（纳秒）
     */
    private Long minPageFaultHandlingTimeNanos;
    
    /**
     * 当前页面缺失处理队列长度
     */
    private Integer currentPageFaultQueueLength;
    
    /**
     * 最近一次页面缺失时间戳（毫秒）
     */
    private Long lastPageFaultTimestamp;
    
    /**
     * 是否检测到颠簸（频繁页面交换）
     */
    private Boolean thrashingDetected;
    
    /**
     * 颠簸次数
     */
    private Integer thrashingCount;
    
    /**
     * 进程页面缺失统计，按页面缺失次数排序
     */
    private List<ProcessPageFaultStat> processPageFaultStats;
    
    /**
     * 页面替换策略
     */
    private String pageReplacementStrategy;
    
    /**
     * 最近的页面缺失记录
     */
    private List<PageFaultRecord> recentPageFaults;
    
    /**
     * 页面访问计数（页号 -> 访问次数）
     */
    private Map<Integer, Integer> pageAccessCounts;
    
    /**
     * 每小时页面缺失统计（小时 -> 缺失次数）
     */
    private Map<Integer, Long> pageFaultsByHour;
    
    /**
     * 进程页面缺失统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessPageFaultStat {
        /**
         * 进程ID
         */
        private Integer processId;
        
        /**
         * 进程名称
         */
        private String processName;
        
        /**
         * 页面缺失总数
         */
        private Long pageFaults;
        
        /**
         * 主要页面缺失数
         */
        private Long majorPageFaults;
        
        /**
         * 次要页面缺失数
         */
        private Long minorPageFaults;
        
        /**
         * 页面调入次数
         */
        private Long pageInsCount;
        
        /**
         * 页面调出次数
         */
        private Long pageOutsCount;
        
        /**
         * 最近一次页面缺失时间戳（毫秒）
         */
        private Long lastPageFaultTimestamp;
    }
    
    /**
     * 页面缺失记录
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageFaultRecord {
        /**
         * 时间戳（毫秒）
         */
        private Long timestamp;
        
        /**
         * 进程ID
         */
        private Integer processId;
        
        /**
         * 虚拟地址
         */
        private Long virtualAddress;
        
        /**
         * 是否为主要页面缺失
         */
        private Boolean isMajor;
        
        /**
         * 处理时间（纳秒）
         */
        private Long handlingTimeNanos;
        
        /**
         * 访问类型（读/写/执行）
         */
        private String accessType;
    }
} 