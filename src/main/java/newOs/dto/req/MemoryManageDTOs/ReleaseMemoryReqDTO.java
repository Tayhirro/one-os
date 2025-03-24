package newOs.dto.req.MemoryManageDTOs;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 内存释放请求DTO
 * 用于从客户端传递内存释放请求参数到内存管理服务
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseMemoryReqDTO {
    
    /**
     * 进程ID
     * 请求释放内存的进程
     */
    private Integer processId;
    
    /**
     * 要释放的内存的虚拟地址
     */
    private Long virtualAddress;
    
    /**
     * 要释放的内存大小（字节）
     * 如果为null或0，表示释放从虚拟地址开始的整个内存块
     */
    private Long size;
    
    /**
     * 释放类型
     * NORMAL - 正常释放（默认）
     * FORCE - 强制释放
     * DEFERRED - 延迟释放
     */
    private String releaseType = "NORMAL";
    
    /**
     * 是否释放相关资源
     * 如果为true，将释放与该内存块关联的所有资源（如文件句柄）
     */
    private Boolean releaseResources = true;
    
    /**
     * 是否立即回收物理内存
     * 如果为true，物理内存将立即被回收；否则可能会延迟回收
     */
    private Boolean immediateReclaim = true;
} 