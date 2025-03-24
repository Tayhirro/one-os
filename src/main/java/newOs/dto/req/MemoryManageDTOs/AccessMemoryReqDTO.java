package newOs.dto.req.MemoryManageDTOs;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 内存访问请求DTO
 * 用于从客户端传递内存访问请求参数到内存管理服务
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessMemoryReqDTO {
    
    /**
     * 进程ID
     * 请求访问内存的进程
     */
    private Integer processId;
    
    /**
     * 访问的虚拟地址
     */
    private Long virtualAddress;
    
    /**
     * 访问类型
     * READ - 读取内存
     * WRITE - 写入内存
     * EXECUTE - 执行内存中的代码
     */
    private String accessType = "READ";
    
    /**
     * 访问大小（字节）
     * 要读取或写入的数据大小
     */
    private Integer size = 4; // 默认4字节
    
    /**
     * 要写入的数据（十六进制字符串）
     * 仅在写入操作时使用
     * 例如："0x12345678"
     */
    private String writeData;
    
    /**
     * 是否检查访问权限
     * 如果为true，将检查进程是否有权限访问该内存
     */
    private Boolean checkPermissions = true;
    
    /**
     * 是否记录访问统计
     * 如果为true，将记录该次访问用于统计和分析
     */
    private Boolean recordStats = true;
    
    /**
     * 是否模拟访问
     * 如果为true，只检查访问是否可行，但不实际执行读写操作
     */
    private Boolean dryRun = false;
} 