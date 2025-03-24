package newOs.kernel.memory.virtual.protection;

import newOs.exception.MemoryProtectionException;
import newOs.kernel.memory.model.VirtualAddress;

/**
 * 内存保护机制接口
 * 负责处理内存访问权限控制和保护异常
 */
public interface MemoryProtection {
    
    /**
     * 检查内存访问权限
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param isWrite 是否为写操作
     * @return 是否有权限访问
     * @throws MemoryProtectionException 内存保护异常
     */
    boolean checkAccess(int processId, VirtualAddress virtualAddress, boolean isWrite) 
            throws MemoryProtectionException;
    
    /**
     * 检查内存读取权限
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 内存区域大小
     * @return 是否有读取权限
     * @throws MemoryProtectionException 内存保护异常
     */
    boolean checkReadAccess(int processId, VirtualAddress virtualAddress, long size) 
            throws MemoryProtectionException;
    
    /**
     * 检查内存写入权限
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 内存区域大小
     * @return 是否有写入权限
     * @throws MemoryProtectionException 内存保护异常
     */
    boolean checkWriteAccess(int processId, VirtualAddress virtualAddress, long size) 
            throws MemoryProtectionException;
    
    /**
     * 检查内存执行权限
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 内存区域大小
     * @return 是否有执行权限
     * @throws MemoryProtectionException 内存保护异常
     */
    boolean checkExecuteAccess(int processId, VirtualAddress virtualAddress, long size) 
            throws MemoryProtectionException;
    
    /**
     * 设置内存访问控制
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 内存区域大小
     * @param canRead 是否可读
     * @param canWrite 是否可写
     * @param canExecute 是否可执行
     * @throws MemoryProtectionException 内存保护异常
     */
    void setAccessControl(int processId, VirtualAddress virtualAddress, long size, 
                         boolean canRead, boolean canWrite, boolean canExecute) 
            throws MemoryProtectionException;
    
    /**
     * 移除内存访问控制
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 内存区域大小
     * @throws MemoryProtectionException 内存保护异常
     */
    void removeAccessControl(int processId, VirtualAddress virtualAddress, long size) 
            throws MemoryProtectionException;
    
    /**
     * 修改内存访问权限
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 内存区域大小
     * @param setRead 设置读权限（null表示不改变）
     * @param setWrite 设置写权限（null表示不改变）
     * @param setExecute 设置执行权限（null表示不改变）
     * @throws MemoryProtectionException 内存保护异常
     */
    void changeAccessPermission(int processId, VirtualAddress virtualAddress, long size, 
                               Boolean setRead, Boolean setWrite, Boolean setExecute) 
            throws MemoryProtectionException;
            
    /**
     * 处理内存保护故障
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param isRead 是否为读操作
     * @param isWrite 是否为写操作
     * @param isExecute 是否为执行操作
     * @return 是否成功处理故障
     */
    boolean handleProtectionFault(int processId, VirtualAddress virtualAddress, 
                                  boolean isRead, boolean isWrite, boolean isExecute);
    
    /**
     * 记录访问违规
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param accessType 访问类型描述
     */
    void recordViolation(int processId, VirtualAddress virtualAddress, String accessType);
    
    /**
     * 获取统计信息
     * @return 保护机制统计信息
     */
    String getStatistics();
    
    /**
     * 重置统计信息
     */
    void resetStatistics();
} 