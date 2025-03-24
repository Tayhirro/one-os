package newOs.kernel.memory.allocation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存回收器
 * 负责回收不再使用的内存，包括终止进程的内存和长时间未访问的交换内存
 */
@Component("allocationMemoryReclaimer")
@Data
@Slf4j
public class MemoryReclaimer {
    
    // 内存分配器
    private final MemoryAllocator memoryAllocator;
    
    // 进程控制块表
    private HashMap<Integer, PCB> pcbTable;
    
    // 正在被监控的进程ID集合
    private final Set<Integer> monitoredProcesses;
    
    // 上一次进程内存使用记录，用于检测内存泄漏
    private final Map<Integer, Integer> lastMemoryUsage;
    
    /**
     * 构造内存回收器
     * @param memoryAllocator 内存分配器
     */
    @Autowired
    public MemoryReclaimer(MemoryAllocator memoryAllocator) {
        this.memoryAllocator = memoryAllocator;
        this.monitoredProcesses = ConcurrentHashMap.newKeySet();
        this.lastMemoryUsage = new ConcurrentHashMap<>();
    }
    
    /**
     * 设置Protected Memory
     * @param protectedMemory 受保护内存对象
     */
    @Autowired
    public void setProtectedMemory(ProtectedMemory protectedMemory) {
        this.pcbTable = protectedMemory.getPcbTable();
    }
    
    /**
     * 回收已终止进程的内存
     * @return 回收的内存块数量
     */
    public int reclaimTerminatedProcessMemory() {
        int totalReclaimed = 0;
        
        for (Map.Entry<Integer, PCB> entry : pcbTable.entrySet()) {
            int pid = entry.getKey();
            PCB pcb = entry.getValue();
            
            // 如果进程已终止但内存未回收
            if (isProcessTerminated(pcb)) {
                int reclaimed = memoryAllocator.freeAll(pid);
                if (reclaimed > 0) {
                    log.info("回收已终止进程(PID={})的内存: {} 块", pid, reclaimed);
                    totalReclaimed += reclaimed;
                }
            }
        }
        
        return totalReclaimed;
    }
    
    /**
     * 判断进程是否已终止
     * @param pcb 进程控制块
     * @return 是否已终止
     */
    private boolean isProcessTerminated(PCB pcb) {
        // 根据状态判断进程是否已终止
        String state = pcb.getState();
        return "TERMINATED".equals(state) || "EXIT".equals(state);
    }
    
    /**
     * 添加需要监控的进程
     * @param pid 进程ID
     */
    public void monitorProcess(int pid) {
        monitoredProcesses.add(pid);
        lastMemoryUsage.put(pid, memoryAllocator.getProcessAllocatedSize(pid));
    }
    
    /**
     * 移除监控的进程
     * @param pid 进程ID
     */
    public void unmonitorProcess(int pid) {
        monitoredProcesses.remove(pid);
        lastMemoryUsage.remove(pid);
    }
    
    /**
     * 检测内存泄漏
     * 如果进程内存使用持续增长，可能存在内存泄漏
     */
    public void detectMemoryLeaks() {
        for (Integer pid : monitoredProcesses) {
            PCB pcb = pcbTable.get(pid);
            if (pcb == null) {
                unmonitorProcess(pid);
                continue;
            }
            
            int currentUsage = memoryAllocator.getProcessAllocatedSize(pid);
            int lastUsage = lastMemoryUsage.getOrDefault(pid, 0);
            
            // 如果内存使用增长超过阈值，记录可能的泄漏
            if (currentUsage > lastUsage && currentUsage - lastUsage > 1024 * 10) { // 假设阈值为10KB
                log.warn("可能的内存泄漏: 进程(PID={})内存使用从{}字节增加到{}字节", 
                        pid, lastUsage, currentUsage);
            }
            
            // 更新记录
            lastMemoryUsage.put(pid, currentUsage);
        }
    }
    
    /**
     * 定时任务：回收内存
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void scheduledMemoryReclaim() {
        log.debug("执行定时内存回收...");
        
        int reclaimed = reclaimTerminatedProcessMemory();
        
        log.info("定时内存回收结果: 回收了{}块内存, 当前空闲内存: {}字节, 碎片率: {:.2f}%", 
                reclaimed, 
                memoryAllocator.getFreeMemorySize(),
                memoryAllocator.getFragmentationRatio() * 100);
        
        // 检测内存泄漏
        detectMemoryLeaks();
    }
    
    /**
     * 强制进行内存回收
     * @return 回收的内存块数量
     */
    public int forceMemoryReclaim() {
        return reclaimTerminatedProcessMemory();
    }
    
    /**
     * 释放指定内存块
     * @param block 内存块
     * @param pid 进程ID
     * @return 是否成功释放
     */
    public boolean free(MemoryBlock block, int pid) {
        if (block == null) {
            return false;
        }
        // 使用内存分配器释放内存
        return memoryAllocator.free(block.getStartAddress(), pid);
    }
    
    /**
     * 回收内存
     * @return 回收的内存大小（字节）
     */
    public long reclaimMemory() {
        log.info("执行内存回收");
        int blocksReclaimed = reclaimTerminatedProcessMemory();
        long bytesReclaimed = 0;
        
        // 估计回收的内存大小
        if (blocksReclaimed > 0 && memoryAllocator != null) {
            bytesReclaimed = memoryAllocator.getFreeMemorySize();
        }
        
        log.info("内存回收完成: {} 个内存块, 估计 {} 字节", blocksReclaimed, bytesReclaimed);
        return bytesReclaimed;
    }
} 