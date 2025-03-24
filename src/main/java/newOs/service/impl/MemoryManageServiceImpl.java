package newOs.service.impl;

import lombok.extern.slf4j.Slf4j;
import newOs.exception.MemoryException;
import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.model.MemoryRegion;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.monitor.MemoryStats;
import newOs.kernel.memory.monitor.PageFaultStats;
import newOs.kernel.memory.monitor.TLBStats;
import newOs.kernel.memory.monitor.MemoryUsageMonitor;
import newOs.kernel.memory.virtual.replacement.PageReplacementManager;
import newOs.kernel.memory.allocation.MemoryAllocator;
import newOs.kernel.memory.virtual.paging.SwapManager;
import newOs.service.MemoryManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存管理服务实现类
 * 提供系统级内存管理功能，包括内存初始化、配置、策略设置等
 */
@Service
@Slf4j
public class MemoryManageServiceImpl implements MemoryManageService {

    @Autowired
    private MemoryManager memoryManager;
    
    @Autowired
    private MemoryStats memoryStats;
    
    @Autowired
    private PageFaultStats pageFaultStats;
    
    @Autowired
    private TLBStats tlbStats;
    
    @Autowired
    private MemoryUsageMonitor memoryUsageMonitor;
    
    @Autowired
    private PageReplacementManager pageReplacementManager;
    
    @Autowired
    private MemoryAllocator memoryAllocator;
    
    @Autowired
    private SwapManager swapManager;
    
    // 内存保护区域列表
    private final List<MemoryRegion> protectedRegions = new ArrayList<>();
    
    // 系统内存使用告警阈值（百分比）
    @Value("${memory.usage.alert.threshold:90}")
    private int memoryUsageAlertThreshold = 90;
    
    // 当前页面大小（字节）
    @Value("${memory.page.size:4096}")
    private int pageSize = 4096;
    
    // 内存超额分配比例
    @Value("${memory.overcommit.ratio:1.5}")
    private double memoryOvercommitRatio = 1.5;
    
    // 交换出页阈值（百分比）
    @Value("${memory.swapping.threshold:80}")
    private int swappingThreshold = 80;
    
    // 交换设备映射表：路径 -> 大小
    private final Map<String, Long> swapDevices = new ConcurrentHashMap<>();
    
    @Override
    public void initializeMemorySystem(long physicalMemorySize, long swapSize) throws MemoryException {
        log.info("初始化内存系统，物理内存大小: {}字节，交换空间大小: {}字节", physicalMemorySize, swapSize);
        
        try {
            // 初始化物理内存
            memoryManager.initializePhysicalMemory(physicalMemorySize);
            
            // 初始化交换空间
            if (swapSize > 0) {
                memoryManager.initializeSwapSpace(swapSize);
            }
            
            // 更新内存统计信息
            memoryStats.setTotalPhysicalMemory(physicalMemorySize);
            memoryStats.setFreePhysicalMemory(physicalMemorySize);
            memoryStats.setTotalSwapSpace(swapSize);
            memoryStats.setFreeSwapSpace(swapSize);
            
            log.info("内存系统初始化完成");
        } catch (Exception e) {
            log.error("内存系统初始化失败: {}", e.getMessage(), e);
            throw new MemoryException("内存系统初始化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getPhysicalMemoryInfo() {
        log.debug("获取系统物理内存信息");
        
        Map<String, Object> info = new HashMap<>();
        info.put("totalSize", memoryStats.getTotalPhysicalMemory());
        info.put("usedSize", memoryStats.getUsedPhysicalMemory());
        info.put("freeSize", memoryStats.getFreePhysicalMemory());
        info.put("usageRatio", memoryStats.getPhysicalMemoryUsageRatio());
        info.put("fragmentationIndex", memoryStats.getFragmentationIndex());
        info.put("pageSize", pageSize);
        info.put("pageFramesCount", memoryStats.getPageFramesCount());
        info.put("usedPageFramesCount", memoryStats.getUsedPageFramesCount());
        info.put("freePageFramesCount", memoryStats.getFreePageFramesCount());
        
        return info;
    }

    @Override
    public Map<String, Object> getVirtualMemoryInfo() {
        log.debug("获取系统虚拟内存信息");
        
        Map<String, Object> info = new HashMap<>();
        info.put("enabled", memoryManager.isVirtualMemoryEnabled());
        info.put("totalVirtualMemory", memoryStats.getTotalPhysicalMemory() + memoryStats.getTotalSwapSpace());
        info.put("pageSize", pageSize);
        info.put("overcommitRatio", memoryOvercommitRatio);
        info.put("tlbEnabled", memoryManager.isTlbEnabled());
        info.put("tlbHitRatio", tlbStats.getHitRatio());
        info.put("pageFaultCount", pageFaultStats.getTotalPageFaults().get());
        info.put("pageFaultRate", pageFaultStats.getPageFaultRate());
        
        return info;
    }

    @Override
    public Map<String, Object> getSwapSpaceInfo() {
        log.debug("获取交换空间信息");
        
        Map<String, Object> info = new HashMap<>();
        info.put("enabled", memoryManager.isSwapEnabled());
        info.put("totalSize", memoryStats.getTotalSwapSpace());
        info.put("usedSize", memoryStats.getUsedSwapSpace());
        info.put("freeSize", memoryStats.getFreeSwapSpace());
        info.put("usageRatio", memoryStats.getSwapUsageRatio());
        info.put("swappingThreshold", swappingThreshold);
        info.put("pageInsCount", pageFaultStats.getPageInsCount().get());
        info.put("pageOutsCount", pageFaultStats.getPageOutsCount().get());
        info.put("swapDevices", new HashMap<>(swapDevices));
        
        return info;
    }

    @Override
    public void configureAllocationStrategy(String strategyName, Map<String, Object> parameters) throws MemoryException {
        log.info("配置内存分配策略: {}，参数: {}", strategyName, parameters);
        
        try {
            // 根据策略名称选择不同的分配器实现
            memoryManager.setAllocationStrategy(strategyName, parameters);
            log.info("内存分配策略配置成功");
        } catch (Exception e) {
            log.error("配置内存分配策略失败: {}", e.getMessage(), e);
            throw new MemoryException("配置内存分配策略失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void configurePageReplacementStrategy(String strategyName, Map<String, Object> parameters) throws MemoryException {
        log.info("配置页面替换策略: {}，参数: {}", strategyName, parameters);
        
        try {
            // 根据策略名称选择不同的页面替换实现
            memoryManager.setPageReplacementStrategy(strategyName, parameters);
            log.info("页面替换策略配置成功");
        } catch (Exception e) {
            log.error("配置页面替换策略失败: {}", e.getMessage(), e);
            throw new MemoryException("配置页面替换策略失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void setProtectedMemoryRegion(PhysicalAddress startAddress, long size, String description) throws MemoryException {
        log.info("设置系统内存保护区域，起始地址: {}，大小: {}，描述: {}", startAddress, size, description);
        
        try {
            // 检查地址范围是否合法
            if (startAddress.getValue() + size > memoryStats.getTotalPhysicalMemory()) {
                throw new MemoryException("地址范围超出物理内存大小");
            }
            
            // 检查是否与现有保护区域重叠
            for (MemoryRegion region : protectedRegions) {
                if (isOverlapping(region.getStartAddress(), region.getSize(), startAddress, size)) {
                    throw new MemoryException("新保护区域与现有区域重叠: " + region.getDescription());
                }
            }
            
            // 添加保护区域
            MemoryRegion region = new MemoryRegion(startAddress, size, description);
            protectedRegions.add(region);
            
            // 设置内存保护
            memoryManager.protectMemoryRegion(startAddress, size);
            
            log.info("内存保护区域设置成功");
        } catch (Exception e) {
            log.error("设置内存保护区域失败: {}", e.getMessage(), e);
            throw new MemoryException("设置内存保护区域失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeProtectedMemoryRegion(PhysicalAddress startAddress) throws MemoryException {
        log.info("删除系统内存保护区域，起始地址: {}", startAddress);
        
        try {
            // 查找匹配的保护区域
            MemoryRegion targetRegion = null;
            for (MemoryRegion region : protectedRegions) {
                if (region.getStartAddress().equals(startAddress)) {
                    targetRegion = region;
                    break;
                }
            }
            
            if (targetRegion == null) {
                throw new MemoryException("找不到指定的内存保护区域: " + startAddress);
            }
            
            // 移除保护区域
            protectedRegions.remove(targetRegion);
            
            // 解除内存保护
            memoryManager.unprotectMemoryRegion(startAddress, targetRegion.getSize());
            
            log.info("内存保护区域删除成功");
        } catch (Exception e) {
            log.error("删除内存保护区域失败: {}", e.getMessage(), e);
            throw new MemoryException("删除内存保护区域失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MemoryRegion> getProtectedMemoryRegions() {
        return new ArrayList<>(protectedRegions);
    }

    @Override
    public void resizeTLB(int tlbSize) throws MemoryException {
        log.info("调整TLB大小: {}", tlbSize);
        
        try {
            // 调整TLB大小
            memoryManager.resizeTLB(tlbSize);
            log.info("TLB大小调整成功");
        } catch (Exception e) {
            log.error("调整TLB大小失败: {}", e.getMessage(), e);
            throw new MemoryException("调整TLB大小失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void flushTLB() throws MemoryException {
        log.info("刷新整个TLB");
        
        try {
            // 刷新TLB
            memoryManager.flushTLB();
            log.info("TLB刷新成功");
        } catch (Exception e) {
            log.error("刷新TLB失败: {}", e.getMessage(), e);
            throw new MemoryException("刷新TLB失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void flushProcessTLB(int processId) throws MemoryException {
        log.info("刷新进程TLB条目，进程ID: {}", processId);
        
        try {
            // 刷新指定进程的TLB条目
            memoryManager.flushProcessTLB(processId);
            log.info("进程TLB条目刷新成功");
        } catch (Exception e) {
            log.error("刷新进程TLB条目失败: {}", e.getMessage(), e);
            throw new MemoryException("刷新进程TLB条目失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getTLBStatistics() {
        log.debug("获取TLB统计信息");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitCount", tlbStats.getHitCount());
        stats.put("missCount", tlbStats.getMissCount());
        stats.put("hitRatio", tlbStats.getHitRatio());
        stats.put("entryCount", tlbStats.getEntryCount());
        stats.put("evictionCount", tlbStats.getEvictionCount());
        stats.put("averageAccessTime", tlbStats.getAverageAccessTimeNanos());
        
        return stats;
    }

    @Override
    public Map<String, Object> compressPhysicalMemory() throws MemoryException {
        log.info("压缩物理内存");
        
        try {
            // 记录压缩前状态
            long beforeUsed = memoryStats.getUsedPhysicalMemory();
            double beforeFragmentation = memoryStats.getFragmentationIndex();
            
            // 执行内存压缩
            memoryManager.compressPhysicalMemory();
            
            // 记录压缩后状态
            long afterUsed = memoryStats.getUsedPhysicalMemory();
            double afterFragmentation = memoryStats.getFragmentationIndex();
            
            // 构造结果
            Map<String, Object> result = new HashMap<>();
            result.put("beforeUsedMemory", beforeUsed);
            result.put("afterUsedMemory", afterUsed);
            result.put("memoryReduction", beforeUsed - afterUsed);
            result.put("beforeFragmentation", beforeFragmentation);
            result.put("afterFragmentation", afterFragmentation);
            result.put("fragmentationReduction", beforeFragmentation - afterFragmentation);
            
            log.info("物理内存压缩完成，减少: {}字节", beforeUsed - afterUsed);
            return result;
        } catch (Exception e) {
            log.error("压缩物理内存失败: {}", e.getMessage(), e);
            throw new MemoryException("压缩物理内存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void setPageSize(int pageSize) throws MemoryException {
        log.info("设置内存分页大小: {} -> {}", this.pageSize, pageSize);
        
        try {
            // 检查页面大小是否是2的幂
            if (pageSize <= 0 || (pageSize & (pageSize - 1)) != 0) {
                throw new MemoryException("页面大小必须是2的幂: " + pageSize);
            }
            
            // 更改页面大小需要重置内存系统
            memoryManager.setPageSize(pageSize);
            
            // 更新成员变量
            this.pageSize = pageSize;
            
            log.info("内存分页大小设置成功");
        } catch (Exception e) {
            log.error("设置内存分页大小失败: {}", e.getMessage(), e);
            throw new MemoryException("设置内存分页大小失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public void setMemoryOvercommitRatio(double ratio) throws MemoryException {
        log.info("设置内存超额分配比例: {} -> {}", memoryOvercommitRatio, ratio);
        
        try {
            // 检查比例是否合法
            if (ratio <= 1.0) {
                throw new MemoryException("内存超额分配比例必须大于1.0: " + ratio);
            }
            
            // 设置超额分配比例
            memoryManager.setOvercommitRatio(ratio);
            
            // 更新成员变量
            this.memoryOvercommitRatio = ratio;
            
            log.info("内存超额分配比例设置成功");
        } catch (Exception e) {
            log.error("设置内存超额分配比例失败: {}", e.getMessage(), e);
            throw new MemoryException("设置内存超额分配比例失败: " + e.getMessage(), e);
        }
    }

    @Override
    public double getMemoryOvercommitRatio() {
        return memoryOvercommitRatio;
    }

    @Override
    public void setSwappingThreshold(int threshold) throws MemoryException {
        log.info("设置交换出页阈值: {} -> {}", swappingThreshold, threshold);
        
        try {
            // 检查阈值是否合法
            if (threshold < 0 || threshold > 100) {
                throw new MemoryException("交换出页阈值必须在0-100之间: " + threshold);
            }
            
            // 设置交换出页阈值
            memoryManager.setSwappingThreshold(threshold);
            
            // 更新成员变量
            this.swappingThreshold = threshold;
            
            log.info("交换出页阈值设置成功");
        } catch (Exception e) {
            log.error("设置交换出页阈值失败: {}", e.getMessage(), e);
            throw new MemoryException("设置交换出页阈值失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int getSwappingThreshold() {
        return swappingThreshold;
    }

    @Override
    public void mountSwapDevice(String devicePath, long size) throws MemoryException {
        log.info("挂载交换设备，路径: {}，大小: {}", devicePath, size);
        
        try {
            // 检查设备是否已存在
            if (swapDevices.containsKey(devicePath)) {
                throw new MemoryException("交换设备已存在: " + devicePath);
            }
            
            // 挂载交换设备
            memoryManager.mountSwapDevice(devicePath, size);
            
            // 记录设备信息
            swapDevices.put(devicePath, size);
            
            // 更新交换空间统计
            memoryStats.setTotalSwapSpace(memoryStats.getTotalSwapSpace() + size);
            memoryStats.setFreeSwapSpace(memoryStats.getFreeSwapSpace() + size);
            
            log.info("交换设备挂载成功");
        } catch (Exception e) {
            log.error("挂载交换设备失败: {}", e.getMessage(), e);
            throw new MemoryException("挂载交换设备失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void unmountSwapDevice(String devicePath) throws MemoryException {
        log.info("卸载交换设备，路径: {}", devicePath);
        
        try {
            // 检查设备是否存在
            Long deviceSize = swapDevices.get(devicePath);
            if (deviceSize == null) {
                throw new MemoryException("交换设备不存在: " + devicePath);
            }
            
            // 卸载交换设备
            memoryManager.unmountSwapDevice(devicePath);
            
            // 更新统计信息
            memoryStats.setTotalSwapSpace(memoryStats.getTotalSwapSpace() - deviceSize);
            
            // 移除设备记录
            swapDevices.remove(devicePath);
            
            log.info("交换设备卸载成功");
        } catch (Exception e) {
            log.error("卸载交换设备失败: {}", e.getMessage(), e);
            throw new MemoryException("卸载交换设备失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean createMemoryDump(String filePath, int processId) throws MemoryException {
        log.info("创建内存转储文件，路径: {}，进程ID: {}", filePath, processId);
        
        try {
            boolean result = memoryManager.createMemoryDump(filePath, processId);
            log.info("内存转储文件创建{}", result ? "成功" : "失败");
            return result;
        } catch (Exception e) {
            log.error("创建内存转储文件失败: {}", e.getMessage(), e);
            throw new MemoryException("创建内存转储文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public double getFragmentationIndex() {
        return memoryStats.getFragmentationIndex();
    }

    @Override
    public Map<String, Object> defragmentMemory() throws MemoryException {
        log.info("执行内存碎片整理");
        
        try {
            // 记录整理前状态
            double beforeIndex = memoryStats.getFragmentationIndex();
            long beforeLargestBlock = memoryAllocator.getLargestFreeBlockSize();
            
            // 执行碎片整理
            memoryManager.defragmentMemory();
            
            // 记录整理后状态
            double afterIndex = memoryStats.getFragmentationIndex();
            long afterLargestBlock = memoryAllocator.getLargestFreeBlockSize();
            
            // 构造结果
            Map<String, Object> result = new HashMap<>();
            result.put("beforeFragmentationIndex", beforeIndex);
            result.put("afterFragmentationIndex", afterIndex);
            result.put("improvementPercent", (beforeIndex - afterIndex) / beforeIndex * 100);
            result.put("beforeLargestFreeBlock", beforeLargestBlock);
            result.put("afterLargestFreeBlock", afterLargestBlock);
            result.put("largestBlockIncrease", afterLargestBlock - beforeLargestBlock);
            
            log.info("内存碎片整理完成，碎片指数: {} -> {}", beforeIndex, afterIndex);
            return result;
        } catch (Exception e) {
            log.error("执行内存碎片整理失败: {}", e.getMessage(), e);
            throw new MemoryException("执行内存碎片整理失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getProcessMemoryUsage(int processId) throws MemoryException {
        log.debug("获取进程内存使用情况，进程ID: {}", processId);
        
        try {
            // 获取进程内存统计
            MemoryStats.ProcessMemoryStat processStat = memoryStats.getProcessStat(processId);
            if (processStat == null) {
                throw new MemoryException("找不到进程的内存统计信息: " + processId);
            }
            
            // 构造结果
            Map<String, Object> usage = new HashMap<>();
            usage.put("processId", processId);
            usage.put("currentUsage", processStat.getCurrentMemoryUsage().get());
            usage.put("peakUsage", processStat.getPeakMemoryUsage().get());
            usage.put("totalAllocated", processStat.getTotalAllocated().get());
            usage.put("totalFreed", processStat.getTotalFreed().get());
            usage.put("allocationCount", processStat.getAllocationCount().get());
            usage.put("freeCount", processStat.getFreeCount().get());
            usage.put("memoryTurnoverRatio", processStat.getMemoryTurnoverRatio());
            usage.put("ageSeconds", processStat.getAgeSeconds());
            
            return usage;
        } catch (Exception e) {
            log.error("获取进程内存使用情况失败: {}", e.getMessage(), e);
            throw new MemoryException("获取进程内存使用情况失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> listProcessesMemoryUsage() {
        log.debug("列出所有进程的内存使用情况");
        
        // 获取所有进程内存统计
        List<MemoryStats.ProcessMemoryStat> processStats = memoryStats.getProcessStats();
        
        // 转换为结果列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (MemoryStats.ProcessMemoryStat stat : processStats) {
            Map<String, Object> usage = new HashMap<>();
            usage.put("processId", stat.getProcessId());
            usage.put("currentUsage", stat.getCurrentMemoryUsage().get());
            usage.put("peakUsage", stat.getPeakMemoryUsage().get());
            usage.put("totalAllocated", stat.getTotalAllocated().get());
            usage.put("totalFreed", stat.getTotalFreed().get());
            usage.put("allocationCount", stat.getAllocationCount().get());
            usage.put("freeCount", stat.getFreeCount().get());
            result.add(usage);
        }
        
        // 按内存使用量排序（降序）
        result.sort((a, b) -> {
            long usageA = (long) a.get("currentUsage");
            long usageB = (long) b.get("currentUsage");
            return Long.compare(usageB, usageA);
        });
        
        return result;
    }

    @Override
    public void setMemoryUsageAlertThreshold(int percentThreshold) {
        log.info("设置系统内存使用告警阈值: {} -> {}", memoryUsageAlertThreshold, percentThreshold);
        
        // 更新阈值
        this.memoryUsageAlertThreshold = percentThreshold;
        
        // 更新监控器阈值
        memoryUsageMonitor.setMemoryUsageAlertThreshold(percentThreshold);
    }

    @Override
    public int getMemoryUsageAlertThreshold() {
        return memoryUsageAlertThreshold;
    }

    @Override
    public long performGarbageCollection() throws MemoryException {
        log.info("执行垃圾内存回收");
        
        try {
            // 记录回收前的使用量
            long beforeUsed = memoryStats.getUsedPhysicalMemory();
            
            // 执行垃圾回收
            memoryManager.performGarbageCollection();
            
            // 计算回收的内存量
            long afterUsed = memoryStats.getUsedPhysicalMemory();
            long freedMemory = beforeUsed - afterUsed;
            
            log.info("垃圾内存回收完成，回收: {}字节", freedMemory);
            return freedMemory;
        } catch (Exception e) {
            log.error("执行垃圾内存回收失败: {}", e.getMessage(), e);
            throw new MemoryException("执行垃圾内存回收失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查两个内存区域是否重叠
     * @param addr1 第一个区域的起始地址
     * @param size1 第一个区域的大小
     * @param addr2 第二个区域的起始地址
     * @param size2 第二个区域的大小
     * @return 是否重叠
     */
    private boolean isOverlapping(PhysicalAddress addr1, long size1, PhysicalAddress addr2, long size2) {
        long start1 = addr1.getValue();
        long end1 = start1 + size1 - 1;
        long start2 = addr2.getValue();
        long end2 = start2 + size2 - 1;
        
        return !(end1 < start2 || end2 < start1);
    }
} 