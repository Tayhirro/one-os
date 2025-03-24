package newOs.kernel.memory.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import newOs.exception.TLBMissException;
import newOs.kernel.memory.model.VirtualAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * L2 TLB (二级转换后备缓冲器)
 * 体积大但速度相对较慢的TLB实现，使用散列映射提高容量
 */
@Component
@Slf4j
public class L2TLB implements TLB {
    
    // 按进程ID和虚拟地址组织的TLB条目映射表
    private final Map<Integer, Map<String, TLBEntry>> entriesByProcess;
    
    // TLB容量（条目数）
    @Getter
    private final int capacity;
    
    // TLB名称
    @Getter
    private final String name = "L2 TLB";
    
    // 最近使用的条目列表，用于LRU替换策略
    private final Map<String, Long> lastAccessTime;
    
    // 当前条目数
    private int size = 0;
    
    // 统计：访问次数
    private final AtomicLong accessCount = new AtomicLong(0);
    
    // 统计：命中次数
    private final AtomicLong hitCount = new AtomicLong(0);
    
    /**
     * 构造L2 TLB
     * @param capacity TLB容量
     */
    public L2TLB(@Value("${memory.cache.l2tlb.capacity:512}") int capacity) {
        this.capacity = capacity;
        this.entriesByProcess = new ConcurrentHashMap<>();
        this.lastAccessTime = new ConcurrentHashMap<>();
        log.info("L2 TLB初始化完成，容量: {} 条目", capacity);
    }
    
    @Override
    public TLBEntry lookup(int pid, VirtualAddress virtualAddress) throws TLBMissException {
        accessCount.incrementAndGet();
        
        String key = pid + ":" + virtualAddress.getPageNumber();
        Map<String, TLBEntry> processEntries = entriesByProcess.get(pid);
        
        if (processEntries != null) {
            TLBEntry entry = processEntries.get(key);
            if (entry != null && entry.isValid()) {
                hitCount.incrementAndGet();
                lastAccessTime.put(key, System.currentTimeMillis());
                return entry;
            }
        }
        
        throw new TLBMissException(
                virtualAddress,
                "L2",
                pid,
                String.format("L2 TLB未命中: 进程%d, 地址(%d,%d)",
                        pid,
                        virtualAddress.getPageNumber(),
                        virtualAddress.getOffset())
        );
    }
    
    @Override
    public void insert(int pid, VirtualAddress virtualAddress, int frameNumber,
                      boolean readable, boolean writable, boolean executable) {
        TLBEntry newEntry = new TLBEntry(
                pid,
                virtualAddress,
                frameNumber,
                readable,
                writable,
                executable
        );
        
        addEntry(newEntry);
    }
    
    /**
     * 添加TLB条目
     * @param entry TLB条目
     */
    public void addEntry(TLBEntry entry) {
        if (entry == null) {
            return;
        }
        
        int pid = entry.getProcessId();
        String key = pid + ":" + entry.getVirtualAddress().getPageNumber();
        
        // 获取或创建进程的TLB映射表
        Map<String, TLBEntry> processEntries = entriesByProcess.computeIfAbsent(
                pid, k -> new HashMap<>());
        
        // 检查是否需要进行替换
        if (!processEntries.containsKey(key) && size >= capacity) {
            // 执行LRU替换
            evictLRUEntry();
        }
        
        // 更新条目
        boolean isNewEntry = !processEntries.containsKey(key);
        processEntries.put(key, entry);
        
        // 更新最近访问时间
        lastAccessTime.put(key, System.currentTimeMillis());
        
        // 更新大小计数
        if (isNewEntry) {
            size++;
        }
    }
    
    /**
     * 驱逐最近最少使用的条目
     */
    private void evictLRUEntry() {
        if (lastAccessTime.isEmpty()) {
            return;
        }
        
        // 查找访问时间最早的条目
        String lruKey = null;
        long earliestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, Long> entry : lastAccessTime.entrySet()) {
            if (entry.getValue() < earliestTime) {
                earliestTime = entry.getValue();
                lruKey = entry.getKey();
            }
        }
        
        if (lruKey != null) {
            // 解析进程ID和键
            String[] parts = lruKey.split(":", 2);
            int pid = Integer.parseInt(parts[0]);
            String key = parts[1];
            
            // 移除条目
            Map<String, TLBEntry> processEntries = entriesByProcess.get(pid);
            if (processEntries != null) {
                processEntries.remove(key);
                if (processEntries.isEmpty()) {
                    entriesByProcess.remove(pid);
                }
            }
            
            lastAccessTime.remove(lruKey);
            size--;
        }
    }
    
    /**
     * 使指定进程的所有TLB条目无效
     * @param pid 进程ID
     * @return 被无效化的条目数量
     */
    @Override
    public int invalidateAll(int pid) {
        int count = 0;
        // 获取进程的TLB映射
        Map<String, TLBEntry> processMapping = entriesByProcess.get(pid);
        if (processMapping != null) {
            count = processMapping.size();
            processMapping.clear();
            size -= count;
            log.info("L2TLB: 已无效化进程[{}]的{}个条目", pid, count);
        }
        return count;
    }
    
    /**
     * 使指定进程的特定虚拟地址的TLB条目无效
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否成功无效化
     */
    @Override
    public boolean invalidate(int pid, VirtualAddress virtualAddress) {
        // 获取进程的TLB映射
        Map<String, TLBEntry> processMapping = entriesByProcess.get(pid);
        if (processMapping != null) {
            // 构造条目键
            String key = pid + ":" + virtualAddress.getPageNumber();
            // 移除条目
            TLBEntry removed = processMapping.remove(key);
            if (removed != null) {
                size--;
                log.info("L2TLB: 已无效化进程[{}]的地址[{}]的映射", pid, virtualAddress);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void flush() {
        entriesByProcess.clear();
        lastAccessTime.clear();
        size = 0;
        log.info("L2 TLB已清空");
    }
    
    @Override
    public double getHitRatio() {
        long access = accessCount.get();
        return access == 0 ? 0 : (double) hitCount.get() / access;
    }
    
    @Override
    public long getHitCount() {
        return hitCount.get();
    }
    
    @Override
    public long getAccessCount() {
        return accessCount.get();
    }
    
    @Override
    public int getSize() {
        return size;
    }
    
    @Override
    public void resetStats() {
        accessCount.set(0);
        hitCount.set(0);
        log.info("L2 TLB统计信息已重置");
    }
    
    /**
     * 获取L2 TLB统计信息的字符串表示
     * @return 统计信息字符串
     */
    @Override
    public String getStatsInfo() {
        return String.format("L2 TLB统计信息 - 容量: %d, 当前大小: %d, 访问次数: %d, 命中次数: %d, 命中率: %.2f%%",
                getCapacity(), getSize(), getAccessCount(), getHitCount(), getHitRatio() * 100);
    }
} 