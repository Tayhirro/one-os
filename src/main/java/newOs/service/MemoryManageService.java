package newOs.service;

import newOs.exception.MemoryException;
import newOs.kernel.memory.model.MemoryRegion;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;

import java.util.List;
import java.util.Map;

/**
 * 内存管理服务接口
 * 提供系统级内存管理功能，包括内存初始化、配置、策略设置等
 */
public interface MemoryManageService {

    /**
     * 初始化内存系统
     * @param physicalMemorySize 物理内存大小（字节）
     * @param swapSize 交换空间大小（字节）
     * @throws MemoryException 内存异常
     */
    void initializeMemorySystem(long physicalMemorySize, long swapSize) throws MemoryException;

    /**
     * 获取系统物理内存信息
     * @return 内存信息映射表
     */
    Map<String, Object> getPhysicalMemoryInfo();

    /**
     * 获取系统虚拟内存信息
     * @return 内存信息映射表
     */
    Map<String, Object> getVirtualMemoryInfo();

    /**
     * 获取交换空间信息
     * @return 交换空间信息映射表
     */
    Map<String, Object> getSwapSpaceInfo();

    /**
     * 配置内存分配策略
     * @param strategyName 策略名称
     * @param parameters 策略参数
     * @throws MemoryException 内存异常
     */
    void configureAllocationStrategy(String strategyName, Map<String, Object> parameters) throws MemoryException;

    /**
     * 配置页面替换策略
     * @param strategyName 策略名称
     * @param parameters 策略参数
     * @throws MemoryException 内存异常
     */
    void configurePageReplacementStrategy(String strategyName, Map<String, Object> parameters) throws MemoryException;

    /**
     * 设置系统内存保护区域
     * @param startAddress 起始物理地址
     * @param size 大小（字节）
     * @param description 描述
     * @throws MemoryException 内存异常
     */
    void setProtectedMemoryRegion(PhysicalAddress startAddress, long size, String description) throws MemoryException;

    /**
     * 删除系统内存保护区域
     * @param startAddress 起始物理地址
     * @throws MemoryException 内存异常
     */
    void removeProtectedMemoryRegion(PhysicalAddress startAddress) throws MemoryException;

    /**
     * 获取所有内存保护区域列表
     * @return 内存保护区域列表
     */
    List<MemoryRegion> getProtectedMemoryRegions();

    /**
     * 调整TLB大小
     * @param tlbSize 新的TLB大小
     * @throws MemoryException 内存异常
     */
    void resizeTLB(int tlbSize) throws MemoryException;

    /**
     * 刷新整个TLB
     * @throws MemoryException 内存异常
     */
    void flushTLB() throws MemoryException;

    /**
     * 刷新指定进程的TLB条目
     * @param processId 进程ID
     * @throws MemoryException 内存异常
     */
    void flushProcessTLB(int processId) throws MemoryException;

    /**
     * 获取TLB统计信息
     * @return TLB统计信息
     */
    Map<String, Object> getTLBStatistics();

    /**
     * 压缩物理内存（通过页面合并或压缩）
     * @return 压缩前后的内存变化信息
     * @throws MemoryException 内存异常
     */
    Map<String, Object> compressPhysicalMemory() throws MemoryException;

    /**
     * 设置内存分页大小
     * @param pageSize 页面大小（字节）
     * @throws MemoryException 内存异常
     */
    void setPageSize(int pageSize) throws MemoryException;

    /**
     * 获取当前内存分页大小
     * @return 页面大小（字节）
     */
    int getPageSize();

    /**
     * 设置内存超额分配比例
     * @param ratio 超额分配比例(>1.0)
     * @throws MemoryException 内存异常
     */
    void setMemoryOvercommitRatio(double ratio) throws MemoryException;

    /**
     * 获取内存超额分配比例
     * @return 超额分配比例
     */
    double getMemoryOvercommitRatio();

    /**
     *.设置交换出页阈值
     * @param threshold 阈值（0-100）
     * @throws MemoryException 内存异常
     */
    void setSwappingThreshold(int threshold) throws MemoryException;

    /**
     * 获取交换出页阈值
     * @return 阈值（0-100）
     */
    int getSwappingThreshold();

    /**
     * 挂载交换设备
     * @param devicePath 设备路径
     * @param size 大小（字节）
     * @throws MemoryException 内存异常
     */
    void mountSwapDevice(String devicePath, long size) throws MemoryException;

    /**
     * 卸载交换设备
     * @param devicePath 设备路径
     * @throws MemoryException 内存异常
     */
    void unmountSwapDevice(String devicePath) throws MemoryException;

    /**
     * 创建内存转储文件
     * @param filePath 文件路径
     * @param processId 进程ID，为0表示整个系统
     * @return 是否成功
     * @throws MemoryException 内存异常
     */
    boolean createMemoryDump(String filePath, int processId) throws MemoryException;

    /**
     * 获取内存碎片化指数
     * @return 碎片化指数（0-1之间，越接近1表示碎片化越严重）
     */
    double getFragmentationIndex();

    /**
     * 执行内存碎片整理
     * @return 整理前后的内存变化信息
     * @throws MemoryException 内存异常
     */
    Map<String, Object> defragmentMemory() throws MemoryException;

    /**
     * 获取指定进程的内存使用情况
     * @param processId 进程ID
     * @return 内存使用情况
     * @throws MemoryException 内存异常
     */
    Map<String, Object> getProcessMemoryUsage(int processId) throws MemoryException;

    /**
     * 列出所有进程的内存使用情况（按内存使用量排序）
     * @return 进程内存使用情况列表
     */
    List<Map<String, Object>> listProcessesMemoryUsage();

    /**
     * 设置系统内存使用告警阈值
     * @param percentThreshold 百分比阈值（0-100）
     */
    void setMemoryUsageAlertThreshold(int percentThreshold);

    /**
     * 获取系统内存使用告警阈值
     * @return 百分比阈值（0-100）
     */
    int getMemoryUsageAlertThreshold();

    /**
     * 执行垃圾内存回收
     * @return 回收的内存大小（字节）
     * @throws MemoryException 内存异常
     */
    long performGarbageCollection() throws MemoryException;
} 