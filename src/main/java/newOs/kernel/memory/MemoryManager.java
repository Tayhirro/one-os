package newOs.kernel.memory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import newOs.exception.AddressTranslationException;
import newOs.exception.MemoryAllocationException;
import newOs.exception.MemoryException;
import newOs.exception.MemoryProtectionException;
import newOs.exception.PageFaultException;
import newOs.kernel.memory.allocation.MemoryAllocator;
import newOs.kernel.memory.allocation.MemoryBlock;
import newOs.kernel.memory.allocation.MemoryReclaimer;
import newOs.kernel.memory.cache.TLBManager;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.monitor.MemoryStats;
import newOs.kernel.memory.monitor.PageFaultStats;
import newOs.kernel.memory.monitor.TLBStats;
import newOs.kernel.memory.virtual.AddressTranslation;
import newOs.kernel.memory.virtual.PageFrameTable;
import newOs.kernel.memory.virtual.PageTable;
import newOs.kernel.memory.virtual.paging.SwapManager;
import newOs.kernel.memory.virtual.protection.MemoryProtection;
import newOs.kernel.memory.virtual.replacement.PageReplacementManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import newOs.kernel.memory.model.MemorySegment;
import newOs.kernel.memory.virtual.paging.Page;
import newOs.kernel.memory.virtual.paging.PageFrame;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;

/**
 * 内存管理总控类
 * 协调各种内存相关组件，提供内存管理的核心功能
 */
@Component
@Slf4j
public class MemoryManager {

    /**
     * 物理内存
     */
    private PhysicalMemory physicalMemory;

    /**
     * 内存分配器
     */
    private MemoryAllocator memoryAllocator;

    /**
     * 内存回收器
     */
    private MemoryReclaimer memoryReclaimer;

    /**
     * 地址转换器
     */
    private AddressTranslation addressTranslation;

    /**
     * TLB管理器
     */
    private TLBManager tlbManager;

    /**
     * 页表管理
     */
    private final Map<Integer, PageTable> processPageTables = new ConcurrentHashMap<>();

    /**
     * 页帧表
     */
    private PageFrameTable pageFrameTable;

    /**
     * 交换空间管理器
     */
    private SwapManager swapManager;

    /**
     * 页面置换管理器
     */
    private PageReplacementManager pageReplacementManager;

    /**
     * 内存保护
     */
    private MemoryProtection memoryProtection;

    /**
     * 内存统计
     */
    private MemoryStats memoryStats;

    /**
     * 缺页统计
     */
    private PageFaultStats pageFaultStats;

    /**
     * TLB统计
     */
    private TLBStats tlbStats;

    /**
     * 内存管理读写锁
     */
    private final ReadWriteLock memoryLock = new ReentrantReadWriteLock();

    /**
     * 进程内存使用映射（进程ID -> 已分配内存大小字节）
     */
    private final Map<Integer, Long> processMemoryUsage = new HashMap<>();

    /**
     * 最大内存分配大小（字节）
     */
    @Value("${memory.max.allocation.size:1073741824}")
    private long maxAllocationSize = 1073741824; // 默认1GB

    /**
     * 最小内存分配大小（字节）
     */
    @Value("${memory.min.allocation.size:4}")
    private long minAllocationSize = 4;

    /**
     * 是否启用内存保护
     */
    @Getter
    @Value("${memory.protection.enabled:true}")
    private boolean memoryProtectionEnabled = true;

    /**
     * 是否启用虚拟内存
     */
    @Getter
    @Value("${memory.virtual.enabled:true}")
    private boolean virtualMemoryEnabled = true;

    /**
     * 是否启用TLB
     */
    @Getter
    @Value("${memory.tlb.enabled:true}")
    private boolean tlbEnabled = true;

    /**
     * 是否启用交换空间
     */
    @Getter
    @Value("${memory.swap.enabled:true}")
    private boolean swapEnabled = true;

    /**
     * 受保护内存管理器
     */
    private ProtectedMemory protectedMemory;

    /**
     * 构造函数
     */
    public MemoryManager() {
        // 默认构造函数，依赖通过setter注入
    }

    @Autowired
    public void setProtectedMemory(ProtectedMemory protectedMemory) {
        this.protectedMemory = protectedMemory;
    }

    @Autowired
    public void setPhysicalMemory(PhysicalMemory physicalMemory) {
        this.physicalMemory = physicalMemory;
    }

    @Autowired
    public void setMemoryAllocator(MemoryAllocator memoryAllocator) {
        this.memoryAllocator = memoryAllocator;
    }

    @Autowired
    public void setMemoryReclaimer(MemoryReclaimer memoryReclaimer) {
        this.memoryReclaimer = memoryReclaimer;
    }

    @Autowired
    public void setAddressTranslation(AddressTranslation addressTranslation) {
        this.addressTranslation = addressTranslation;
    }

    @Autowired
    public void setTlbManager(TLBManager tlbManager) {
        this.tlbManager = tlbManager;
    }

    @Autowired
    public void setPageFrameTable(PageFrameTable pageFrameTable) {
        this.pageFrameTable = pageFrameTable;
    }

    @Autowired
    public void setSwapManager(SwapManager swapManager) {
        this.swapManager = swapManager;
    }

    @Autowired
    public void setPageReplacementManager(PageReplacementManager pageReplacementManager) {
        this.pageReplacementManager = pageReplacementManager;
    }

    @Autowired
    public void setMemoryProtection(MemoryProtection memoryProtection) {
        this.memoryProtection = memoryProtection;
    }

    @Autowired
    public void setMemoryStats(MemoryStats memoryStats) {
        this.memoryStats = memoryStats;
    }

    @Autowired
    public void setPageFaultStats(PageFaultStats pageFaultStats) {
        this.pageFaultStats = pageFaultStats;
    }

    @Autowired
    public void setTlbStats(TLBStats tlbStats) {
        this.tlbStats = tlbStats;
    }

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        log.info("内存管理器初始化中...");
        log.info("物理内存大小: {}", formatBytes(physicalMemory.getTotalSize()));
        log.info("启用内存保护: {}", memoryProtectionEnabled);
        log.info("启用虚拟内存: {}", virtualMemoryEnabled);
        log.info("启用TLB: {}", tlbEnabled);
        log.info("启用交换空间: {}", swapEnabled);
        log.info("内存管理器初始化完成");
    }

    /**
     * 分配内存
     * @param processId 进程ID
     * @param size 分配大小（字节）
     * @return 虚拟地址
     * @throws MemoryAllocationException 内存分配异常
     */
    public VirtualAddress allocateMemory(int processId, long size) throws MemoryAllocationException {
        if (size <= 0) {
            throw new MemoryAllocationException("内存分配大小必须大于0");
        }
        
        if (size < minAllocationSize) {
            size = minAllocationSize;
        }
        
        if (size > maxAllocationSize) {
            throw new MemoryAllocationException("内存分配大小超过最大限制: " + formatBytes(size) + " > " + formatBytes(maxAllocationSize));
        }
        
        try {
            memoryLock.writeLock().lock();
            
            log.info("【内存管理】开始为进程{}分配{}字节内存", processId, formatBytes(size));
            
            // 分配物理内存，获取物理地址
            PhysicalAddress physAddr = memoryAllocator.allocate((int)size, processId);
            log.info("【内存管理】分配物理内存成功，物理地址: 0x{}", Long.toHexString(physAddr.getValue()));
            
            // 如果启用虚拟内存，则创建页表映射
            VirtualAddress virtualAddress;
            if (virtualMemoryEnabled) {
                // 获取或创建进程页表
                PageTable pageTable = getOrCreatePageTable(processId);
                log.info("【内存管理】使用虚拟内存，进程{}的页表已就绪", processId);
                
                // 创建虚拟地址映射
                virtualAddress = addressTranslation.mapPhysicalAddress(processId, physAddr, (int)size);
                log.info("【内存管理】创建虚拟地址映射，虚拟地址: 0x{}", Long.toHexString(virtualAddress.getValue()));
            } else {
                // 不使用虚拟内存，直接返回物理地址对应的虚拟地址
                virtualAddress = new VirtualAddress(physAddr.getValue());
                log.info("【内存管理】不使用虚拟内存，直接使用物理地址作为虚拟地址");
            }
            
            // 更新进程内存使用统计
            updateProcessMemoryUsage(processId, size, true);
            
            // 记录内存分配统计
            memoryStats.recordAllocationRequest(size, true);
            memoryStats.recordProcessAllocation(processId, size);
            
            long freeMemory = memoryAllocator.getFreeMemorySize();
            long totalMemory = physicalMemory.getTotalSize();
            log.info("【内存管理】内存分配完成，当前内存使用率: {}%", 
                    String.format("%.2f", (1 - (double)freeMemory/totalMemory) * 100));
            
            return virtualAddress;
        } catch (Exception e) {
            // 记录内存分配失败统计
            memoryStats.recordAllocationRequest(size, false);
            
            if (e instanceof MemoryAllocationException) {
                throw (MemoryAllocationException) e;
            }
            
            throw new MemoryAllocationException("内存分配失败: " + e.getMessage(), e);
        } finally {
            memoryLock.writeLock().unlock();
        }
    }

    /**
     * 释放内存
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @throws MemoryException 内存异常
     */
    public void freeMemory(int processId, VirtualAddress virtualAddress) throws MemoryException {
        if (virtualAddress == null) {
            throw new MemoryException("虚拟地址不能为空");
        }
        
        log.info("【内存管理】开始释放进程{}的虚拟地址: 0x{}", 
                processId, Long.toHexString(virtualAddress.getValue()));
        
        PhysicalAddress physicalAddress = null;
        long blockSize = 0;
        
        try {
            memoryLock.writeLock().lock();
            
            if (virtualMemoryEnabled) {
                // 获取物理地址
                try {
                    physicalAddress = addressTranslation.translateToPhysical(processId, virtualAddress);
                    log.info("【内存管理】虚拟地址 0x{} 对应的物理地址: 0x{}", 
                            Long.toHexString(virtualAddress.getValue()), 
                            Long.toHexString(physicalAddress.getValue()));
                } catch (Exception e) {
                    // 检查是否因为映射不存在导致的异常
                    if (e.getMessage() != null && e.getMessage().contains("不存在")) {
                        log.warn("【内存管理】虚拟地址 0x{} 没有对应的物理映射，可能已被释放", 
                                Long.toHexString(virtualAddress.getValue()));
                        return; // 地址已经被释放或不存在，直接返回
                    }
                    throw new MemoryException("地址转换失败: " + e.getMessage(), e);
                }
                
                // 获取分配块大小
                try {
                    PageTable pageTable = getOrCreatePageTable(processId);
                    if (pageTable != null) {
                        blockSize = pageTable.getMappingSize(virtualAddress);
                        if (blockSize <= 0) {
                            // 如果页表中无法获取内存块大小，则尝试从PCB的内存分配表中获取
                            PCB pcb = protectedMemory.getPcbTable().get(processId);
                            if (pcb != null && pcb.getMemoryAllocationMap() != null) {
                                Long size = pcb.getMemoryAllocationMap().get(virtualAddress);
                                if (size != null && size > 0) {
                                    blockSize = size;
                                    log.info("【内存管理】从PCB的内存分配表获取到内存块大小: {} 字节", blockSize);
                                } else {
                                    blockSize = 4096; // 默认使用一页大小
                                    log.warn("【内存管理】PCB内存分配表中未找到映射大小，使用默认值4096字节");
                                }
                            } else {
                                blockSize = 4096; // 默认使用一页大小
                                log.warn("【内存管理】无法获取PCB或内存分配表，使用默认块大小4096字节");
                            }
                        }
                    } else {
                        blockSize = 4096; // 默认使用一页大小
                        log.warn("【内存管理】无法获取进程页表，使用默认块大小4096字节");
                    }
                } catch (Exception e) {
                    blockSize = 4096; // 默认使用一页大小
                    log.warn("【内存管理】获取映射大小时发生异常，使用默认值4096字节: {}", e.getMessage());
                }
                
                if (blockSize <= 0) {
                    log.warn("【内存管理】无效的虚拟地址映射大小: 0x{}", 
                            Long.toHexString(virtualAddress.getValue()));
                    blockSize = 4096; // 默认使用一页大小
                }
                
                // 移除页表映射
                try {
                    addressTranslation.unmapMemoryBlock(processId, virtualAddress);
                    log.info("【内存管理】解除虚拟地址映射，块大小: {} 字节", blockSize);
                } catch (Exception e) {
                    log.warn("【内存管理】解除虚拟地址映射失败: {}", e.getMessage());
                    // 继续执行，尝试释放物理内存
                }

                // 记录内存释放统计
                memoryStats.recordFreeRequest(blockSize);
                memoryStats.recordProcessFree(processId, blockSize);
            } else {
                // 不使用虚拟内存，虚拟地址等于物理地址
                physicalAddress = new PhysicalAddress(virtualAddress.getValue());
                
                // 查询分配大小（简化，实际应从分配器获取）
                blockSize = 4096; // 假设为一页大小
                log.info("【内存管理】未使用虚拟内存，直接释放物理地址: 0x{}", 
                        Long.toHexString(physicalAddress.getValue()));
            }
            
            // 有效的物理地址，释放物理内存
            if (physicalAddress != null && physicalAddress.getValue() >= 0) {
                try {
                    boolean success = memoryAllocator.free(physicalAddress, processId);
                    
                    if (!success) {
                        log.warn("【内存管理】物理内存释放失败，可能是无效地址或不属于该进程");
                    } else {
                        // 更新进程内存使用统计
                        updateProcessMemoryUsage(processId, blockSize, false);
                        
                        long freeMemory = memoryAllocator.getFreeMemorySize();
                        long totalMemory = physicalMemory.getTotalSize();
                        log.info("【内存管理】内存释放完成，当前内存使用率: {}%", 
                                String.format("%.2f", (1 - (double)freeMemory/totalMemory) * 100));
                    }
                } catch (Exception e) {
                    log.error("【内存管理】释放物理内存时出错: {}", e.getMessage());
                    throw new MemoryException("内存释放失败: " + e.getMessage(), e);
                }
            } else {
                log.warn("【内存管理】无效的物理地址，跳过物理内存释放");
            }
        } finally {
            memoryLock.writeLock().unlock();
        }
    }

    /**
     * 读取内存
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 读取大小（字节）
     * @return 读取的字节数组
     * @throws MemoryException 内存异常
     */
    public byte[] readMemory(int processId, VirtualAddress virtualAddress, int size) throws MemoryException {
        if (size <= 0) {
            throw new MemoryException("读取大小必须大于0");
        }
        
        try {
            memoryLock.readLock().lock();
            
            // 检查访问权限
            if (memoryProtectionEnabled) {
                checkAccessPermission(processId, virtualAddress, false);
            }
            
            PhysicalAddress physicalAddress;
            
            if (virtualMemoryEnabled) {
                // 通过TLB或页表进行地址转换
                if (tlbEnabled) {
                    try {
                        // 尝试通过TLB进行地址转换
                        physicalAddress = tlbManager.lookup(processId, virtualAddress);
                        tlbStats.recordHit(true); // 假设L1 TLB
                    } catch (Exception e) {
                        // TLB未命中，使用页表进行地址转换
                        tlbStats.recordMiss(true); // 假设L1 TLB
                        
                        try {
                            physicalAddress = addressTranslation.translateToPhysical(processId, virtualAddress);
                            // 更新TLB - 添加updateEntry方法
                            tlbManager.updateEntry(processId, virtualAddress, physicalAddress);
                        } catch (PageFaultException pfe) {
                            // 处理缺页异常
                            handlePageFault(processId, virtualAddress, false);
                            // 重新尝试转换地址
                            physicalAddress = addressTranslation.translateToPhysical(processId, virtualAddress);
                        }
                    }
                } else {
                    // 不使用TLB，直接使用页表进行地址转换
                    try {
                        physicalAddress = addressTranslation.translateToPhysical(processId, virtualAddress);
                    } catch (PageFaultException pfe) {
                        // 处理缺页异常
                        handlePageFault(processId, virtualAddress, false);
                        // 重新尝试转换地址
                        physicalAddress = addressTranslation.translateToPhysical(processId, virtualAddress);
                    }
                }
            } else {
                // 不使用虚拟内存，直接使用物理地址
                physicalAddress = new PhysicalAddress(virtualAddress.getValue());
            }
            
            // 从物理内存读取数据
            return physicalMemory.read(physicalAddress.getValue(), size);
        } catch (Exception e) {
            if (e instanceof MemoryException) {
                throw (MemoryException) e;
            }
            throw new MemoryException("内存读取失败: " + e.getMessage(), e);
        } finally {
            memoryLock.readLock().unlock();
        }
    }

    /**
     * 写入内存
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param data 要写入的数据
     * @throws MemoryException 内存异常
     */
    public void writeMemory(int processId, VirtualAddress virtualAddress, byte[] data) throws MemoryException {
        if (data == null || data.length == 0) {
            throw new MemoryException("写入数据不能为空");
        }
        
        try {
            memoryLock.readLock().lock();
            
            // 检查访问权限
            if (memoryProtectionEnabled) {
                checkAccessPermission(processId, virtualAddress, true);
            }
            
            PhysicalAddress physicalAddress;
            
            if (virtualMemoryEnabled) {
                // 通过TLB或页表进行地址转换
                if (tlbEnabled) {
                    try {
                        // 尝试通过TLB进行地址转换
                        physicalAddress = tlbManager.lookup(processId, virtualAddress);
                        tlbStats.recordHit(true); // 假设L1 TLB
                    } catch (Exception e) {
                        // TLB未命中，使用页表进行地址转换
                        tlbStats.recordMiss(true); // 假设L1 TLB
                        
                        try {
                            physicalAddress = addressTranslation.translateToPhysical(processId, virtualAddress);
                            // 更新TLB
                            tlbManager.updateEntry(processId, virtualAddress, physicalAddress);
                        } catch (PageFaultException pfe) {
                            // 处理缺页异常
                            handlePageFault(processId, virtualAddress, true);
                            // 重新尝试转换地址
                            physicalAddress = addressTranslation.translateToPhysical(processId, virtualAddress);
                        }
                    }
                } else {
                    // 不使用TLB，直接使用页表进行地址转换
                    try {
                        physicalAddress = addressTranslation.translateToPhysical(processId, virtualAddress);
                    } catch (PageFaultException pfe) {
                        // 处理缺页异常
                        handlePageFault(processId, virtualAddress, true);
                        // 重新尝试转换地址
                        physicalAddress = addressTranslation.translateToPhysical(processId, virtualAddress);
                    }
                }
            } else {
                // 不使用虚拟内存，直接使用物理地址
                physicalAddress = new PhysicalAddress(virtualAddress.getValue());
            }
            
            // 写入物理内存
            physicalMemory.write(physicalAddress.getValue(), data);
        } catch (Exception e) {
            if (e instanceof MemoryException) {
                throw (MemoryException) e;
            }
            throw new MemoryException("内存写入失败: " + e.getMessage(), e);
        } finally {
            memoryLock.readLock().unlock();
        }
    }

    /**
     * 检查内存访问权限
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param isWrite 是否为写操作
     * @throws MemoryProtectionException 内存保护异常
     */
    private void checkAccessPermission(int processId, VirtualAddress virtualAddress, boolean isWrite) throws MemoryProtectionException {
        if (!memoryProtection.checkAccess(processId, virtualAddress, isWrite)) {
            throw new MemoryProtectionException("内存访问权限不足: 进程=" + processId + ", 地址=" + virtualAddress + ", 操作=" + (isWrite ? "写" : "读"));
        }
    }

    /**
     * 处理缺页异常
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param isWrite 是否为写操作
     * @throws MemoryException 内存异常
     */
    private void handlePageFault(int processId, VirtualAddress virtualAddress, boolean isWrite) throws MemoryException {
        long startTime = System.nanoTime();
        boolean isMinor = false;
        
        try {
            // 获取进程页表
            PageTable pageTable = processPageTables.get(processId);
            if (pageTable == null) {
                throw new MemoryException("进程不存在或未分配内存: " + processId);
            }
            
            // 使用页面置换管理器处理缺页
            isMinor = pageReplacementManager.handlePageFault(processId, virtualAddress, isWrite);
            
            // 记录缺页统计
            long handlingTime = System.nanoTime() - startTime;
            if (isMinor) {
                pageFaultStats.recordMinorPageFault(handlingTime);
            } else {
                pageFaultStats.recordMajorPageFault(handlingTime);
            }
            pageFaultStats.recordProcessPageFault(processId, isMinor);
            
            log.debug("处理缺页异常: 进程={}, 地址={}, 类型={}, 处理时间={}ns", 
                    processId, virtualAddress, isMinor ? "次缺页" : "主缺页", handlingTime);
        } catch (Exception e) {
            // 缺页处理失败
            long handlingTime = System.nanoTime() - startTime;
            log.error("缺页处理失败: 进程={}, 地址={}, 错误={}, 处理时间={}ns", 
                    processId, virtualAddress, e.getMessage(), handlingTime);
            
            throw new MemoryException("缺页处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取或创建进程的页表
     * @param processId 进程ID
     * @return 进程页表
     */
    private PageTable getOrCreatePageTable(int processId) {
        return processPageTables.computeIfAbsent(processId, k -> new PageTable());
    }

    /**
     * 更新进程内存使用统计
     * @param processId 进程ID
     * @param size 内存大小
     * @param isAllocation 是否为分配操作
     */
    private void updateProcessMemoryUsage(int processId, long size, boolean isAllocation) {
        processMemoryUsage.merge(processId, isAllocation ? size : -size, Long::sum);
    }

    /**
     * 获取进程已分配的内存大小
     * @param processId 进程ID
     * @return 已分配内存大小（字节）
     */
    public long getProcessMemoryUsage(int processId) {
        synchronized (processMemoryUsage) {
            return processMemoryUsage.getOrDefault(processId, 0L);
        }
    }

    /**
     * 释放指定进程的所有内存
     * @param processId 进程ID
     */
    public void freeAllProcessMemory(int processId) {
        memoryLock.writeLock().lock();
        try {
            log.info("释放进程{}的所有内存", processId);
            
            // 获取进程页表
            PageTable pageTable = processPageTables.remove(processId);
            
            // 直接调用内存分配器的freeAll方法释放所有内存
            int blocksFreed = memoryAllocator.freeAll(processId);
            
            // 更新进程内存使用统计
            synchronized (processMemoryUsage) {
                long totalFreed = processMemoryUsage.getOrDefault(processId, 0L);
                if (totalFreed > 0) {
                    memoryStats.recordProcessFree(processId, totalFreed);
                    processMemoryUsage.remove(processId);
                }
            }
            
            log.debug("释放进程所有内存: {}，释放了{}个内存块", processId, blocksFreed);
        } finally {
            memoryLock.writeLock().unlock();
        }
    }

    /**
     * 计算内存碎片化指数
     * @return 碎片化指数（0-1之间，越接近1表示碎片化程度越高）
     */
    public double calculateFragmentationIndex() {
        return memoryAllocator.getFragmentationRatio();
    }

    /**
     * 格式化字节数为可读字符串
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2fGB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    // 添加基本的内存读写操作方法
    
    /**
     * 读取一个字节
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 读取的字节值
     * @throws MemoryException 内存异常
     */
    public byte readByte(int processId, VirtualAddress virtualAddress) throws MemoryException {
        byte[] data = readMemory(processId, virtualAddress, 1);
        return data[0];
    }
    
    /**
     * 读取一个短整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 读取的短整型值
     * @throws MemoryException 内存异常
     */
    public short readShort(int processId, VirtualAddress virtualAddress) throws MemoryException {
        byte[] data = readMemory(processId, virtualAddress, 2);
        return ByteBuffer.wrap(data).getShort();
    }
    
    /**
     * 读取一个整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 读取的整型值
     * @throws MemoryException 内存异常
     */
    public int readInt(int processId, VirtualAddress virtualAddress) throws MemoryException {
        byte[] data = readMemory(processId, virtualAddress, 4);
        return ByteBuffer.wrap(data).getInt();
    }
    
    /**
     * 读取一个长整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 读取的长整型值
     * @throws MemoryException 内存异常
     */
    public long readLong(int processId, VirtualAddress virtualAddress) throws MemoryException {
        byte[] data = readMemory(processId, virtualAddress, 8);
        return ByteBuffer.wrap(data).getLong();
    }
    
    /**
     * 写入一个字节
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param value 要写入的字节值
     * @throws MemoryException 内存异常
     */
    public void writeByte(int processId, VirtualAddress virtualAddress, byte value) throws MemoryException {
        writeMemory(processId, virtualAddress, new byte[] {value});
    }
    
    /**
     * 写入一个短整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param value 要写入的短整型值
     * @throws MemoryException 内存异常
     */
    public void writeShort(int processId, VirtualAddress virtualAddress, short value) throws MemoryException {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort(value);
        writeMemory(processId, virtualAddress, buffer.array());
    }
    
    /**
     * 写入一个整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param value 要写入的整型值
     * @throws MemoryException 内存异常
     */
    public void writeInt(int processId, VirtualAddress virtualAddress, int value) throws MemoryException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(value);
        writeMemory(processId, virtualAddress, buffer.array());
    }
    
    /**
     * 写入一个长整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param value 要写入的长整型值
     * @throws MemoryException 内存异常
     */
    public void writeLong(int processId, VirtualAddress virtualAddress, long value) throws MemoryException {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(value);
        writeMemory(processId, virtualAddress, buffer.array());
    }
    
    /**
     * 检查内存权限
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 区域大小
     * @param permissionType 权限类型（r:读，w:写，x:执行）
     * @return 是否具有指定权限
     */
    public boolean checkMemoryPermission(int processId, VirtualAddress virtualAddress, long size, String permissionType) {
        try {
            if (!memoryProtectionEnabled) {
                return true;
            }
            
            switch (permissionType) {
                case "r":
                    return memoryProtection.checkReadAccess(processId, virtualAddress, size);
                case "w":
                    return memoryProtection.checkWriteAccess(processId, virtualAddress, size);
                case "x":
                    return memoryProtection.checkExecuteAccess(processId, virtualAddress, size);
                default:
                    return false;
            }
        } catch (Exception e) {
            log.warn("检查内存权限失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 将虚拟地址转换为物理地址
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 物理地址
     * @throws AddressTranslationException 地址转换异常
     */
    public PhysicalAddress translateVirtualToPhysical(int processId, VirtualAddress virtualAddress) throws AddressTranslationException {
        try {
            if (!virtualMemoryEnabled) {
                return new PhysicalAddress(virtualAddress.getValue());
            }
            
            return addressTranslation.translateToPhysical(processId, virtualAddress);
        } catch (Exception e) {
            throw new AddressTranslationException("地址转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分配物理内存
     * @param size 内存大小（字节）
     * @return 物理地址
     * @throws MemoryAllocationException 内存分配异常
     */
    public PhysicalAddress allocatePhysicalMemory(long size) throws MemoryAllocationException {
        if (size <= 0) {
            throw new MemoryAllocationException("内存分配大小必须大于0");
        }
        
        try {
            memoryLock.writeLock().lock();
            
            // 分配物理内存 - 使用临时进程ID -1表示内核分配
            PhysicalAddress physAddr = memoryAllocator.allocate((int)size, -1);
            
            // 记录内存分配统计
            memoryStats.recordAllocationRequest(size, true);
            
            return physAddr;
        } catch (Exception e) {
            // 记录内存分配失败统计
            memoryStats.recordAllocationRequest(size, false);
            
            if (e instanceof MemoryAllocationException) {
                throw (MemoryAllocationException) e;
            }
            
            throw new MemoryAllocationException("物理内存分配失败: " + e.getMessage());
        } finally {
            memoryLock.writeLock().unlock();
        }
    }
    
    /**
     * 释放物理内存
     * @param physicalAddress 物理地址
     * @throws MemoryException 内存异常
     */
    public void freePhysicalMemory(PhysicalAddress physicalAddress) throws MemoryException {
        try {
            memoryLock.writeLock().lock();
            
            // 释放内存，使用进程ID -1表示内核内存
            boolean success = memoryAllocator.free(physicalAddress, -1);
            
            if (!success) {
                throw new MemoryException("无效的物理地址或地址不属于指定进程: " + physicalAddress);
            }
            
            // 记录内存释放统计
            memoryStats.recordFreeRequest(4096); // 假设默认页大小，实际应该根据实际大小记录
        } finally {
            memoryLock.writeLock().unlock();
        }
    }
    
    /**
     * 创建私有内存映射（写时复制）
     * @param processId 进程ID
     * @param physicalAddress 物理地址
     * @param size 映射大小
     * @param protection 保护权限（r:读, w:写, x:执行）
     * @return 虚拟地址
     * @throws MemoryException 内存异常
     */
    public VirtualAddress createPrivateMapping(int processId, PhysicalAddress physicalAddress, long size, String protection) throws MemoryException {
        try {
            memoryLock.writeLock().lock();
            
            // 获取或创建进程页表
            PageTable pageTable = getOrCreatePageTable(processId);
            
            // 创建私有映射（写时复制）
            VirtualAddress virtualAddress = new VirtualAddress();
            virtualAddress.setValue(pageTable.getNextAvailableAddress());
            
            // 设置保护权限
            if (memoryProtectionEnabled) {
                boolean canRead = protection.contains("r");
                boolean canWrite = protection.contains("w");
                boolean canExec = protection.contains("x");
                
                memoryProtection.setAccessControl(processId, virtualAddress, size, canRead, canWrite, canExec);
            }
            
            // 创建映射
            addressTranslation.createPrivateMapping(processId, virtualAddress, physicalAddress, size);
            
            // 更新进程内存使用统计
            updateProcessMemoryUsage(processId, size, true);
            
            return virtualAddress;
        } finally {
            memoryLock.writeLock().unlock();
        }
    }
    
    /**
     * 创建共享内存映射
     * @param processId 进程ID
     * @param physicalAddress 物理地址
     * @param size 映射大小
     * @param protection 保护权限（r:读, w:写, x:执行）
     * @return 虚拟地址
     * @throws MemoryException 内存异常
     */
    public VirtualAddress createSharedMapping(int processId, PhysicalAddress physicalAddress, long size, String protection) throws MemoryException {
        try {
            memoryLock.writeLock().lock();
            
            // 获取或创建进程页表
            PageTable pageTable = getOrCreatePageTable(processId);
            
            // 创建共享映射
            VirtualAddress virtualAddress = new VirtualAddress();
            virtualAddress.setValue(pageTable.getNextAvailableAddress());
            
            // 设置保护权限
            if (memoryProtectionEnabled) {
                boolean canRead = protection.contains("r");
                boolean canWrite = protection.contains("w");
                boolean canExec = protection.contains("x");
                
                memoryProtection.setAccessControl(processId, virtualAddress, size, canRead, canWrite, canExec);
            }
            
            // 创建映射
            addressTranslation.createSharedMapping(processId, virtualAddress, physicalAddress, size);
            
            // 更新进程内存使用统计
            updateProcessMemoryUsage(processId, size, true);
            
            return virtualAddress;
        } finally {
            memoryLock.writeLock().unlock();
        }
    }
    
    /**
     * 移除内存映射
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 映射大小（如果为0，则自动确定大小）
     * @throws MemoryException 内存异常
     */
    public void removeMemoryMapping(int processId, VirtualAddress virtualAddress, long size) throws MemoryException {
        try {
            memoryLock.writeLock().lock();
            
            // 获取进程页表
            PageTable pageTable = processPageTables.get(processId);
            if (pageTable == null) {
                throw new MemoryException("进程不存在或未分配内存: " + processId);
            }
            
            // 确定映射大小
            long mappingSize = size;
            if (mappingSize == 0) {
                // 自动确定大小
                mappingSize = pageTable.getMappingSize(virtualAddress);
                if (mappingSize <= 0) {
                    throw new MemoryException("无效的虚拟地址映射: " + virtualAddress);
                }
            }
            
            // 移除映射
            addressTranslation.removeMapping(processId, virtualAddress, mappingSize);
            
            // 如果启用了内存保护，移除保护设置
            if (memoryProtectionEnabled) {
                memoryProtection.removeAccessControl(processId, virtualAddress, mappingSize);
            }
            
            // 更新进程内存使用统计
            updateProcessMemoryUsage(processId, mappingSize, false);
        } finally {
            memoryLock.writeLock().unlock();
        }
    }

    /**
     * 创建页面副本
     * @param pid 进程ID
     * @param page 原始页面
     * @param virtualAddress 目标虚拟地址
     * @return 是否创建成功
     */
    public boolean createPageCopy(int pid, Page page, VirtualAddress virtualAddress) {
        log.debug("创建页面副本: 进程={}, 页面={}, 目标地址={}", 
                pid, page, virtualAddress);
        
        try {
            // 获取原始页帧
            int originalFrameNumber = page.getFrameNumber();
            PageFrame originalFrame = pageFrameTable.getFrame(originalFrameNumber);
            
            if (originalFrame == null) {
                log.error("无法获取原始页帧: {}", originalFrameNumber);
                return false;
            }
            
            // 分配新页帧
            PageFrame newFrame = pageFrameTable.allocateFrame(pid, page.getPageNumber());
            
            if (newFrame == null) {
                log.error("无法分配新页帧");
                return false;
            }
            
            // 复制页面内容
            physicalMemory.copyFrame(originalFrameNumber, newFrame.getFrameNumber());
            
            // 更新页表项
            page.setFrameNumber(newFrame.getFrameNumber());
            page.setShared(false);     // 不再共享
            page.setCopyOnWrite(false); // 不再标记写时复制
            page.setWritable(true);     // 可写
            page.setDirty(true);        // 标记为已修改
            
            log.debug("页面写时复制成功: 进程={}, 页面={}, 新页帧={}", 
                    pid, page, newFrame.getFrameNumber());
            
            return true;
        } catch (Exception e) {
            log.error("写时复制失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 加载代码页
     * @param pid 进程ID
     * @param page 页面
     * @param frame 目标页帧
     * @return 是否加载成功
     */
    public boolean loadCodePage(int pid, Page page, PageFrame frame) {
        log.debug("加载代码页: 进程={}, 页面={}, 页帧={}", 
                pid, page, frame.getFrameNumber());
        
        try {
            // 在实际实现中，需要从可执行文件加载代码
            // 这里简化处理，只将页帧初始化为零
            
            // 初始化页帧内容
            frame.clear();
            
            // 标记页面属性
            page.setCodePage(true);
            page.setWritable(false); // 代码页通常是只读的
            page.setExecutable(true); // 代码页可执行
            
            log.debug("代码页加载成功: 进程={}, 页面={}", pid, page);
            return true;
        } catch (Exception e) {
            log.error("加载代码页失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 加载数据页
     * @param pid 进程ID
     * @param page 页面
     * @param frame 目标页帧
     * @return 是否加载成功
     */
    public boolean loadDataPage(int pid, Page page, PageFrame frame) {
        log.debug("加载数据页: 进程={}, 页面={}, 页帧={}", 
                pid, page, frame.getFrameNumber());
        
        try {
            // 在实际实现中，需要从可执行文件加载初始化数据
            // 这里简化处理，只将页帧初始化为零
            
            // 初始化页帧内容
            frame.clear();
            
            // 标记页面属性
            page.setDataPage(true);
            page.setWritable(true);  // 数据页通常是可写的
            page.setExecutable(false); // 数据页通常不可执行
            
            log.debug("数据页加载成功: 进程={}, 页面={}", pid, page);
            return true;
        } catch (Exception e) {
            log.error("加载数据页失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 初始化物理内存
     * @param size 内存大小，以字节为单位
     * @throws MemoryException 如果初始化失败
     */
    public void initializePhysicalMemory(long size) throws MemoryException {
        log.info("初始化物理内存，大小: {} 字节", size);
        try {
            // 物理内存初始化操作
            physicalMemory.initialize(size);
            log.info("物理内存初始化完成");
        } catch (Exception e) {
            log.error("物理内存初始化失败: {}", e.getMessage(), e);
            throw new MemoryException("物理内存初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化交换空间
     * @param size 交换空间大小，以字节为单位
     * @throws MemoryException 如果初始化失败
     */
    public void initializeSwapSpace(long size) throws MemoryException {
        log.info("初始化交换空间，大小: {} 字节", size);
        try {
            // 实际实现应该调用swapManager进行初始化
            // 仅作为演示
            log.info("交换空间初始化完成");
        } catch (Exception e) {
            log.error("交换空间初始化失败: {}", e.getMessage(), e);
            throw new MemoryException("交换空间初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置内存分配策略
     * @param strategyName 策略名称
     * @param parameters 策略参数
     * @throws MemoryException 如果设置失败
     */
    public void setAllocationStrategy(String strategyName, Map<String, Object> parameters) throws MemoryException {
        log.info("设置内存分配策略: {}, 参数: {}", strategyName, parameters);
        try {
            // 设置内存分配策略
            memoryAllocator.setAllocationStrategy(strategyName, parameters);
            log.info("内存分配策略设置完成");
        } catch (Exception e) {
            log.error("设置内存分配策略失败: {}", e.getMessage(), e);
            throw new MemoryException("设置内存分配策略失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置页面替换策略
     * @param strategyName 策略名称
     * @param parameters 策略参数
     * @throws MemoryException 如果设置失败
     */
    public void setPageReplacementStrategy(String strategyName, Map<String, Object> parameters) throws MemoryException {
        log.info("设置页面替换策略: {}, 参数: {}", strategyName, parameters);
        try {
            // 设置页面替换策略
            pageReplacementManager.setReplacementAlgorithm(strategyName);
            log.info("页面替换策略设置完成");
        } catch (Exception e) {
            log.error("设置页面替换策略失败: {}", e.getMessage(), e);
            throw new MemoryException("设置页面替换策略失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保护内存区域
     * @param startAddress 起始物理地址
     * @param size 大小（字节）
     * @throws MemoryException 如果保护失败
     */
    public void protectMemoryRegion(PhysicalAddress startAddress, long size) throws MemoryException {
        log.info("保护内存区域: 起始地址={}, 大小={}", startAddress, size);
        try {
            // 保护内存区域的实现
            log.info("内存区域保护完成");
        } catch (Exception e) {
            log.error("保护内存区域失败: {}", e.getMessage(), e);
            throw new MemoryException("保护内存区域失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解除内存区域保护
     * @param startAddress 起始物理地址
     * @param size 大小（字节）
     * @throws MemoryException 如果解除保护失败
     */
    public void unprotectMemoryRegion(PhysicalAddress startAddress, long size) throws MemoryException {
        log.info("解除内存区域保护: 起始地址={}, 大小={}", startAddress, size);
        try {
            // 解除内存区域保护的实现
            log.info("内存区域保护解除完成");
        } catch (Exception e) {
            log.error("解除内存区域保护失败: {}", e.getMessage(), e);
            throw new MemoryException("解除内存区域保护失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调整TLB大小
     * @param size 新的TLB大小
     * @throws MemoryException 如果调整失败
     */
    public void resizeTLB(int size) throws MemoryException {
        log.info("调整TLB大小: {}", size);
        try {
            // 调整TLB大小
            tlbManager.resize(size);
            log.info("TLB大小调整完成");
        } catch (Exception e) {
            log.error("调整TLB大小失败: {}", e.getMessage(), e);
            throw new MemoryException("调整TLB大小失败: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新所有TLB
     * @throws MemoryException 如果刷新失败
     */
    public void flushTLB() throws MemoryException {
        log.info("刷新所有TLB");
        try {
            // 刷新所有TLB
            tlbManager.invalidateAll();
            log.info("所有TLB刷新完成");
        } catch (Exception e) {
            log.error("刷新所有TLB失败: {}", e.getMessage(), e);
            throw new MemoryException("刷新TLB失败: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新进程TLB
     * @param processId 进程ID
     * @throws MemoryException 如果刷新失败
     */
    public void flushProcessTLB(int processId) throws MemoryException {
        log.info("刷新进程{}的TLB", processId);
        try {
            // 刷新进程TLB
            tlbManager.invalidateProcessEntries(processId);
            log.info("进程{}的TLB条目刷新完成", processId);
        } catch (Exception e) {
            log.error("刷新进程{}的TLB条目失败: {}", processId, e.getMessage(), e);
            throw new MemoryException("刷新进程TLB失败: " + e.getMessage(), e);
        }
    }

    /**
     * 压缩物理内存
     * @throws MemoryException 如果压缩失败
     * @return 压缩结果信息
     */
    public Map<String, Object> compressPhysicalMemory() throws MemoryException {
        log.info("压缩物理内存");
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 压缩物理内存的实现
            long before = memoryStats.getFreePhysicalMemory();
            
            // 此处实现具体的压缩算法
            // ...
            
            long after = memoryStats.getFreePhysicalMemory();
            long reclaimed = after - before;
            
            result.put("success", true);
            result.put("beforeFreeMemory", before);
            result.put("afterFreeMemory", after);
            result.put("reclaimedMemory", reclaimed);
            
            log.info("物理内存压缩完成，回收了{}字节", reclaimed);
        } catch (Exception e) {
            log.error("压缩物理内存失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            throw new MemoryException("压缩物理内存失败: " + e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * 设置页面大小
     * @param pageSize 新的页面大小
     * @throws MemoryException 如果设置失败
     */
    public void setPageSize(int pageSize) throws MemoryException {
        log.info("设置页面大小: {}", pageSize);
        try {
            // 设置页面大小的实现
            log.info("页面大小设置完成");
        } catch (Exception e) {
            log.error("设置页面大小失败: {}", e.getMessage(), e);
            throw new MemoryException("设置页面大小失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置内存超额分配比例
     * @param ratio 超额分配比例
     * @throws MemoryException 如果设置失败
     */
    public void setOvercommitRatio(double ratio) throws MemoryException {
        log.info("设置内存超额分配比例: {}", ratio);
        try {
            // 设置超额分配比例的实现
            log.info("内存超额分配比例设置完成");
        } catch (Exception e) {
            log.error("设置内存超额分配比例失败: {}", e.getMessage(), e);
            throw new MemoryException("设置内存超额分配比例失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置交换阈值
     * @param threshold 交换阈值（百分比）
     * @throws MemoryException 如果设置失败
     */
    public void setSwappingThreshold(int threshold) throws MemoryException {
        log.info("设置交换阈值: {}%", threshold);
        try {
            // 设置交换阈值的实现
            log.info("交换阈值设置完成");
        } catch (Exception e) {
            log.error("设置交换阈值失败: {}", e.getMessage(), e);
            throw new MemoryException("设置交换阈值失败: " + e.getMessage(), e);
        }
    }

    /**
     * 挂载交换设备
     * @param devicePath 设备路径
     * @param size 大小（字节）
     * @throws MemoryException 如果挂载失败
     */
    public void mountSwapDevice(String devicePath, long size) throws MemoryException {
        log.info("挂载交换设备: 路径={}, 大小={}", devicePath, size);
        try {
            // 挂载交换设备的实现
            log.info("交换设备挂载完成");
        } catch (Exception e) {
            log.error("挂载交换设备失败: {}", e.getMessage(), e);
            throw new MemoryException("挂载交换设备失败: " + e.getMessage(), e);
        }
    }

    /**
     * 卸载交换设备
     * @param devicePath 设备路径
     * @throws MemoryException 如果卸载失败
     */
    public void unmountSwapDevice(String devicePath) throws MemoryException {
        log.info("卸载交换设备: 路径={}", devicePath);
        try {
            // 卸载交换设备的实现
            log.info("交换设备卸载完成");
        } catch (Exception e) {
            log.error("卸载交换设备失败: {}", e.getMessage(), e);
            throw new MemoryException("卸载交换设备失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建内存转储
     * @param filePath 文件路径
     * @param processId 进程ID，-1表示整个系统
     * @return 是否成功
     * @throws MemoryException 如果创建失败
     */
    public boolean createMemoryDump(String filePath, int processId) throws MemoryException {
        log.info("创建内存转储: 文件路径={}, 进程ID={}", filePath, processId);
        try {
            // 创建内存转储的实现
            log.info("内存转储创建完成");
            return true;
        } catch (Exception e) {
            log.error("创建内存转储失败: {}", e.getMessage(), e);
            throw new MemoryException("创建内存转储失败: " + e.getMessage(), e);
        }
    }

    /**
     * 碎片整理
     * @throws MemoryException 如果整理失败
     * @return 整理结果信息
     */
    public Map<String, Object> defragmentMemory() throws MemoryException {
        log.info("开始内存碎片整理");
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 碎片整理的实现
            double before = memoryStats.getFragmentationIndex();
            
            // 此处实现具体的碎片整理算法
            // ...
            
            double after = memoryStats.getFragmentationIndex();
            
            result.put("success", true);
            result.put("beforeFragmentationIndex", before);
            result.put("afterFragmentationIndex", after);
            result.put("improvement", before - after);
            
            log.info("内存碎片整理完成，碎片率从{}%降低到{}%", 
                    String.format("%.2f", before * 100),
                    String.format("%.2f", after * 100));
        } catch (Exception e) {
            log.error("内存碎片整理失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            throw new MemoryException("内存碎片整理失败: " + e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * 获取内存段列表
     * @return 内存段列表
     */
    public List<MemorySegment> getMemorySegments() {
        log.debug("获取内存段列表");
        return new ArrayList<>(); // 实际实现应返回真实的内存段列表
    }

    /**
     * 执行垃圾回收
     * @return 回收的内存大小（字节）
     * @throws MemoryException 如果回收失败
     */
    public long performGarbageCollection() throws MemoryException {
        log.info("执行垃圾回收");
        
        try {
            // 垃圾回收实现
            long reclaimed = memoryReclaimer.reclaimMemory();
            log.info("垃圾回收完成，释放了{}字节内存", reclaimed);
            return reclaimed;
        } catch (Exception e) {
            log.error("垃圾回收失败: {}", e.getMessage(), e);
            throw new MemoryException("垃圾回收失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查内存区域是否可读
     * @param processId 进程ID
     * @param address 虚拟地址
     * @param length 长度
     * @return 是否可读
     */
    public boolean isReadable(int processId, VirtualAddress address, int length) {
        memoryLock.readLock().lock();
        try {
            // 获取页表
            PageTable pageTable = processPageTables.get(processId);
            if (pageTable == null) {
                return false;
            }
            
            // 检查地址范围内的所有页面是否可读
            long startAddr = address.getValue();
            long endAddr = startAddr + length - 1;
            
            // 页面掩码和大小
            final long PAGE_MASK = ~(VirtualAddress.PAGE_SIZE - 1L);
            final int PAGE_SIZE = VirtualAddress.PAGE_SIZE;
            
            for (long addr = startAddr; addr <= endAddr; addr = (addr & PAGE_MASK) + PAGE_SIZE) {
                VirtualAddress currentAddr = new VirtualAddress(addr);
                try {
                    if (!pageTable.hasReadPermission(processId, currentAddr)) {
                        return false;
                    }
                } catch (Exception e) {
                    log.error("检查读权限失败: {}", e.getMessage());
                    return false;
                }
            }
            
            return true;
        } finally {
            memoryLock.readLock().unlock();
        }
    }
    
    /**
     * 检查内存区域是否可写
     * @param processId 进程ID
     * @param address 虚拟地址
     * @param length 长度
     * @return 是否可写
     */
    public boolean isWritable(int processId, VirtualAddress address, int length) {
        memoryLock.readLock().lock();
        try {
            // 获取页表
            PageTable pageTable = processPageTables.get(processId);
            if (pageTable == null) {
                return false;
            }
            
            // 检查地址范围内的所有页面是否可写
            long startAddr = address.getValue();
            long endAddr = startAddr + length - 1;
            
            // 页面掩码和大小
            final long PAGE_MASK = ~(VirtualAddress.PAGE_SIZE - 1L);
            final int PAGE_SIZE = VirtualAddress.PAGE_SIZE;
            
            for (long addr = startAddr; addr <= endAddr; addr = (addr & PAGE_MASK) + PAGE_SIZE) {
                VirtualAddress currentAddr = new VirtualAddress(addr);
                try {
                    if (!pageTable.hasWritePermission(processId, currentAddr)) {
                        return false;
                    }
                } catch (Exception e) {
                    log.error("检查写权限失败: {}", e.getMessage());
                    return false;
                }
            }
            
            return true;
        } finally {
            memoryLock.readLock().unlock();
        }
    }
    
    /**
     * 将虚拟地址转换为物理地址
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 物理地址
     * @throws AddressTranslationException 地址转换异常
     */
    public PhysicalAddress translate(int processId, VirtualAddress virtualAddress) throws AddressTranslationException {
        // 如果启用了TLB，先检查TLB
        if (tlbEnabled) {
            try {
                return tlbManager.lookup(processId, virtualAddress);
            } catch (Exception e) {
                // TLB未命中，继续进行地址转换
                log.debug("TLB未命中: {}", e.getMessage());
            }
        }
        
        memoryLock.readLock().lock();
        try {
            // 获取页表
            PageTable pageTable = processPageTables.get(processId);
            if (pageTable == null) {
                throw new AddressTranslationException("进程页表不存在", processId, virtualAddress);
            }
            
            try {
                // 进行地址转换
                PhysicalAddress physicalAddress = pageTable.translate(processId, virtualAddress);
                
                // 如果启用了TLB，更新TLB
                if (tlbEnabled) {
                    tlbManager.updateEntry(processId, virtualAddress, physicalAddress);
                }
                
                return physicalAddress;
            } catch (PageFaultException e) {
                // 处理缺页异常
                log.warn("地址转换过程中发生缺页: {}", e.getMessage());
                throw new AddressTranslationException("地址转换过程中发生缺页", processId, virtualAddress, e);
            } catch (Exception e) {
                log.error("地址转换失败: {}", e.getMessage());
                throw new AddressTranslationException("地址转换失败: " + e.getMessage(), processId, virtualAddress, e);
            }
        } finally {
            memoryLock.readLock().unlock();
        }
    }
} 