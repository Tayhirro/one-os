package newOs.dto.resp.MemoryManageDTOs;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 内存使用情况响应DTO
 * 包含系统内存使用的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryUsageRespDTO {
    
    /**
     * 总物理内存大小（字节）
     */
    private Long totalPhysicalMemory;
    
    /**
     * 已使用物理内存大小（字节）
     */
    private Long usedPhysicalMemory;
    
    /**
     * 可用物理内存大小（字节）
     */
    private Long freePhysicalMemory;
    
    /**
     * 物理内存使用率（0.0-1.0）
     */
    private Double physicalMemoryUsageRatio;
    
    /**
     * 总交换空间大小（字节）
     */
    private Long totalSwapSpace;
    
    /**
     * 已使用交换空间大小（字节）
     */
    private Long usedSwapSpace;
    
    /**
     * 可用交换空间大小（字节）
     */
    private Long freeSwapSpace;
    
    /**
     * 交换空间使用率（0.0-1.0）
     */
    private Double swapUsageRatio;
    
    /**
     * 内存碎片化指数（0.0-1.0，值越大表示碎片化程度越高）
     */
    private Double fragmentationIndex;
    
    /**
     * 页面大小（字节）
     */
    private Integer pageSize;
    
    /**
     * 总页框数
     */
    private Integer totalPageFrames;
    
    /**
     * 已使用页框数
     */
    private Integer usedPageFrames;
    
    /**
     * 空闲页框数
     */
    private Integer freePageFrames;
    
    /**
     * 虚拟内存是否启用
     */
    private Boolean virtualMemoryEnabled;
    
    /**
     * 交换空间是否启用
     */
    private Boolean swapEnabled;
    
    /**
     * 内存分配策略（如"FIRST_FIT", "BEST_FIT", "WORST_FIT"等）
     */
    private String allocationStrategy;
    
    /**
     * 页面置换策略（如"LRU", "FIFO", "CLOCK"等）
     */
    private String pageReplacementStrategy;
    
    /**
     * 内存使用告警阈值（百分比）
     */
    private Integer memoryUsageAlertThreshold;
    
    /**
     * 交换阈值（触发交换操作的内存使用百分比）
     */
    private Integer swappingThreshold;
    
    /**
     * 内存过度分配比率（虚拟内存可以超出物理内存的比例）
     */
    private Double overcommitRatio;
    
    /**
     * 最大连续空闲内存块大小（字节）
     */
    private Long largestFreeBlock;
    
    /**
     * 进程内存使用情况列表
     */
    private List<ProcessMemoryUsage> processMemoryUsages;
    
    /**
     * 受保护内存区域数量
     */
    private Integer protectedRegionsCount;
    
    /**
     * 附加统计信息
     */
    private Map<String, Object> additionalStats;
    
    /**
     * 进程内存使用情况
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessMemoryUsage {
        /**
         * 进程ID
         */
        private Integer processId;
        
        /**
         * 进程名称
         */
        private String processName;
        
        /**
         * 当前内存使用量（字节）
         */
        private Long currentUsage;
        
        /**
         * 峰值内存使用量（字节）
         */
        private Long peakUsage;
        
        /**
         * 已分配总内存（字节）
         */
        private Long totalAllocated;
        
        /**
         * 已释放总内存（字节）
         */
        private Long totalFreed;
        
        /**
         * 内存分配次数
         */
        private Integer allocationCount;
        
        /**
         * 内存释放次数
         */
        private Integer freeCount;
        
        /**
         * 当前映射的页面数
         */
        private Integer mappedPages;
        
        /**
         * 页面缺失次数
         */
        private Long pageFaults;
        
        /**
         * 内存区域数量
         */
        private Integer memoryRegionCount;
    }
} 