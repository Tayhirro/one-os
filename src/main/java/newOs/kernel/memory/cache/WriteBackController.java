package newOs.kernel.memory.cache;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import newOs.kernel.memory.PhysicalMemory;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.virtual.PageTable;
import newOs.kernel.memory.virtual.PageFrameTable;
import newOs.kernel.memory.virtual.paging.Page;
import newOs.kernel.memory.virtual.paging.PageFrame;
import newOs.kernel.memory.virtual.paging.SwapManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.io.IOException;

/**
 * 写回控制器
 * 实现写回(Write-Back)缓存策略，管理脏数据的写回操作
 */
@Component
@Slf4j
@Data
public class WriteBackController implements CacheWritePolicy {
    
    // 缓存的数据，按进程ID和虚拟地址组织
    private final Map<Integer, Map<String, CacheEntry>> cacheData;
    
    // 物理内存对象
    private final PhysicalMemory physicalMemory;
    
    // 页表对象，用于地址转换
    private final PageTable pageTable;
    
    // 页帧表对象
    private final PageFrameTable pageFrameTable;
    
    // 交换管理器
    private final SwapManager swapManager;
    
    // 交换文件通道
    private final FileChannel swapChannel;
    
    // 最大缓存项数量
    private final int maxCacheEntries;
    
    // 写回阈值，当脏项数量超过此阈值时触发写回
    private final int writeBackThreshold;
    
    // 当前脏项数量
    private int dirtyCount = 0;
    
    // 统计：写操作总数
    private final AtomicLong writeCount = new AtomicLong(0);
    
    // 统计：读操作总数
    private final AtomicLong readCount = new AtomicLong(0);
    
    // 统计：写命中次数
    private final AtomicLong writeHitCount = new AtomicLong(0);
    
    // 统计：读命中次数
    private final AtomicLong readHitCount = new AtomicLong(0);
    
    // 统计：写回操作次数
    private final AtomicLong writeBackCount = new AtomicLong(0);
    
    private static final int PAGE_SIZE = 4096;
    
    /**
     * 缓存条目，存储数据和元数据
     */
    @Data
    class CacheEntry {
        // 缓存的数据
        private byte[] data;
        
        // 对应的物理地址
        private PhysicalAddress physicalAddress;
        
        // 是否为脏数据
        private boolean dirty;
        
        // 最后访问时间
        private long lastAccessTime;
        
        // 访问计数
        private int accessCount;
        
        /**
         * 构造缓存条目
         * @param data 数据
         * @param physicalAddress 物理地址
         * @param dirty 是否为脏数据
         */
        public CacheEntry(byte[] data, PhysicalAddress physicalAddress, boolean dirty) {
            this.data = data;
            this.physicalAddress = physicalAddress;
            this.dirty = dirty;
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount = 0;
        }
        
        /**
         * 更新访问信息
         */
        public void markAccessed() {
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount++;
        }
        
        /**
         * 标记为脏数据
         */
        public void markDirty() {
            if (!this.dirty) {
                this.dirty = true;
            }
        }
    }
    
    /**
     * 构造写回控制器
     * @param physicalMemory 物理内存对象
     * @param pageTable 页表对象
     * @param pageFrameTable 页帧表对象
     * @param swapManager 交换管理器
     * @param swapChannel 交换文件通道
     * @param maxCacheEntries 最大缓存项数量
     * @param writeBackThreshold 写回阈值
     */
    public WriteBackController(
            PhysicalMemory physicalMemory,
            PageTable pageTable,
            PageFrameTable pageFrameTable,
            SwapManager swapManager,
            FileChannel swapChannel,
            @Value("${memory.cache.max_entries:1024}") int maxCacheEntries,
            @Value("${memory.cache.writeback.threshold:64}") int writeBackThreshold) {
        
        this.cacheData = new ConcurrentHashMap<>();
        this.physicalMemory = physicalMemory;
        this.pageTable = pageTable;
        this.pageFrameTable = pageFrameTable;
        this.swapManager = swapManager;
        this.swapChannel = swapChannel;
        this.maxCacheEntries = maxCacheEntries;
        this.writeBackThreshold = writeBackThreshold;
        
        log.info("写回控制器初始化完成，最大缓存条目: {}, 写回阈值: {}", maxCacheEntries, writeBackThreshold);
    }
    
    @Override
    public PolicyType getPolicyType() {
        return PolicyType.WRITE_BACK;
    }
    
    @Override
    public boolean write(int pid, VirtualAddress virtualAddress, PhysicalAddress physicalAddress, 
                         byte[] data, int size) {
        
        writeCount.incrementAndGet();
        
        // 获取缓存键
        String cacheKey = getCacheKey(virtualAddress);
        
        // 获取或创建进程的缓存映射
        Map<String, CacheEntry> processCacheMap = cacheData.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());
        
        // 查找缓存条目
        CacheEntry entry = processCacheMap.get(cacheKey);
        boolean cacheHit = (entry != null);
        
        if (cacheHit) {
            // 缓存命中，更新数据
            System.arraycopy(data, 0, entry.getData(), 0, Math.min(size, entry.getData().length));
            entry.markDirty();
            entry.markAccessed();
            writeHitCount.incrementAndGet();
        } else {
            // 缓存未命中，创建新条目
            byte[] cachedData = new byte[size];
            System.arraycopy(data, 0, cachedData, 0, size);
            entry = new CacheEntry(cachedData, physicalAddress, true);
            
            // 检查缓存是否已满，如果已满则执行替换
            if (getCacheEntryCount() >= maxCacheEntries) {
                evictEntry();
            }
            
            // 添加新条目
            processCacheMap.put(cacheKey, entry);
        }
        
        // 更新脏项计数
        if (!cacheHit) {
            dirtyCount++;
        }
        
        // 检查是否需要触发写回
        if (dirtyCount >= writeBackThreshold) {
            writeBackDirtyEntries();
        }
        
        return true;
    }
    
    @Override
    public byte[] read(int pid, VirtualAddress virtualAddress, PhysicalAddress physicalAddress, 
                       int size) {
        
        readCount.incrementAndGet();
        
        // 获取缓存键
        String cacheKey = getCacheKey(virtualAddress);
        
        // 获取进程的缓存映射
        Map<String, CacheEntry> processCacheMap = cacheData.get(pid);
        if (processCacheMap == null) {
            // 进程无缓存，从物理内存读取
            byte[] data = new byte[size];
            physicalMemory.readBlock(physicalAddress, data, 0, size);
            return data;
        }
        
        // 查找缓存条目
        CacheEntry entry = processCacheMap.get(cacheKey);
        if (entry != null) {
            // 缓存命中，更新访问信息
            entry.markAccessed();
            readHitCount.incrementAndGet();
            
            // 返回缓存数据
            byte[] result = new byte[size];
            System.arraycopy(entry.getData(), 0, result, 0, Math.min(size, entry.getData().length));
            return result;
        }
        
        // 缓存未命中，从物理内存读取
        byte[] data = new byte[size];
        physicalMemory.readBlock(physicalAddress, data, 0, size);
        
        // 添加到缓存（根据写分配策略）
        if (getPolicyType() == PolicyType.WRITE_ALLOCATE) {
            // 检查缓存是否已满
            if (getCacheEntryCount() >= maxCacheEntries) {
                evictEntry();
            }
            
            // 创建新缓存条目
            CacheEntry newEntry = new CacheEntry(data.clone(), physicalAddress, false);
            processCacheMap.put(cacheKey, newEntry);
        }
        
        return data;
    }
    
    @Override
    public boolean flush(int pid, VirtualAddress virtualAddress) {
        // 获取缓存键
        String cacheKey = getCacheKey(virtualAddress);
        
        // 获取进程的缓存映射
        Map<String, CacheEntry> processCacheMap = cacheData.get(pid);
        if (processCacheMap == null) {
            return false;
        }
        
        // 查找缓存条目
        CacheEntry entry = processCacheMap.get(cacheKey);
        if (entry != null && entry.isDirty()) {
            // 写回到物理内存
            writeBackEntry(entry, pid, virtualAddress);
            return true;
        }
        
        return false;
    }
    
    @Override
    public int flushAll(int pid) {
        int flushedCount = 0;
        
        // 获取进程的缓存映射
        Map<String, CacheEntry> processCacheMap = cacheData.get(pid);
        if (processCacheMap == null) {
            return 0;
        }
        
        // 遍历所有缓存条目
        for (Map.Entry<String, CacheEntry> mapEntry : processCacheMap.entrySet()) {
            CacheEntry entry = mapEntry.getValue();
            if (entry.isDirty()) {
                // 写回到物理内存
                VirtualAddress virtualAddress = parseVirtualAddressFromKey(mapEntry.getKey());
                writeBackEntry(entry, pid, virtualAddress);
                flushedCount++;
            }
        }
        
        return flushedCount;
    }
    
    @Override
    public boolean invalidate(int pid, VirtualAddress virtualAddress) {
        // 获取缓存键
        String cacheKey = getCacheKey(virtualAddress);
        
        // 获取进程的缓存映射
        Map<String, CacheEntry> processCacheMap = cacheData.get(pid);
        if (processCacheMap == null) {
            return false;
        }
        
        // 查找并移除缓存条目
        CacheEntry entry = processCacheMap.remove(cacheKey);
        if (entry != null) {
            // 如果是脏数据，先写回
            if (entry.isDirty()) {
                writeBackEntry(entry, pid, virtualAddress);
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public int invalidateAll(int pid) {
        // 获取进程的缓存映射
        Map<String, CacheEntry> processCacheMap = cacheData.remove(pid);
        if (processCacheMap == null) {
            return 0;
        }
        
        int count = 0;
        // 遍历所有缓存条目
        for (Map.Entry<String, CacheEntry> mapEntry : processCacheMap.entrySet()) {
            CacheEntry entry = mapEntry.getValue();
            if (entry.isDirty()) {
                // 写回到物理内存
                VirtualAddress virtualAddress = parseVirtualAddressFromKey(mapEntry.getKey());
                writeBackEntry(entry, pid, virtualAddress);
            }
            count++;
        }
        
        // 更新脏项计数
        dirtyCount = calculateDirtyCount();
        
        return count;
    }
    
    @Override
    public int getDirtyCount() {
        return dirtyCount;
    }
    
    @Override
    public String getWriteStatsInfo() {
        return String.format(
                "写回控制器统计信息:\n" +
                "缓存条目数: %d/%d\n" +
                "脏项数: %d\n" +
                "写操作: %d (命中率: %.2f%%)\n" +
                "读操作: %d (命中率: %.2f%%)\n" +
                "写回操作: %d",
                getCacheEntryCount(), maxCacheEntries,
                dirtyCount,
                writeCount.get(),
                writeCount.get() > 0 ? (double) writeHitCount.get() / writeCount.get() * 100 : 0,
                readCount.get(),
                readCount.get() > 0 ? (double) readHitCount.get() / readCount.get() * 100 : 0,
                writeBackCount.get()
        );
    }
    
    /**
     * 定时写回任务，每隔一定时间将脏数据写回物理内存
     */
    @Scheduled(fixedDelayString = "${memory.cache.writeback.interval:1000}")
    public void scheduledWriteBack() {
        if (dirtyCount > 0) {
            log.debug("执行定时写回任务，当前脏项数: {}", dirtyCount);
            writeBackDirtyEntries();
        }
    }
    
    /**
     * 写回所有脏数据
     */
    private void writeBackDirtyEntries() {
        int writtenCount = 0;
        
        // 遍历所有进程的缓存
        for (Map.Entry<Integer, Map<String, CacheEntry>> processEntry : cacheData.entrySet()) {
            int pid = processEntry.getKey();
            Map<String, CacheEntry> processCacheMap = processEntry.getValue();
            
            // 遍历进程的所有缓存条目
            for (Map.Entry<String, CacheEntry> mapEntry : processCacheMap.entrySet()) {
                CacheEntry entry = mapEntry.getValue();
                if (entry.isDirty()) {
                    // 写回到物理内存
                    VirtualAddress virtualAddress = parseVirtualAddressFromKey(mapEntry.getKey());
                    writeBackEntry(entry, pid, virtualAddress);
                    writtenCount++;
                }
            }
        }
        
        if (writtenCount > 0) {
            log.debug("写回了 {} 个脏项", writtenCount);
        }
        
        // 重置脏项计数
        dirtyCount = 0;
    }
    
    /**
     * 写回单个缓存条目
     * @param entry 缓存条目
     * @param pid 进程ID
     * @param virtualAddress 虚拟地址
     */
    private void writeBackEntry(CacheEntry entry, int pid, VirtualAddress virtualAddress) {
        // 获取物理地址
        PhysicalAddress physicalAddress = entry.getPhysicalAddress();
        
        // 写入物理内存
        physicalMemory.writeBlock(physicalAddress, entry.getData(), 0, entry.getData().length);
        
        // 重置脏标志
        entry.setDirty(false);
        
        // 更新统计信息
        writeBackCount.incrementAndGet();
        
        log.trace("写回缓存条目: pid={}, 地址=({},{}), 大小={}字节", 
                pid, 
                virtualAddress.getPageNumber(), 
                virtualAddress.getOffset(),
                entry.getData().length);
    }
    
    /**
     * 替换策略，当缓存满时选择一个条目替换
     * 目前实现简单的LRU策略
     */
    private void evictEntry() {
        // 查找最近最少使用的条目
        CacheEntry lruEntry = null;
        int evictPid = -1;
        String evictKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<Integer, Map<String, CacheEntry>> processEntry : cacheData.entrySet()) {
            int pid = processEntry.getKey();
            Map<String, CacheEntry> processCacheMap = processEntry.getValue();
            
            for (Map.Entry<String, CacheEntry> mapEntry : processCacheMap.entrySet()) {
                CacheEntry entry = mapEntry.getValue();
                if (entry.getLastAccessTime() < oldestTime) {
                    oldestTime = entry.getLastAccessTime();
                    lruEntry = entry;
                    evictPid = pid;
                    evictKey = mapEntry.getKey();
                }
            }
        }
        
        if (lruEntry != null && evictPid != -1 && evictKey != null) {
            // 如果是脏数据，先写回
            if (lruEntry.isDirty()) {
                VirtualAddress virtualAddress = parseVirtualAddressFromKey(evictKey);
                writeBackEntry(lruEntry, evictPid, virtualAddress);
                dirtyCount--;
            }
            
            // 从缓存中移除
            Map<String, CacheEntry> processCacheMap = cacheData.get(evictPid);
            if (processCacheMap != null) {
                processCacheMap.remove(evictKey);
                if (processCacheMap.isEmpty()) {
                    cacheData.remove(evictPid);
                }
            }
            
            log.trace("替换缓存条目: pid={}, key={}", evictPid, evictKey);
        }
    }
    
    /**
     * 获取缓存键
     * @param virtualAddress 虚拟地址
     * @return 缓存键
     */
    private String getCacheKey(VirtualAddress virtualAddress) {
        return String.format("%d:%d", 
                virtualAddress.getPageNumber(),
                virtualAddress.getOffset());
    }
    
    /**
     * 从缓存键解析虚拟地址
     * @param key 缓存键
     * @return 虚拟地址
     */
    private VirtualAddress parseVirtualAddressFromKey(String key) {
        String[] parts = key.split(":");
        if (parts.length == 2) {
            int pageNumber = Integer.parseInt(parts[0]);
            int offset = Integer.parseInt(parts[1]);
            return new VirtualAddress(pageNumber * 4096 + offset);
        }
        return null;
    }
    
    /**
     * 获取当前缓存条目总数
     * @return 缓存条目总数
     */
    private int getCacheEntryCount() {
        int count = 0;
        for (Map<String, CacheEntry> processCacheMap : cacheData.values()) {
            count += processCacheMap.size();
        }
        return count;
    }
    
    /**
     * 重新计算脏项数量
     * @return 脏项数量
     */
    private int calculateDirtyCount() {
        int count = 0;
        for (Map<String, CacheEntry> processCacheMap : cacheData.values()) {
            for (CacheEntry entry : processCacheMap.values()) {
                if (entry.isDirty()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 写回页面
     * @param virtualAddress 虚拟地址
     * @param processId 进程ID
     * @return 是否成功
     */
    public boolean writeBack(VirtualAddress virtualAddress, int processId) {
        int pageNumber = virtualAddress.getPageNumber();
        String key = processId + ":" + pageNumber;
        
        // 获取页面
        Page page = pageTable.getPage(virtualAddress, processId);
        if (page == null) {
            log.error("写回失败：页面不存在，进程={}，页号={}", processId, pageNumber);
            return false;
        }
        
        // 如果页面没有被修改，不需要写回
        if (!page.isDirty()) {
            return true;
        }
        
        // 获取页帧
        PageFrame frame = pageFrameTable.getFrame(page.getFrameNumber());
        if (frame == null) {
            log.error("写回失败：页帧不存在，进程={}，页号={}，帧号={}", 
                     processId, pageNumber, page.getFrameNumber());
            return false;
        }
        
        // 写回页面
        try {
            return writeBackPage(page, frame);
        } catch (IOException e) {
            log.error("写回失败：IO错误，进程={}，页号={}，错误={}", 
                     processId, pageNumber, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 写回页面
     * @param page 页面
     * @param frame 页帧
     * @return 是否成功
     * @throws IOException 如果写入交换区时发生IO错误
     */
    private boolean writeBackPage(Page page, PageFrame frame) throws IOException {
        // 准备缓冲区
        ByteBuffer buffer = ByteBuffer.allocateDirect(PAGE_SIZE);
        
        // 从物理内存读取数据
        byte[] data = physicalMemory.read(frame.getFrameNumber() * PAGE_SIZE, PAGE_SIZE);
        buffer.put(data);
        
        // 准备写入
        buffer.flip();
        
        // 写入交换区
        if (page.hasSwapLocation()) {
            swapChannel.write(buffer, page.getSwapLocation());
        } else {
            // 如果没有交换区位置，分配一个
            long swapLocation = swapManager.allocateSwapArea(page.getProcessId(), page.getPageNumber());
            if (swapLocation < 0) {
                log.error("写回失败：无法分配交换区位置，进程={}，页号={}", 
                         page.getProcessId(), page.getPageNumber());
                return false;
            }
            page.setSwapLocation(swapLocation);
            swapChannel.write(buffer, swapLocation);
        }
        
        // 清除脏位
        page.resetDirty();
        
        // 增加写回计数
        writeBackCount.incrementAndGet();
        
        log.debug("页面已写回：进程={}，页号={}，交换位置={}", 
                 page.getProcessId(), page.getPageNumber(), page.getSwapLocation());
        
        return true;
    }
} 