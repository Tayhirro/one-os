package newOs.kernel.process;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import newOs.dto.req.Info.InfoImplDTO.ProcessInfoReturnImplDTO;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.virtual.protection.MemoryProtection;
import newOs.kernel.process.scheduler.ProcessScheduler;
import newOs.tools.ProcessTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static newOs.common.processConstant.processStateConstant.CREATED;
import static newOs.kernel.process.scheduler.ProcessScheduler.strategy;
import static newOs.common.processConstant.processStateConstant.TERMINATED;

/**
 * 进程管理器实现类
 * 负责进程的创建、执行、调度和资源管理
 */
@Component
@Data
@Slf4j
public class ProcessManagerImpl implements ProcessManager {
    
    // 保护模式内存控制器
    private final ProtectedMemory protectedMemory;
    
    // CPU模拟器
    private final X86CPUSimulator x86CPUSimulator;
    
    // 进程执行任务工厂
    private ProcessExecutionTaskFactory processExecutionTaskFactory;
    
    // 进程调度器
    private ProcessScheduler processScheduler;
    
    // PCB表和进程队列的引用
    private final HashMap<Integer, PCB> pcbTable;
    private final Queue<PCB> readyQueue;
    private final Queue<PCB> runningQueue;
    private final Queue<PCB> waitingQueue;
    private final Queue<PCB> readySJFQueue;
    
    // 内存保护组件
    private MemoryProtection memoryProtection;

    /**
     * 构造进程管理器
     * @param protectedMemory 保护模式内存
     * @param x86CPUSimulator CPU模拟器
     */
    @Autowired
    public ProcessManagerImpl(ProtectedMemory protectedMemory, X86CPUSimulator x86CPUSimulator){
        this.protectedMemory = protectedMemory;
        this.x86CPUSimulator = x86CPUSimulator;
        
        // 初始化进程相关字段
        this.pcbTable = protectedMemory.getPcbTable();
        this.readyQueue = protectedMemory.getReadyQueue();
        this.runningQueue = protectedMemory.getRunningQueue();
        this.waitingQueue = protectedMemory.getWaitingQueue();
        this.readySJFQueue = protectedMemory.getReadySJFQueue();
    }
    
    /**
     * 设置进程执行任务工厂
     * @param processExecutionTaskFactory 进程执行任务工厂
     */
    @Autowired
    public void setProcessExecutionTaskFactory(ProcessExecutionTaskFactory processExecutionTaskFactory) {
        this.processExecutionTaskFactory = processExecutionTaskFactory;
    }
    
    /**
     * 设置进程调度器
     * @param processScheduler 进程调度器
     */
    @Autowired
    public void setProcessScheduler(ProcessScheduler processScheduler) {
        this.processScheduler = processScheduler;
    }
    
    /**
     * 设置内存保护组件
     * @param memoryProtection 内存保护组件
     */
    @Autowired
    public void setMemoryProtection(MemoryProtection memoryProtection) {
        this.memoryProtection = memoryProtection;
    }

    @Override
    public ProcessInfoReturnImplDTO createProcess(String processName, JSONObject args, String[] instructions){
        // 创建进程
        int pid = ProcessTool.getPid(processName);
        
        // 创建基本PCB对象 - 使用无参构造函数然后设置属性
        PCB pcb = new PCB();
        pcb.setPid(pid);
        pcb.setProcessName(processName);
        pcb.setIr(0);
        pcb.setSize(-1);
        pcb.setState(CREATED);
        pcb.setPBTR(-1);
        pcb.setSBTR(-1);
        pcb.setPageTableSize(-1);
        pcb.setSegmentTableSize(-1);
        pcb.setTimeStamp(System.currentTimeMillis());
        pcb.setRemainingTime(-1);
        pcb.setExpectedTime(-1);
        pcb.setPriority(3);
        pcb.setInstructions(null);
        pcb.setSwapInTime(-1);
        pcb.setSwapOutTime(-1);
        pcb.setPageFaultRate(-1);
        pcb.setCoreId(-1);
        
        // 初始化内存管理相关字段
        pcb.setMemoryAllocationMap(new HashMap<>());
        pcb.setTotalAllocatedMemory(0);
        pcb.setCurrentUsedMemory(0);
        pcb.setPeakMemoryUsage(0);
        pcb.setPageHits(0);
        pcb.setPageFaults(0);
        pcb.setTlbHits(0);
        pcb.setTlbMisses(0);

        pcbTable.put(pid, pcb);
        
        LinkedList<String> list = new LinkedList<>();
        long expectedTime = 0;
        //设置pcb的基础内容
        for (String inst : instructions) {
            if (inst.charAt(0) == 'M') {
                pcb.setSize(inst.charAt(2) * 1024);
            } else {
                list.add(inst);
                if (inst.charAt(0) == 'C') {
                    expectedTime += Long.parseLong(inst.split(" ")[1]);
                }
                if (inst.charAt(0) == 'R') {
                    expectedTime += Long.parseLong(inst.split(" ")[2]);
                }
                if (inst.charAt(0) == 'W') {
                    expectedTime += Long.parseLong(inst.split(" ")[2]);
                }
            }
        }
        pcb.setInstructions(list.toArray(new String[0]));
        pcb.setExpectedTime(expectedTime);
        
        // 写进文件系统  --暂时可以不用做
        ProcessInfoReturnImplDTO processRInfo = new ProcessInfoReturnImplDTO();
        return processRInfo;
    }

    @Override
    public void executeProcess(PCB pcb){
        //
        try{
            // 检查进程内存状态
            if (pcb.getMemoryAllocationMap() == null || pcb.getMemoryAllocationMap().isEmpty()) {
                log.error("进程 " + pcb.getPid() + " 未分配内存，无法执行");
                return;
            }
            
            ExecutorService[] cpuSimulatorExecutors = x86CPUSimulator.getExecutors();
            int i = 1;
            for(;i<cpuSimulatorExecutors.length;i++){
                ThreadPoolExecutor executor = (ThreadPoolExecutor) cpuSimulatorExecutors[i];
                int idleThreads = executor.getCorePoolSize() - executor.getActiveCount();
                if(idleThreads > 0) {
                    //有空闲进程
                    ProcessExecutionTask processExecutionTask = processExecutionTaskFactory.createTask(pcb);
                    //设置cordid
                    pcb.setCoreId(i);
                    //唤醒调度器
                    cpuSimulatorExecutors[i].submit(processExecutionTask);
                    break;
                }
            }
            //循环完都没有
            if(i == cpuSimulatorExecutors.length) {
                //加入就绪队列
                log.info("进程{}-{}进入就绪队列", pcb.getCoreId(), pcb.getPid());
                if(strategy.equals("SJRF") || strategy.equals("SJF")){
                    readySJFQueue.add(pcb);
                } else {
                    readyQueue.add(pcb);
                }

                x86CPUSimulator.getExecutorServiceReady().get(0).incrementAndGet(); //进行自增
                log.debug("就绪队列计数: {}", x86CPUSimulator.getExecutorServiceReady().get(0).get());
            }
        } finally { //延时一段用于 activecount的数值更新
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                log.error("执行进程时出现中断异常", e);
            }
        }
    }

    @Override
    public VirtualAddress getHeapStart(int processId) {
        // 默认实现
        return new VirtualAddress(0);
    }

    @Override
    public VirtualAddress getHeapEnd(int processId) {
        // 默认实现
        return new VirtualAddress(0);
    }

    @Override
    public VirtualAddress getStackStart(int processId) {
        // 默认实现
        return new VirtualAddress(0);
    }

    @Override
    public VirtualAddress getStackEnd(int processId) {
        // 默认实现
        return new VirtualAddress(0);
    }

    @Override
    public VirtualAddress getCodeStart(int processId) {
        // 默认实现
        return new VirtualAddress(0);
    }

    @Override
    public VirtualAddress getCodeEnd(int processId) {
        // 默认实现
        return new VirtualAddress(0);
    }

    @Override
    public VirtualAddress getDataStart(int processId) {
        // 默认实现
        return new VirtualAddress(0);
    }

    @Override
    public VirtualAddress getDataEnd(int processId) {
        // 默认实现
        return new VirtualAddress(0);
    }

    @Override
    public boolean growHeap(int processId, int size) {
        // 默认实现
        return false;
    }

    @Override
    public boolean shrinkHeap(int processId, int size) {
        // 默认实现
        return false;
    }

    @Override
    public boolean isValidAddress(int processId, VirtualAddress address) {
        // 默认实现
        return false;
    }

    @Override
    public void cleanupProcessMemory(int processId) {
        // 默认实现
    }

    @Override
    public VirtualAddress getCurrentInstructionPointer(int processId) {
        // 默认实现
        return new VirtualAddress(0);
    }

    @Override
    public VirtualAddress getCurrentStackPointer(int processId) {
        // 默认实现
        return new VirtualAddress(0);
    }
    
    @Override
    public boolean sendSignal(int pid, String signalName, String message) {
        log.debug("向进程{}发送信号: {}, 消息: {}", pid, signalName, message);
        
        try {
            // 查找进程
            PCB pcb = pcbTable.get(pid);
            if (pcb == null) {
                log.error("进程不存在: {}", pid);
                return false;
            }
            
            // 构造信号对象
            Map<String, Object> signal = new HashMap<>();
            signal.put("name", signalName);
            signal.put("message", message);
            signal.put("timestamp", System.currentTimeMillis());
            
            // 记录信号发送
            log.info("信号已发送: 进程={}, 信号={}, 消息={}", pid, signalName, message);
            
            // 处理特殊信号 - 如果是段错误(SIGSEGV)，立即终止进程
            if ("SIGSEGV".equals(signalName)) {
                log.error("进程{}因段错误(SIGSEGV)被强制终止", pid);
                pcb.setState(TERMINATED);
                
                // 回收进程资源
                terminateProcess(pid);
                
                // 没有直接调度器的调度方法，依赖terminateProcess方法处理
            }
            
            return true;
        } catch (Exception e) {
            log.error("发送信号失败: 进程={}, 信号={}, 错误={}", pid, signalName, e.getMessage());
            return false;
        }
    }

    /**
     * 终止进程并清理其内存资源
     * @param pid 要终止的进程ID
     * @return 是否成功终止
     */
    @Override
    public boolean terminateProcess(int pid) {
        PCB pcb = pcbTable.get(pid);
        if (pcb == null) {
            log.warn("尝试终止不存在的进程: {}", pid);
            return false;
        }
        
        log.info("终止进程: {}", pid);
        
        boolean success = true;
        
        // 释放进程的所有内存资源
        try {
            // 从PCB的内存分配表中获取所有已分配的内存地址
            if (pcb.getMemoryAllocationMap() != null && !pcb.getMemoryAllocationMap().isEmpty()) {
                // 创建一个副本以避免ConcurrentModificationException
                Map<VirtualAddress, Long> memoryMap = new HashMap<>(pcb.getMemoryAllocationMap());
                
                for (Map.Entry<VirtualAddress, Long> entry : memoryMap.entrySet()) {
                    try {
                        VirtualAddress address = entry.getKey();
                        log.debug("释放进程{}的内存: 地址=0x{}, 大小={}字节", 
                                pid, Long.toHexString(address.getValue()), entry.getValue());
                        protectedMemory.freeMemory(pid, address);
                    } catch (Exception e) {
                        log.warn("释放进程{}的内存地址0x{}时出错: {}", 
                                pid, entry.getKey().getValue(), e.getMessage());
                        // 继续执行，不中断整个终止过程
                        success = false;
                    }
                }
            } else {
                log.debug("进程{}没有已分配的内存需要释放", pid);
            }
            
            // 从进程表中移除
            pcbTable.remove(pid);
            
            // 通知调度器进程已终止
            processScheduler.removeProcess(pcb);
            
            return success;
        } catch (Exception e) {
            log.error("终止进程 {} 时出错: {}", pid, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取进程的内存使用情况
     * @param pid 进程ID
     * @return 进程内存使用情况的描述字符串
     */
    @Override
    public String getProcessMemoryInfo(int pid) {
        PCB pcb = pcbTable.get(pid);
        if (pcb == null) {
            return "进程不存在: " + pid;
        }
        
        StringBuilder info = new StringBuilder();
        info.append("进程 ").append(pid).append(" 内存使用情况:\n");
        
        // 代码段信息
        VirtualAddress codeStart = pcb.getCodeSegmentStart();
        long codeSize = pcb.getCodeSegmentSize();
        info.append("代码段: 起始地址=0x").append(Long.toHexString(codeStart != null ? codeStart.getValue() : 0))
            .append(", 大小=").append(codeSize).append(" 字节\n");
        
        // 堆信息
        VirtualAddress heapStart = getHeapStart(pid);
        VirtualAddress heapEnd = getHeapEnd(pid);
        long heapSize = heapEnd != null && heapStart != null ? 
                heapEnd.getValue() - heapStart.getValue() : 0;
        info.append("堆: 起始地址=0x").append(Long.toHexString(heapStart != null ? heapStart.getValue() : 0))
            .append(", 大小=").append(heapSize).append(" 字节\n");
        
        // 栈信息
        VirtualAddress stackStart = getStackStart(pid);
        long stackSize = pcb.getStackSize();
        info.append("栈: 起始地址=0x").append(Long.toHexString(stackStart != null ? stackStart.getValue() : 0))
            .append(", 大小=").append(stackSize).append(" 字节\n");
        
        // 总内存使用
        long totalMemory = codeSize + heapSize + stackSize;
        info.append("总内存使用: ").append(totalMemory).append(" 字节");
        
        return info.toString();
    }
    
    /**
     * 为进程分配额外内存
     * @param pid 进程ID
     * @param size 要分配的内存大小(字节)
     * @return 分配的虚拟地址，失败返回null
     */
    @Override
    public VirtualAddress allocateMemoryForProcess(int pid, long size) {
        PCB pcb = pcbTable.get(pid);
        if (pcb == null) {
            log.warn("尝试为不存在的进程 {} 分配内存", pid);
            return null;
        }
        
        try {
            // 在堆上分配内存
            VirtualAddress newMemory = protectedMemory.allocateMemory(pid, size);
            
            // 更新堆结束地址
            if (newMemory != null) {
                pcb.setHeapEnd(new VirtualAddress(newMemory.getValue() + size));
            }
            
            return newMemory;
        } catch (Exception e) {
            log.error("为进程 {} 分配 {} 字节内存时出错: {}", pid, size, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 释放进程的内存
     * @param pid 进程ID
     * @param address 要释放的内存地址
     * @return 是否成功释放
     */
    @Override
    public boolean freeProcessMemory(int pid, VirtualAddress address) {
        PCB pcb = pcbTable.get(pid);
        if (pcb == null) {
            log.warn("尝试释放不存在进程 {} 的内存", pid);
            return false;
        }
        
        try {
            // 释放指定地址的内存
            protectedMemory.freeMemory(pid, address);
            return true;
        } catch (Exception e) {
            log.error("释放进程 {} 地址 {} 的内存时出错: {}", 
                    pid, address.getValue(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 改变内存段的访问权限
     * @param pid 进程ID
     * @param address 内存地址
     * @param permissions 新的权限字符串 (如 "RW", "RX", "RWX")
     * @return 是否成功更改
     */
    @Override
    public boolean changeMemoryPermissions(int pid, VirtualAddress address, String permissions) {
        if (!pcbTable.containsKey(pid)) {
            log.warn("尝试更改不存在进程 {} 的内存权限", pid);
            return false;
        }
        
        try {
            // 解析权限字符串
            boolean canRead = permissions.contains("R");
            boolean canWrite = permissions.contains("W");
            boolean canExecute = permissions.contains("X");
            
            // 修改权限
            memoryProtection.setAccessControl(pid, address, 4096, canRead, canWrite, canExecute);
            return true;
        } catch (Exception e) {
            log.error("更改进程 {} 地址 {} 的内存权限时出错: {}", 
                    pid, address.getValue(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查进程是否存在
     * @param pid 进程ID
     * @return 进程是否存在
     */
    public boolean isProcessExist(int pid) {
        return pcbTable.containsKey(pid);
    }
    
    /**
     * 获取进程控制块
     * @param pid 进程ID
     * @return 进程控制块，不存在则返回null
     */
    public PCB getProcess(int pid) {
        return pcbTable.get(pid);
    }
}