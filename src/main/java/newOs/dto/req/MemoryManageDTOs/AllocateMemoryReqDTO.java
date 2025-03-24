package newOs.dto.req.MemoryManageDTOs;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 内存分配请求DTO
 * 用于从客户端传递内存分配请求参数到内存管理服务
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllocateMemoryReqDTO {
    
    /**
     * 进程ID
     * 请求分配内存的进程
     */
    private Integer processId;
    
    /**
     * 请求分配的内存大小（字节）
     */
    private Long size;
    
    /**
     * 内存分配优先级
     * HIGH - 高优先级，优先分配
     * NORMAL - 一般优先级（默认）
     * LOW - 低优先级，可延迟分配
     */
    private String priority = "NORMAL";
    
    /**
     * 内存类型
     * RAM - 随机访问内存（默认）
     * SHARED - 共享内存
     * MAPPED - 内存映射文件
     */
    private String memoryType = "RAM";
    
    /**
     * 所需的对齐方式（字节）
     * 例如：4096表示页对齐
     */
    private Integer alignment = 1;
    
    /**
     * 是否允许交换到磁盘
     * 如果为true，内存可以被换出到磁盘
     */
    private Boolean swappable = true;
    
    /**
     * 内存保护标志
     * 例如："RW"表示可读写，"RX"表示可读可执行
     */
    private String protectionFlags = "RW";
    
    /**
     * 首选的内存地址
     * 如果不为null，系统将尝试在此地址分配内存（如果可能）
     */
    private Long preferredAddress = null;
    
    /**
     * 内存分配策略
     * FIRST_FIT - 首次适应
     * BEST_FIT - 最佳适应（默认）
     * WORST_FIT - 最差适应
     */
    private String allocationStrategy = "BEST_FIT";
    
    /**
     * 是否在分配后自动初始化为0
     */
    private Boolean zeroInitialize = true;
} 