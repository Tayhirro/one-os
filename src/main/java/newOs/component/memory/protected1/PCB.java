package newOs.component.memory.protected1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.model.PhysicalAddress;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 *
 * 8 12 12
 * 段表基址寄存器 页表基址寄存器 页内偏移
 *  256段数  4k页数  4k大小的页
 */
public class PCB {
    // 进程id
    private int pid;
    // 进程名
    private String processName;

    //上下文
    // 指令寄存器，保存当前正在执行指令地址
    private int ir;
    // 进程大小，单位为B
    private int size;
    // 进程状态
    private String state;
    // 页表基址寄存器
    private int PBTR;
    // 段表基址寄存器
    private int SBTR;
    // 页表大小
    private int pageTableSize;
    //段表大小
    private int segmentTableSize;

    //进程创建时间
    private long timeStamp;

    // 剩余可执行时间，单位ms
    private long remainingTime;
    // 预期运行时间
    private long expectedTime;
    // 进程优先级
    private int priority;
    // 指令集
    private String[] instructions;

    //LRU算法执行换入换出时间
    private int swapInTime;

    private int swapOutTime;

    private int pageFaultRate;

    //上一次的核心id
    private Integer coreId;
    
    // 进程的内存分配信息
    private Map<VirtualAddress, Long> memoryAllocationMap; // 虚拟地址 -> 分配大小
    
    // 进程的内存使用情况统计
    private long totalAllocatedMemory; // 总分配内存
    private long currentUsedMemory;    // 当前使用内存
    private long peakMemoryUsage;      // 峰值内存使用
    
    // 页面管理相关
    private List<Integer> allocatedPageFrames; // 已分配页帧列表
    private int pageHits;                     // 页命中次数
    private int pageFaults;                   // 页缺失次数
    
    // TLB相关
    private int tlbHits;                      // TLB命中次数
    private int tlbMisses;                    // TLB未命中次数
    
    // 内存保护相关
    private Map<VirtualAddress, String> memoryAccessRights; // 内存访问权限映射
    
    // 内存段相关信息
    private List<MemorySegment> memorySegments; // 进程内存段列表
    
    /**
     * 内存段信息类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemorySegment {
        private VirtualAddress startAddress;  // 段起始虚拟地址
        private long size;                    // 段大小
        private String type;                  // 段类型（如代码段、数据段、堆、栈等）
        private String accessRights;          // 访问权限
    }
    
    /**
     * 获取进程当前内存使用率
     * @return 内存使用率（0.0-1.0）
     */
    public double getMemoryUsageRatio() {
        return totalAllocatedMemory > 0 ? (double) currentUsedMemory / totalAllocatedMemory : 0.0;
    }
    
    /**
     * 获取页面命中率
     * @return 页面命中率（0.0-1.0）
     */
    public double getPageHitRatio() {
        int totalAccesses = pageHits + pageFaults;
        return totalAccesses > 0 ? (double) pageHits / totalAccesses : 0.0;
    }
    
    /**
     * 获取TLB命中率
     * @return TLB命中率（0.0-1.0）
     */
    public double getTlbHitRatio() {
        int totalAccesses = tlbHits + tlbMisses;
        return totalAccesses > 0 ? (double) tlbHits / totalAccesses : 0.0;
    }
    
    /**
     * 更新内存访问统计
     * @param isPageHit 是否页命中
     * @param isTlbHit 是否TLB命中
     */
    public void updateMemoryAccessStats(boolean isPageHit, boolean isTlbHit) {
        if (isPageHit) {
            this.pageHits++;
        } else {
            this.pageFaults++;
        }
        
        if (isTlbHit) {
            this.tlbHits++;
        } else {
            this.tlbMisses++;
        }
    }
    
    /**
     * 获取进程内存布局信息
     * @return 内存布局描述
     */
    public String getMemoryLayout() {
        StringBuilder layout = new StringBuilder();
        layout.append("进程ID: ").append(pid).append("\n");
        layout.append("进程名称: ").append(processName).append("\n");
        layout.append("总分配内存: ").append(totalAllocatedMemory).append(" 字节\n");
        layout.append("当前使用内存: ").append(currentUsedMemory).append(" 字节\n");
        layout.append("内存使用率: ").append(String.format("%.2f%%", getMemoryUsageRatio() * 100)).append("\n");
        
        if (memorySegments != null && !memorySegments.isEmpty()) {
            layout.append("\n内存段信息:\n");
            for (int i = 0; i < memorySegments.size(); i++) {
                MemorySegment segment = memorySegments.get(i);
                layout.append(i + 1).append(". ");
                layout.append("类型: ").append(segment.getType()).append(", ");
                layout.append("起始地址: ").append(segment.getStartAddress()).append(", ");
                layout.append("大小: ").append(segment.getSize()).append(" 字节, ");
                layout.append("权限: ").append(segment.getAccessRights()).append("\n");
            }
        }
        
        return layout.toString();
    }
    
    /**
     * 检查指定虚拟地址是否有访问权限
     * @param virtualAddress 要检查的虚拟地址
     * @param accessType 访问类型（"READ", "WRITE", "EXECUTE"）
     * @return 是否有访问权限
     */
    public boolean hasAccessPermission(VirtualAddress virtualAddress, String accessType) {
        if (memoryAccessRights == null || !memoryAccessRights.containsKey(virtualAddress)) {
            return false; // 默认没有权限
        }
        
        String rights = memoryAccessRights.get(virtualAddress);
        
        switch (accessType.toUpperCase()) {
            case "READ":
                return rights.contains("R");
            case "WRITE":
                return rights.contains("W");
            case "EXECUTE":
                return rights.contains("X");
            default:
                return false;
        }
    }
    
    /**
     * 设置内存访问权限
     * @param virtualAddress 虚拟地址
     * @param accessRights 权限字符串 (如 "RW", "RX", "RWX")
     */
    public void setMemoryAccessPermission(VirtualAddress virtualAddress, String accessRights) {
        if (memoryAccessRights == null) {
            memoryAccessRights = new HashMap<>();
        }
        
        memoryAccessRights.put(virtualAddress, accessRights);
    }
    
    /**
     * 添加内存段
     * @param segment 要添加的内存段
     */
    public void addMemorySegment(MemorySegment segment) {
        if (memorySegments == null) {
            memorySegments = new ArrayList<>();
        }
        
        memorySegments.add(segment);
    }

    /**
     * 代码段起始地址
     */
    private VirtualAddress codeSegmentStart;
    
    /**
     * 代码段大小
     */
    private int codeSegmentSize;
    
    /**
     * 栈大小
     */
    private int stackSize;
    
    /**
     * 进程信号列表
     */
    private List<Signal> signals = new LinkedList<>();
    
    /**
     * 信号类，表示发送给进程的信号
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Signal {
        private String name;        // 信号名称
        private String message;     // 信号消息
        private long timestamp;     // 时间戳
    }
    
    /**
     * 获取代码段起始地址
     * @return 代码段起始地址
     */
    public VirtualAddress getCodeSegmentStart() {
        return codeSegmentStart;
    }
    
    /**
     * 设置代码段起始地址
     * @param codeSegmentStart 代码段起始地址
     */
    public void setCodeSegmentStart(VirtualAddress codeSegmentStart) {
        this.codeSegmentStart = codeSegmentStart;
    }
    
    /**
     * 获取代码段大小
     * @return 代码段大小(字节)
     */
    public int getCodeSegmentSize() {
        return codeSegmentSize;
    }
    
    /**
     * 设置代码段大小
     * @param codeSegmentSize 代码段大小(字节)
     */
    public void setCodeSegmentSize(int codeSegmentSize) {
        this.codeSegmentSize = codeSegmentSize;
    }
    
    /**
     * 获取栈大小
     * @return 栈大小(字节)
     */
    public int getStackSize() {
        return stackSize;
    }
    
    /**
     * 设置栈大小
     * @param stackSize 栈大小(字节)
     */
    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }
    
    /**
     * 获取进程信号列表
     * @return 信号列表
     */
    public List<Signal> getSignals() {
        return signals;
    }
    
    /**
     * 设置进程信号列表
     * @param signals 信号列表
     */
    public void setSignals(List<Signal> signals) {
        this.signals = signals;
    }
    
    /**
     * 添加信号
     * @param signal 要添加的信号
     */
    public void addSignal(Signal signal) {
        if (signals == null) {
            signals = new LinkedList<>();
        }
        signals.add(signal);
    }

    /**
     * 堆区结束地址
     */
    private VirtualAddress heapEnd;
    
    /**
     * 设置堆区结束地址
     * @param heapEnd 堆区结束地址
     */
    public void setHeapEnd(VirtualAddress heapEnd) {
        this.heapEnd = heapEnd;
    }
    
    /**
     * 获取堆区结束地址
     * @return 堆区结束地址
     */
    public VirtualAddress getHeapEnd() {
        return heapEnd;
    }
}
