package newOs.kernel.memory;

import lombok.extern.slf4j.Slf4j;
import newOs.kernel.memory.cache.TLB;

@Slf4j
public class TLBManager {
    
    private final TLB l1TLB;
    private final TLB l2TLB;
    
    /**
     * 构造函数
     * @param l1TLB 一级TLB
     * @param l2TLB 二级TLB
     */
    public TLBManager(TLB l1TLB, TLB l2TLB) {
        this.l1TLB = l1TLB;
        this.l2TLB = l2TLB;
    }
    
    /**
     * 调整TLB大小
     * @param size 新的大小
     */
    public void resize(int size) {
        if (l1TLB instanceof Resizable) {
            ((Resizable) l1TLB).resize(size);
        }
        if (l2TLB instanceof Resizable) {
            ((Resizable) l2TLB).resize(size * 4); // 二级缓存通常更大
        }
        log.info("TLB大小已调整为: {}", size);
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
     * 使特定进程的所有TLB条目失效
     * @param processId 进程ID
     */
    public void invalidateAll(int processId) {
        l1TLB.invalidateAll(processId);
        l2TLB.invalidateAll(processId);
        log.debug("进程{}的所有TLB条目已失效", processId);
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
     * 可调整大小的接口
     */
    private interface Resizable {
        void resize(int newSize);
    } 
} 