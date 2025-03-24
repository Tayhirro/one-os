package newOs.kernel.memory.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import newOs.exception.AddressTranslationException;
import newOs.exception.TLBMissException;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.virtual.PageTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TLB管理器
 * 管理多级TLB缓存，协调L1和L2 TLB的工作，处理TLB未命中情况
 */
@Component
@Slf4j
public class TLBManager {
    
    // L1 TLB，速度快但容量小
    private final L1TLB l1TLB;
    
    // L2 TLB，容量大但速度较慢
    private final L2TLB l2TLB;
    
    // 页表，用于在TLB未命中时查找地址映射
    private final PageTable pageTable;
    
    // 统计：查找操作总数
    @Getter
    private final AtomicLong lookupCount = new AtomicLong(0);
    
    // 统计：L1 TLB命中次数
    @Getter
    private final AtomicLong l1HitCount = new AtomicLong(0);
    
    // 统计：L2 TLB命中次数
    @Getter
    private final AtomicLong l2HitCount = new AtomicLong(0);
    
    // 统计：页表查找次数
    @Getter
    private final AtomicLong pageTableLookupCount = new AtomicLong(0);
    
    /**
     * 构造TLB管理器
     * @param l1TLB L1 TLB
     * @param l2TLB L2 TLB
     * @param pageTable 页表
     */
    @Autowired
    public TLBManager(L1TLB l1TLB, L2TLB l2TLB, PageTable pageTable) {
        this.l1TLB = l1TLB;
        this.l2TLB = l2TLB;
        this.pageTable = pageTable;
    }
    
    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        log.info("TLB管理器初始化完成");
    }
    
    /**
     * 查找虚拟地址对应的物理地址
     * 
     * 查找顺序：
     * 1. 先查找L1 TLB
     * 2. 若L1未命中，查找L2 TLB
     * 3. 若L2未命中，查找页表
     * 4. 将查找结果添加到TLB中
     * 
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @return 物理地址
     * @throws AddressTranslationException 地址转换异常
     */
    public PhysicalAddress lookup(int pid, VirtualAddress virtualAddress) throws AddressTranslationException {
        lookupCount.incrementAndGet();
        
        try {
            // 1. 查找L1 TLB
            TLBEntry entry = l1TLB.lookup(pid, virtualAddress);
            l1HitCount.incrementAndGet();
            
            // 构建物理地址
            PhysicalAddress physicalAddress = new PhysicalAddress(entry.getFrameNumber(), virtualAddress.getOffset());
            
            log.info("【内存管理-TLB】L1 TLB命中，进程ID: {}, 虚拟地址: 0x{}, 物理地址: 0x{}, 访问权限: {}{}{}",
                    pid, Long.toHexString(virtualAddress.getValue()), Long.toHexString(physicalAddress.getValue()),
                    entry.isReadable() ? "R" : "-", 
                    entry.isWritable() ? "W" : "-", 
                    entry.isExecutable() ? "X" : "-");
            return physicalAddress;
        } catch (TLBMissException e) {
            // L1 TLB未命中，继续查找L2 TLB
            try {
                // 2. 查找L2 TLB
                TLBEntry entry = l2TLB.lookup(pid, virtualAddress);
                l2HitCount.incrementAndGet();
                
                // 构建物理地址
                PhysicalAddress physicalAddress = new PhysicalAddress(entry.getFrameNumber(), virtualAddress.getOffset());
                
                // 添加到L1 TLB中
                l1TLB.addEntry(entry);
                
                log.info("【内存管理-TLB】L1 TLB未命中，L2 TLB命中，进程ID: {}, 虚拟地址: 0x{}, 物理地址: 0x{}, 已更新到L1 TLB",
                        pid, Long.toHexString(virtualAddress.getValue()), Long.toHexString(physicalAddress.getValue()));
                return physicalAddress;
            } catch (TLBMissException e2) {
                // L2 TLB也未命中，查找页表
                pageTableLookupCount.incrementAndGet();
                
                // 3. 查找页表
                log.info("【内存管理-TLB】L1和L2 TLB都未命中，查询页表，进程ID: {}, 虚拟地址: 0x{}", 
                        pid, Long.toHexString(virtualAddress.getValue()));
                
                // 页表查找（使用原有方法）
                PhysicalAddress physicalAddress = pageTable.translate(pid, virtualAddress);
                
                // 将结果添加到两级TLB中
                l1TLB.insert(pid, virtualAddress, (int)physicalAddress.getFrameNumber(), true, true, false);
                l2TLB.insert(pid, virtualAddress, (int)physicalAddress.getFrameNumber(), true, true, false);
                
                log.info("【内存管理-TLB】页表查找成功，已更新TLB，进程ID: {}, 虚拟地址: 0x{}, 物理地址: 0x{}", 
                        pid, Long.toHexString(virtualAddress.getValue()), Long.toHexString(physicalAddress.getValue()));
                
                return physicalAddress;
            }
        }
    }
    
    /**
     * 调整TLB大小
     * @param size 新的大小
     */
    public void resize(int size) {
        // 此处需实现具体的TLB大小调整逻辑
        // 由于TLB接口没有提供resize方法，这里只记录日志
        log.info("TLB大小调整请求: {} (未实现调整功能)", size);
    }
    
    /**
     * 使所有TLB条目失效
     */
    public void invalidateAll() {
        l1TLB.flush();
        l2TLB.flush();
        log.debug("所有TLB条目已失效");
    }
    
    /**
     * 使指定进程的所有TLB条目失效
     * @param processId 进程ID
     */
    public void invalidateProcessEntries(int processId) {
        l1TLB.invalidateAll(processId);
        l2TLB.invalidateAll(processId);
        log.debug("进程{}的TLB条目已失效", processId);
    }
    
    /**
     * 使指定虚拟地址的TLB条目失效
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否成功无效化（任一级TLB中有条目被无效化即为成功）
     */
    public boolean invalidate(int pid, VirtualAddress virtualAddress) {
        boolean l1Invalid = l1TLB.invalidate(pid, virtualAddress);
        boolean l2Invalid = l2TLB.invalidate(pid, virtualAddress);
        boolean result = l1Invalid || l2Invalid;
        
        if (result) {
            log.trace("使TLB条目失效: pid={}, va={}, L1={}, L2={}", 
                    pid, virtualAddress, l1Invalid, l2Invalid);
        }
        
        return result;
    }
    
    /**
     * 更新虚拟地址到物理地址的映射
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @param physicalAddress 物理地址
     */
    public void updateEntry(int pid, VirtualAddress virtualAddress, PhysicalAddress physicalAddress) {
        // 默认所有权限都开启
        boolean readable = true;
        boolean writable = true; 
        boolean executable = true;
        
        // 尝试从页表获取实际权限
        try {
            readable = pageTable.hasReadPermission(pid, virtualAddress);
            writable = pageTable.hasWritePermission(pid, virtualAddress);
            executable = pageTable.hasExecutePermission(pid, virtualAddress);
        } catch (Exception e) {
            log.warn("获取页面权限失败，使用默认权限: {}", e.getMessage());
        }
        
        // 创建并添加TLB条目
        l1TLB.update(pid, virtualAddress, physicalAddress, readable, writable, executable);
        l2TLB.update(pid, virtualAddress, physicalAddress, readable, writable, executable);
        
        log.trace("更新TLB条目: pid={}, va={} -> pa={}", 
                pid, virtualAddress, physicalAddress);
    }
    
    /**
     * 完全刷新TLB，清空所有条目
     */
    public void flush() {
        l1TLB.flush();
        l2TLB.flush();
        log.info("TLB完全刷新");
    }
    
    /**
     * 获取TLB统计信息
     * @return 统计信息字符串
     */
    public String getStatsInfo() {
        long totalLookups = lookupCount.get();
        long l1Hits = l1HitCount.get();
        long l2Hits = l2HitCount.get();
        long pageTableLookups = pageTableLookupCount.get();
        
        double l1HitRate = totalLookups > 0 ? (double) l1Hits / totalLookups * 100 : 0;
        double l2HitRate = (totalLookups - l1Hits) > 0 ? (double) l2Hits / (totalLookups - l1Hits) * 100 : 0;
        double overallHitRate = totalLookups > 0 ? (double) (l1Hits + l2Hits) / totalLookups * 100 : 0;
        
        return String.format(
                "TLB统计信息:\n" +
                "总查找次数: %d\n" +
                "L1 TLB容量: %d, 命中: %d (命中率: %.2f%%)\n" +
                "L2 TLB容量: %d, 命中: %d (命中率: %.2f%%)\n" +
                "整体命中率: %.2f%%\n" +
                "页表查找: %d",
                totalLookups,
                l1TLB.getCapacity(), l1Hits, l1HitRate,
                l2TLB.getCapacity(), l2Hits, l2HitRate,
                overallHitRate,
                pageTableLookups
        );
    }
    
    /**
     * 预热TLB，预加载常用地址映射
     * @param pid 进程ID
     * @param startAddress 起始虚拟地址
     * @param pageCount 预加载页数
     */
    public void warmup(int pid, VirtualAddress startAddress, int pageCount) {
        log.info("开始TLB预热: pid={}, 起始地址={}, 页数={}", 
                pid, startAddress, pageCount);
        
        int successCount = 0;
        
        for (int i = 0; i < pageCount; i++) {
            try {
                // 构建当前页的虚拟地址，每页偏移一个页面大小
                VirtualAddress currentVA = new VirtualAddress(startAddress.getValue() + i * VirtualAddress.PAGE_SIZE);
                
                // 尝试查找并加载到TLB
                lookup(pid, currentVA);
                successCount++;
            } catch (Exception e) {
                log.warn("TLB预热过程中地址加载失败: {}", e.getMessage());
            }
        }
        
        log.info("TLB预热完成: 成功加载 {}/{} 页", successCount, pageCount);
    }
} 