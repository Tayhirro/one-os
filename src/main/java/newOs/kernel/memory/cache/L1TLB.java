package newOs.kernel.memory.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import newOs.exception.TLBMissException;
import newOs.kernel.memory.model.VirtualAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashMap;
import java.util.Map;

/**
 * L1 TLB (一级转换后备缓冲器)
 * 体积小但速度快的TLB实现，采用全相联映射
 */
@Component
@Slf4j
public class L1TLB implements TLB {
    
    // TLB条目列表，用ArrayList便于快速遍历
    private final List<TLBEntry> entries;
    
    // TLB容量（条目数）
    @Getter
    private final int capacity;
    
    // TLB名称
    @Getter
    private final String name = "L1 TLB";
    
    // 统计：访问次数
    private final AtomicLong accessCount = new AtomicLong(0);
    
    // 统计：命中次数
    private final AtomicLong hitCount = new AtomicLong(0);
    
    // 使用Map来存储TLB条目
    private final Map<String, TLBEntry> entriesMap = new HashMap<>();
    
    /**
     * 构造L1 TLB
     * @param capacity TLB容量
     */
    public L1TLB(@Value("${memory.cache.l1tlb.capacity:64}") int capacity) {
        this.capacity = capacity;
        this.entries = Collections.synchronizedList(new ArrayList<>(capacity));
        log.info("L1 TLB初始化完成，容量: {} 条目", capacity);
    }
    
    /**
     * 查找TLB条目
     * @param virtualAddress 虚拟地址
     * @param processId 进程ID
     * @return TLB条目
     */
    public TLBEntry lookup(VirtualAddress virtualAddress, int processId) {
        int pageNumber = virtualAddress.getPageNumber();
        String key = processId + ":" + pageNumber;
        return entriesMap.get(key);
    }
    
    @Override
    public TLBEntry lookup(int pid, VirtualAddress virtualAddress) throws TLBMissException {
        accessCount.incrementAndGet();
        
        synchronized (entries) {
            // 遍历条目查找匹配项
            for (TLBEntry entry : entries) {
                if (entry.isValid() && entry.matches(pid, virtualAddress)) {
                    // 记录访问并移动到列表前端（实现LRU）
                    entry.markAccessed();
                    entries.remove(entry);
                    entries.add(0, entry);
                    
                    hitCount.incrementAndGet();
                    return entry;
                }
            }
        }
        
        // 未找到，抛出TLB未命中异常
        throw new TLBMissException(
                virtualAddress, 
                "L1", 
                pid, 
                String.format("L1 TLB未命中: 进程%d, 地址(%d,%d)", 
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
        
        synchronized (entries) {
            // 如果超出容量，移除最旧的条目
            if (entries.size() >= capacity) {
                entries.remove(entries.size() - 1);
            }
            
            // 添加到列表前端
            entries.add(0, entry);
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
        synchronized (entries) {
            Iterator<TLBEntry> iterator = entries.iterator();
            while (iterator.hasNext()) {
                TLBEntry entry = iterator.next();
                if (entry.getPid() == pid) {
                    iterator.remove();
                    count++;
                }
            }
        }
        log.info("L1TLB: 已无效化进程[{}]的{}个条目", pid, count);
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
        synchronized (entries) {
            Iterator<TLBEntry> iterator = entries.iterator();
            while (iterator.hasNext()) {
                TLBEntry entry = iterator.next();
                if (entry.getPid() == pid && 
                    entry.getPageNumber() == virtualAddress.getPageNumber()) {
                    iterator.remove();
                    log.info("L1TLB: 已无效化进程[{}]的地址[{}]的映射", pid, virtualAddress);
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public void flush() {
        synchronized (entries) {
            entries.clear();
        }
        log.info("L1 TLB已清空");
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
        return entries.size();
    }
    
    @Override
    public void resetStats() {
        accessCount.set(0);
        hitCount.set(0);
        log.info("L1 TLB统计信息已重置");
    }
    
    /**
     * 获取L1 TLB统计信息的字符串表示
     * @return 统计信息字符串
     */
    @Override
    public String getStatsInfo() {
        return String.format("L1 TLB统计信息 - 容量: %d, 当前大小: %d, 访问次数: %d, 命中次数: %d, 命中率: %.2f%%",
                getCapacity(), getSize(), getAccessCount(), getHitCount(), getHitRatio() * 100);
    }
} 