package newOs.controller;

import lombok.extern.slf4j.Slf4j;
import newOs.dto.req.MemoryManageDTOs.AccessMemoryReqDTO;
import newOs.dto.req.MemoryManageDTOs.AllocateMemoryReqDTO;
import newOs.dto.req.MemoryManageDTOs.ReleaseMemoryReqDTO;
import newOs.dto.resp.MemoryManageDTOs.MemoryUsageRespDTO;
import newOs.dto.resp.MemoryManageDTOs.PageFaultStatsRespDTO;
import newOs.dto.resp.MemoryManageDTOs.TLBStatsRespDTO;
import newOs.exception.MemoryException;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.service.MemoryAccessService;
import newOs.service.MemoryFileSystemService;
import newOs.service.MemoryManageService;
import newOs.service.ProcessMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内存管理控制器
 * 提供内存管理相关的RESTful API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    @Autowired
    private MemoryManageService memoryManageService;
    
    @Autowired
    private ProcessMemoryService processMemoryService;
    
    @Autowired
    private MemoryAccessService memoryAccessService;
    
    @Autowired
    private MemoryFileSystemService memoryFileSystemService;
    
    /**
     * 获取系统内存使用情况
     * @return 内存使用情况统计
     */
    @GetMapping("/usage")
    public ResponseEntity<MemoryUsageRespDTO> getMemoryUsage() {
        log.info("获取系统内存使用情况");
        
        try {
            // 获取物理内存信息
            Map<String, Object> physicalMemoryInfo = memoryManageService.getPhysicalMemoryInfo();
            
            // 获取虚拟内存信息
            Map<String, Object> virtualMemoryInfo = memoryManageService.getVirtualMemoryInfo();
            
            // 获取交换空间信息
            Map<String, Object> swapSpaceInfo = memoryManageService.getSwapSpaceInfo();
            
            // 获取进程内存使用列表
            List<Map<String, Object>> processMemoryUsages = memoryManageService.listProcessesMemoryUsage();
            
            // 构建响应DTO
            MemoryUsageRespDTO respDTO = new MemoryUsageRespDTO();
            
            // 设置物理内存信息
            respDTO.setTotalPhysicalMemory((Long) physicalMemoryInfo.get("totalSize"));
            respDTO.setUsedPhysicalMemory((Long) physicalMemoryInfo.get("usedSize"));
            respDTO.setFreePhysicalMemory((Long) physicalMemoryInfo.get("freeSize"));
            respDTO.setPhysicalMemoryUsageRatio((Double) physicalMemoryInfo.get("usageRatio"));
            respDTO.setFragmentationIndex((Double) physicalMemoryInfo.get("fragmentationIndex"));
            respDTO.setPageSize((Integer) physicalMemoryInfo.get("pageSize"));
            respDTO.setTotalPageFrames((Integer) physicalMemoryInfo.get("pageFramesCount"));
            respDTO.setUsedPageFrames((Integer) physicalMemoryInfo.get("usedPageFramesCount"));
            respDTO.setFreePageFrames((Integer) physicalMemoryInfo.get("freePageFramesCount"));
            
            // 设置虚拟内存信息
            respDTO.setVirtualMemoryEnabled((Boolean) virtualMemoryInfo.get("enabled"));
            respDTO.setOvercommitRatio((Double) virtualMemoryInfo.get("overcommitRatio"));
            respDTO.setPageReplacementStrategy((String) physicalMemoryInfo.get("pageReplacementStrategy"));
            
            // 设置交换空间信息
            respDTO.setTotalSwapSpace((Long) swapSpaceInfo.get("totalSize"));
            respDTO.setUsedSwapSpace((Long) swapSpaceInfo.get("usedSize"));
            respDTO.setFreeSwapSpace((Long) swapSpaceInfo.get("freeSize"));
            respDTO.setSwapUsageRatio((Double) swapSpaceInfo.get("usageRatio"));
            respDTO.setSwappingThreshold((Integer) swapSpaceInfo.get("swappingThreshold"));
            respDTO.setSwapEnabled((Boolean) swapSpaceInfo.get("enabled"));
            
            // 设置其他属性
            respDTO.setAllocationStrategy("BEST_FIT"); // 假设值，实际应从服务获取
            respDTO.setMemoryUsageAlertThreshold(memoryManageService.getMemoryUsageAlertThreshold());
            respDTO.setLargestFreeBlock(0L); // 假设值，实际应从服务获取
            respDTO.setProtectedRegionsCount(memoryManageService.getProtectedMemoryRegions().size());
            
            // 设置进程内存使用情况
            List<MemoryUsageRespDTO.ProcessMemoryUsage> processUsages = processMemoryUsages.stream()
                    .map(usage -> {
                        MemoryUsageRespDTO.ProcessMemoryUsage processUsage = new MemoryUsageRespDTO.ProcessMemoryUsage();
                        processUsage.setProcessId((Integer) usage.get("processId"));
                        processUsage.setCurrentUsage((Long) usage.get("currentUsage"));
                        processUsage.setPeakUsage((Long) usage.get("peakUsage"));
                        processUsage.setTotalAllocated((Long) usage.get("totalAllocated"));
                        processUsage.setTotalFreed((Long) usage.get("totalFreed"));
                        processUsage.setAllocationCount((Integer) usage.get("allocationCount"));
                        processUsage.setFreeCount((Integer) usage.get("freeCount"));
                        // 其他字段可能需要从其他服务方法获取
                        return processUsage;
                    })
                    .toList();
            
            respDTO.setProcessMemoryUsages(processUsages);
            
            // 设置附加统计信息
            Map<String, Object> additionalStats = new HashMap<>();
            additionalStats.put("gcEnabled", true); // 假设值
            additionalStats.put("lastGCTime", System.currentTimeMillis() - 60000); // 假设值
            additionalStats.put("lastGCFreedMemory", 1024 * 1024L); // 假设值
            respDTO.setAdditionalStats(additionalStats);
            
            return ResponseEntity.ok(respDTO);
        } catch (Exception e) {
            log.error("获取内存使用情况失败：{}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * 获取缺页统计信息
     * @return 缺页统计信息
     */
    @GetMapping("/pagefault/stats")
    public ResponseEntity<PageFaultStatsRespDTO> getPageFaultStats() {
        log.info("获取系统缺页统计信息");
        
        try {
            // 这里需要实现从服务获取缺页统计信息并构建响应DTO
            // 由于缺乏具体实现，这里使用模拟数据
            PageFaultStatsRespDTO respDTO = PageFaultStatsRespDTO.builder()
                    .totalPageFaults(1000L)
                    .pageFaultRate(0.5)
                    .majorPageFaults(200L)
                    .minorPageFaults(800L)
                    .pageInsCount(300L)
                    .pageOutsCount(150L)
                    .averagePageFaultHandlingTimeNanos(5000.0)
                    .thrashingDetected(false)
                    .build();
            
            return ResponseEntity.ok(respDTO);
        } catch (Exception e) {
            log.error("获取缺页统计信息失败：{}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * 获取TLB统计信息
     * @return TLB统计信息
     */
    @GetMapping("/tlb/stats")
    public ResponseEntity<TLBStatsRespDTO> getTLBStats() {
        log.info("获取系统TLB统计信息");
        
        try {
            // 从服务获取TLB统计信息
            Map<String, Object> tlbStats = memoryManageService.getTLBStatistics();
            
            // 构建响应DTO
            TLBStatsRespDTO respDTO = new TLBStatsRespDTO();
            respDTO.setHitCount((Long) tlbStats.get("hitCount"));
            respDTO.setMissCount((Long) tlbStats.get("missCount"));
            respDTO.setTotalAccessCount((Long) tlbStats.get("hitCount") + (Long) tlbStats.get("missCount"));
            respDTO.setHitRatio((Double) tlbStats.get("hitRatio"));
            respDTO.setEntryCount((Integer) tlbStats.get("entryCount"));
            respDTO.setEvictionCount((Long) tlbStats.get("evictionCount"));
            respDTO.setAverageAccessTimeNanos((Double) tlbStats.get("averageAccessTime"));
            
            // 其他字段需要从服务获取，这里使用模拟数据
            respDTO.setEnabled(true);
            respDTO.setMultiLevelTLBEnabled(false);
            respDTO.setPrefetchEnabled(false);
            respDTO.setReplacementPolicy("LRU");
            respDTO.setWritePolicy("WRITE_BACK");
            respDTO.setCoverage("GLOBAL");
            
            return ResponseEntity.ok(respDTO);
        } catch (Exception e) {
            log.error("获取TLB统计信息失败：{}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * 分配内存
     * @param requestDTO 内存分配请求
     * @return 分配结果，包含虚拟地址
     */
    @PostMapping("/allocate")
    public ResponseEntity<Map<String, Object>> allocateMemory(@RequestBody AllocateMemoryReqDTO requestDTO) {
        log.info("内存分配请求：进程ID={}, 大小={}, 优先级={}, 内存类型={}", 
                requestDTO.getProcessId(), requestDTO.getSize(), requestDTO.getPriority(), requestDTO.getMemoryType());
        
        try {
            // 调用进程内存服务分配内存
            // 根据实际服务接口调整参数
            boolean isShared = "SHARED".equals(requestDTO.getMemoryType());
            VirtualAddress virtualAddress = processMemoryService.allocateMemory(
                    requestDTO.getProcessId(), 
                    requestDTO.getSize(),
                    isShared
            );
            
            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("processId", requestDTO.getProcessId());
            result.put("virtualAddress", virtualAddress.getValue());
            result.put("size", requestDTO.getSize());
            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (MemoryException e) {
            log.error("内存分配失败：{}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("processId", requestDTO.getProcessId());
            error.put("size", requestDTO.getSize());
            error.put("success", false);
            error.put("errorCode", "ALLOCATION_FAILED");
            error.put("errorMessage", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 释放内存
     * @param requestDTO 内存释放请求
     * @return 释放结果
     */
    @PostMapping("/release")
    public ResponseEntity<Map<String, Object>> releaseMemory(@RequestBody ReleaseMemoryReqDTO requestDTO) {
        log.info("内存释放请求：进程ID={}, 虚拟地址={}, 大小={}, 类型={}", 
                requestDTO.getProcessId(), requestDTO.getVirtualAddress(), 
                requestDTO.getSize(), requestDTO.getReleaseType());
        
        try {
            // 创建虚拟地址对象
            VirtualAddress virtualAddress = new VirtualAddress(requestDTO.getVirtualAddress());
            
            // 调用进程内存服务释放内存
            processMemoryService.freeMemory(
                    requestDTO.getProcessId(),
                    virtualAddress
            );
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("processId", requestDTO.getProcessId());
            response.put("virtualAddress", requestDTO.getVirtualAddress());
            response.put("size", requestDTO.getSize());
            response.put("success", true);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (MemoryException e) {
            log.error("内存释放失败：{}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("processId", requestDTO.getProcessId());
            error.put("virtualAddress", requestDTO.getVirtualAddress());
            error.put("success", false);
            error.put("errorCode", "RELEASE_FAILED");
            error.put("errorMessage", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 访问内存
     * @param requestDTO 内存访问请求
     * @return 访问结果
     */
    @PostMapping("/access")
    public ResponseEntity<Map<String, Object>> accessMemory(@RequestBody AccessMemoryReqDTO requestDTO) {
        log.info("内存访问请求：进程ID={}, 虚拟地址={}, 访问类型={}, 大小={}", 
                requestDTO.getProcessId(), requestDTO.getVirtualAddress(), 
                requestDTO.getAccessType(), requestDTO.getSize());
        
        try {
            // 创建虚拟地址对象
            VirtualAddress virtualAddress = new VirtualAddress(requestDTO.getVirtualAddress());
            
            // 根据访问类型执行不同操作
            Map<String, Object> result = new HashMap<>();
            result.put("processId", requestDTO.getProcessId());
            result.put("virtualAddress", requestDTO.getVirtualAddress());
            result.put("accessType", requestDTO.getAccessType());
            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());
            
            if ("READ".equals(requestDTO.getAccessType())) {
                // 读取内存
                byte[] data = memoryAccessService.read(
                        requestDTO.getProcessId(),
                        virtualAddress,
                        requestDTO.getSize()
                );
                
                // 将读取的数据转换为十六进制字符串
                StringBuilder hexData = new StringBuilder();
                for (byte b : data) {
                    hexData.append(String.format("%02X", b));
                }
                
                result.put("data", hexData.toString());
                result.put("dataSize", data.length);
            } else if ("WRITE".equals(requestDTO.getAccessType())) {
                // 写入内存
                // 将十六进制字符串转换为字节数组
                String hexData = requestDTO.getWriteData();
                if (hexData != null && hexData.startsWith("0x")) {
                    hexData = hexData.substring(2);
                }
                
                int len = hexData != null ? hexData.length() : 0;
                byte[] data = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    data[i / 2] = (byte) ((Character.digit(hexData.charAt(i), 16) << 4)
                            + Character.digit(hexData.charAt(i + 1), 16));
                }
                
                // 写入内存
                boolean writeResult = memoryAccessService.write(
                        requestDTO.getProcessId(),
                        virtualAddress,
                        data
                );
                
                result.put("written", writeResult);
                result.put("writtenSize", data.length);
            } else if ("EXECUTE".equals(requestDTO.getAccessType())) {
                // 执行内存中的代码
                boolean execResult = memoryAccessService.execute(
                        requestDTO.getProcessId(),
                        virtualAddress,
                        requestDTO.getSize()
                );
                
                result.put("executed", execResult);
            } else {
                result.put("success", false);
                result.put("errorCode", "INVALID_ACCESS_TYPE");
                result.put("errorMessage", "不支持的访问类型: " + requestDTO.getAccessType());
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            return ResponseEntity.ok(result);
        } catch (MemoryException e) {
            log.error("内存访问失败：{}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("processId", requestDTO.getProcessId());
            error.put("virtualAddress", requestDTO.getVirtualAddress());
            error.put("accessType", requestDTO.getAccessType());
            error.put("success", false);
            error.put("errorCode", "ACCESS_FAILED");
            error.put("errorMessage", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 执行内存管理维护操作
     * @param operation 操作类型
     * @return 操作结果
     */
    @PostMapping("/maintenance/{operation}")
    public ResponseEntity<Map<String, Object>> performMaintenanceOperation(@PathVariable String operation) {
        log.info("执行内存管理维护操作：{}", operation);
        
        Map<String, Object> result = new HashMap<>();
        result.put("operation", operation);
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            switch (operation) {
                case "defragment":
                    // 内存碎片整理
                    Map<String, Object> defragResult = memoryManageService.defragmentMemory();
                    result.put("success", true);
                    result.put("details", defragResult);
                    break;
                    
                case "compress":
                    // 内存压缩
                    Map<String, Object> compressResult = memoryManageService.compressPhysicalMemory();
                    result.put("success", true);
                    result.put("details", compressResult);
                    break;
                    
                case "gc":
                    // 垃圾回收
                    long freedMemory = memoryManageService.performGarbageCollection();
                    result.put("success", true);
                    result.put("freedMemory", freedMemory);
                    break;
                    
                case "flush_tlb":
                    // 刷新TLB
                    memoryManageService.flushTLB();
                    result.put("success", true);
                    break;
                    
                case "flush_pagecache":
                    // 刷新页缓存
                    memoryFileSystemService.flushAllPageCache();
                    result.put("success", true);
                    break;
                    
                default:
                    result.put("success", false);
                    result.put("errorCode", "UNKNOWN_OPERATION");
                    result.put("errorMessage", "未知的维护操作: " + operation);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            return ResponseEntity.ok(result);
        } catch (MemoryException e) {
            log.error("执行内存管理维护操作失败：{}", e.getMessage(), e);
            
            result.put("success", false);
            result.put("errorCode", "OPERATION_FAILED");
            result.put("errorMessage", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 设置内存管理配置
     * @param config 配置项名称
     * @param value 配置值
     * @return 设置结果
     */
    @PutMapping("/config/{config}")
    public ResponseEntity<Map<String, Object>> setMemoryConfig(
            @PathVariable String config,
            @RequestParam String value) {
        log.info("设置内存管理配置：{}={}", config, value);
        
        Map<String, Object> result = new HashMap<>();
        result.put("config", config);
        result.put("value", value);
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            switch (config) {
                case "page_size":
                    // 设置页大小
                    int pageSize = Integer.parseInt(value);
                    memoryManageService.setPageSize(pageSize);
                    result.put("success", true);
                    break;
                    
                case "allocation_strategy":
                    // 设置内存分配策略
                    Map<String, Object> strategyParams = new HashMap<>();
                    memoryManageService.configureAllocationStrategy(value, strategyParams);
                    result.put("success", true);
                    break;
                    
                case "page_replacement_strategy":
                    // 设置页面置换策略
                    Map<String, Object> replacementParams = new HashMap<>();
                    memoryManageService.configurePageReplacementStrategy(value, replacementParams);
                    result.put("success", true);
                    break;
                    
                case "memory_alert_threshold":
                    // 设置内存告警阈值
                    int threshold = Integer.parseInt(value);
                    memoryManageService.setMemoryUsageAlertThreshold(threshold);
                    result.put("success", true);
                    break;
                    
                case "swapping_threshold":
                    // 设置交换阈值
                    int swapThreshold = Integer.parseInt(value);
                    memoryManageService.setSwappingThreshold(swapThreshold);
                    result.put("success", true);
                    break;
                    
                case "overcommit_ratio":
                    // 设置内存超额分配比例
                    double ratio = Double.parseDouble(value);
                    memoryManageService.setMemoryOvercommitRatio(ratio);
                    result.put("success", true);
                    break;
                    
                default:
                    result.put("success", false);
                    result.put("errorCode", "UNKNOWN_CONFIG");
                    result.put("errorMessage", "未知的配置项: " + config);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("设置内存管理配置失败：{}", e.getMessage(), e);
            
            result.put("success", false);
            result.put("errorCode", "CONFIG_FAILED");
            result.put("errorMessage", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 获取进程内存使用详情
     * @param processId 进程ID
     * @return 进程内存使用详情
     */
    @GetMapping("/process/{processId}")
    public ResponseEntity<Map<String, Object>> getProcessMemoryUsage(@PathVariable int processId) {
        log.info("获取进程内存使用详情：进程ID={}", processId);
        
        try {
            Map<String, Object> usage = memoryManageService.getProcessMemoryUsage(processId);
            usage.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(usage);
        } catch (MemoryException e) {
            log.error("获取进程内存使用详情失败：{}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("processId", processId);
            error.put("success", false);
            error.put("errorCode", "PROCESS_NOT_FOUND");
            error.put("errorMessage", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * 创建内存转储文件
     * @param processId 进程ID
     * @param filePath 文件路径
     * @return 操作结果
     */
    @PostMapping("/dump")
    public ResponseEntity<Map<String, Object>> createMemoryDump(
            @RequestParam int processId,
            @RequestParam String filePath) {
        log.info("创建内存转储文件：进程ID={}, 文件路径={}", processId, filePath);
        
        try {
            boolean result = memoryManageService.createMemoryDump(filePath, processId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("processId", processId);
            response.put("filePath", filePath);
            response.put("success", result);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (MemoryException e) {
            log.error("创建内存转储文件失败：{}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("processId", processId);
            error.put("filePath", filePath);
            error.put("success", false);
            error.put("errorCode", "DUMP_FAILED");
            error.put("errorMessage", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 初始化内存系统
     * @param physicalSize 物理内存大小
     * @param swapSize 交换空间大小
     * @return 初始化结果
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeMemorySystem(
            @RequestParam long physicalSize,
            @RequestParam(required = false, defaultValue = "0") long swapSize) {
        log.info("初始化内存系统：物理内存大小={}, 交换空间大小={}", physicalSize, swapSize);
        
        try {
            memoryManageService.initializeMemorySystem(physicalSize, swapSize);
            
            Map<String, Object> result = new HashMap<>();
            result.put("physicalSize", physicalSize);
            result.put("swapSize", swapSize);
            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (MemoryException e) {
            log.error("初始化内存系统失败：{}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("physicalSize", physicalSize);
            error.put("swapSize", swapSize);
            error.put("success", false);
            error.put("errorCode", "INITIALIZATION_FAILED");
            error.put("errorMessage", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 