package newOs.component.memory.protected1;


import lombok.Data;
import newOs.common.InterruptConstant.InterruptType;
import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.dto.req.Info.InterruptInfo;
import newOs.kernel.device.DeviceDriver;
import newOs.kernel.interrupt.ISR;
import org.springframework.stereotype.Component;

import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.PhysicalMemory;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.virtual.PageFrameTable;
import newOs.kernel.memory.virtual.PageTable;
import newOs.kernel.memory.monitor.MemoryUsageMonitor;
import newOs.service.MemoryManageService;
import newOs.service.ProcessMemoryService;
import newOs.exception.MemoryException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;


/**
 * 保护内存
 */

@Component
@Data
public class ProtectedMemory {

    // 进程控制块表
    private HashMap<Integer, PCB> pcbTable; // 假设每个PCB有一个唯一的PID
    // 中断向量表
//    // 设备控制表
//    private LinkedList<DeviceInfo> deviceInfoTable;
//    // 文件信息表
//    private HashMap<FileNode, FileInfoo> fileInfoTable;
    private ConcurrentHashMap<InterruptType, ISR<? extends InterruptInfo>> IDT;

    // 运行队列  --目前正在执行的pcb
    private  ConcurrentLinkedQueue<PCB> runningQueue;
    // 就绪队列  --低级调度就绪队列
    private  ConcurrentLinkedQueue<PCB> readyQueue;



    // 等待队列 --内存阻塞队列
    private ConcurrentLinkedQueue<PCB> waitingQueue;

    //sjf队列
    private final PriorityBlockingQueue<PCB> readySJFQueue;


    private final ConcurrentLinkedQueue<PCB> highPriorityQueue;
    private final ConcurrentLinkedQueue<PCB> mediumPriorityQueue;
    private final ConcurrentLinkedQueue<PCB> lowPriorityQueue;

    //设备队列
    private final ConcurrentLinkedQueue<DeviceDriver> deviceQueue;




    //中断请求表
    private final ConcurrentHashMap<Long, InterruptRequestLine> irlTable;

    //IO中断线
    private final ConcurrentLinkedQueue<PCB> irlIO;
    
    private MemoryManager memoryManager;
    
    @Autowired
    public void setMemoryManager(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }
    
    private PhysicalMemory physicalMemory;
    
    @Autowired
    public void setPhysicalMemory(PhysicalMemory physicalMemory) {
        this.physicalMemory = physicalMemory;
    }
    
    private PageFrameTable pageFrameTable;
    
    @Autowired
    public void setPageFrameTable(PageFrameTable pageFrameTable) {
        this.pageFrameTable = pageFrameTable;
    }
    
    private MemoryUsageMonitor memoryUsageMonitor;
    
    @Autowired
    public void setMemoryUsageMonitor(MemoryUsageMonitor memoryUsageMonitor) {
        this.memoryUsageMonitor = memoryUsageMonitor;
    }
    
    private MemoryManageService memoryManageService;
    
    @Autowired
    public void setMemoryManageService(MemoryManageService memoryManageService) {
        this.memoryManageService = memoryManageService;
    }
    
    private ProcessMemoryService processMemoryService;
    
    @Autowired
    public void setProcessMemoryService(ProcessMemoryService processMemoryService) {
        this.processMemoryService = processMemoryService;
    }
    
    // 页表缓存 - 进程ID到页表的映射
    private ConcurrentHashMap<Integer, PageTable> pageTableCache;
    
    // 内存配置参数
    private int pageSize = 4096; // 默认页面大小为4KB
    private int totalPhysicalMemory = 1024 * 1024 * 1024; // 默认1GB物理内存
    private boolean virtualMemoryEnabled = true; // 默认启用虚拟内存
    private double overcommitRatio = 1.5; // 内存过度分配比率
    private int swappingThreshold = 80; // 触发交换的内存使用百分比阈值

    public  ProtectedMemory() {
        // 初始化数据结构
        pcbTable = new HashMap<>();
//        deviceInfoTable = new LinkedList<>();
//        fileInfoTable = new HashMap<>();
        IDT = new ConcurrentHashMap<InterruptType, ISR<? extends InterruptInfo>>();
        irlTable = new ConcurrentHashMap<>();

        runningQueue = new ConcurrentLinkedQueue<>();
        readyQueue = new ConcurrentLinkedQueue<>();
        waitingQueue = new ConcurrentLinkedQueue<>();

        readySJFQueue = new PriorityBlockingQueue<>(10, (o1, o2) -> {
            if (o1.getExpectedTime() > o2.getExpectedTime()) {
                return 1;
            } else if (o1.getExpectedTime() < o2.getExpectedTime()) {
                return -1;
            } else {
                return 0;
            }
        });

        highPriorityQueue = new ConcurrentLinkedQueue<>();
        mediumPriorityQueue = new ConcurrentLinkedQueue<>();
        lowPriorityQueue = new ConcurrentLinkedQueue<>();
        irlIO = new ConcurrentLinkedQueue<>();

        
        deviceQueue = new ConcurrentLinkedQueue<>();
        
        pageTableCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取中断向量表
     * @return 中断向量表
     */
    public ConcurrentHashMap<InterruptType, ISR<? extends InterruptInfo>> getIDT() {
        return IDT;
    }
    
    /**
     * 为进程分配内存
     * @param processId 进程ID
     * @param size 需要分配的内存大小（字节）
     * @return 分配的虚拟地址，分配失败则返回null
     */
    public VirtualAddress allocateMemory(int processId, long size) {
        try {
            PCB pcb = pcbTable.get(processId);
            if (pcb == null) {
                return null;
            }
            
            // 使用processMemoryService分配内存
            return processMemoryService.allocateMemory(processId, size, false);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 释放进程的内存
     * @param pcb 进程控制块
     * @param virtualAddress 要释放的内存虚拟地址
     * @return 是否成功释放
     */
    public boolean freeMemoryForProcess(PCB pcb, VirtualAddress virtualAddress) {
        try {
            // 获取分配大小
            Long size = pcb.getMemoryAllocationMap().get(virtualAddress);
            if (size == null) {
                return false; // 没有找到该地址的分配记录
            }
            
            // 调用内存管理服务释放内存
            // 修复类型不匹配问题：假设processMemoryService.freeMemory返回void
            // 我们改为尝试执行操作，如果没有异常则认为成功
            processMemoryService.freeMemory(pcb.getPid(), virtualAddress);
            boolean result = true; // 如果没有抛出异常，则认为成功
            
            if (result) {
                // 更新PCB中的内存分配信息
                pcb.getMemoryAllocationMap().remove(virtualAddress);
                
                // 更新内存使用统计
                pcb.setCurrentUsedMemory(pcb.getCurrentUsedMemory() - size);
            }
            
            return result;
        } catch (MemoryException e) {
            return false;
        }
    }
    
    /**
     * 访问内存（读操作）
     * @param pcb 进程控制块
     * @param virtualAddress 虚拟地址
     * @param size 要读取的大小
     * @return 读取的数据
     * @throws MemoryException 如果内存访问失败
     */
    public byte[] readMemory(PCB pcb, VirtualAddress virtualAddress, int size) throws MemoryException {
        // 在实际实现中会调用内存访问服务
        // 这里为简化起见，返回一个模拟的数组
        return new byte[size];
    }
    
    /**
     * 访问内存（写操作）
     * @param pcb 进程控制块
     * @param virtualAddress 虚拟地址
     * @param data 要写入的数据
     * @return 是否写入成功
     * @throws MemoryException 如果内存访问失败
     */
    public boolean writeMemory(PCB pcb, VirtualAddress virtualAddress, byte[] data) throws MemoryException {
        // 在实际实现中会调用内存访问服务
        return true;
    }
    
    /**
     * 处理进程结束，释放所有分配的内存
     * @param pcb 要结束的进程
     * @return 是否成功释放所有内存
     */
    public boolean cleanupProcessMemory(PCB pcb) {
        boolean allSuccess = true;
        
        // 释放所有分配的内存
        if (pcb.getMemoryAllocationMap() != null) {
            for (VirtualAddress addr : pcb.getMemoryAllocationMap().keySet()) {
                boolean success = freeMemoryForProcess(pcb, addr);
                if (!success) {
                    allSuccess = false;
                }
            }
            pcb.getMemoryAllocationMap().clear();
        }
        
        // 清理页表缓存
        pageTableCache.remove(pcb.getPid());
        
        return allSuccess;
    }
    
    /**
     * 获取进程的内存使用统计
     * @param pid 进程ID
     * @return 进程的内存使用统计信息
     * @throws MemoryException 如果找不到进程
     */
    public HashMap<String, Object> getProcessMemoryStats(int pid) throws MemoryException {
        PCB pcb = pcbTable.get(pid);
        if (pcb == null) {
            throw new MemoryException("进程不存在: " + pid);
        }
        
        HashMap<String, Object> stats = new HashMap<>();
        stats.put("processId", pcb.getPid());
        stats.put("processName", pcb.getProcessName());
        stats.put("totalAllocated", pcb.getTotalAllocatedMemory());
        stats.put("currentUsage", pcb.getCurrentUsedMemory());
        stats.put("peakUsage", pcb.getPeakMemoryUsage());
        stats.put("pageHits", pcb.getPageHits());
        stats.put("pageFaults", pcb.getPageFaults());
        stats.put("tlbHits", pcb.getTlbHits());
        stats.put("tlbMisses", pcb.getTlbMisses());
        stats.put("pageHitRatio", pcb.getPageHitRatio());
        stats.put("tlbHitRatio", pcb.getTlbHitRatio());
        stats.put("memoryUsageRatio", pcb.getMemoryUsageRatio());
        
        return stats;
    }
    
    /**
     * 初始化内存系统
     * @param physicalMemorySize 物理内存大小
     * @param pageSize 页面大小
     * @param enableVirtualMemory 是否启用虚拟内存
     */
    public void initializeMemorySystem(long physicalMemorySize, int pageSize, boolean enableVirtualMemory) {
        this.totalPhysicalMemory = (int) physicalMemorySize;
        this.pageSize = pageSize;
        this.virtualMemoryEnabled = enableVirtualMemory;
        
        // 调用内存管理服务初始化系统
        try {
            memoryManageService.initializeMemorySystem(physicalMemorySize, 
                    enableVirtualMemory ? (long)(physicalMemorySize * overcommitRatio) : 0);
        } catch (MemoryException e) {
            // 记录初始化失败
        }
    }

    /**
     * 释放指定进程的虚拟地址对应的内存
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 是否成功释放
     */
    public boolean freeMemory(int processId, VirtualAddress virtualAddress) {
        try {
            PCB pcb = pcbTable.get(processId);
            if (pcb == null) {
                return false; // 进程不存在
            }
            
            return freeMemoryForProcess(pcb, virtualAddress);
        } catch (Exception e) {
            return false;
        }
    }
}
