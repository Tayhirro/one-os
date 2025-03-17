package newOs.kernel.process;


import com.alibaba.fastjson.JSONObject;
import jdk.jfr.DataAmount;
import lombok.Data;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.InfoImpl.ProcessInfoReturnImpl;
import newOs.kernel.process.scheduler.ProcessScheduler;
import newOs.tools.ProcessTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static newOs.common.processConstant.processStateConstant.CREATED;

@Component
@Data
public class ProcessManager{
    //依赖注入:
    private final HashMap<Integer, PCB> pcbTable;
    private final Queue<PCB> readyQueue;
    private final Queue<PCB> runningQueue;
    private final Queue<PCB> waitingQueue;
    private final ProtectedMemory protectedMemory;
    private final X86CPUSimulator x86CPUSimulator;
    private final ProcessExecutionTaskFactory processExecutionTaskFactory;
    private final ProcessScheduler processScheduler;
    //没有实现中级调度




    @Autowired
    public ProcessManager(ProtectedMemory protectedMemory, X86CPUSimulator x86CPUSimulator, ProcessExecutionTaskFactory processExecutionTaskFactory, ProcessScheduler processScheduler){
        this.pcbTable = protectedMemory.getPcbTable();
        this.readyQueue = protectedMemory.getReadyQueue();
        this.runningQueue = protectedMemory.getRunningQueue();
        this.waitingQueue = protectedMemory.getWaitingQueue();



        this.protectedMemory = protectedMemory;
        this.x86CPUSimulator = x86CPUSimulator;
        this.processExecutionTaskFactory = processExecutionTaskFactory;
        this.processScheduler = processScheduler;
    }

    public ProcessInfoReturnImpl createProcess(String processName, JSONObject args,String[] instructions){
        // 创建进程
        int pid = ProcessTool.getPid(processName);
        // 创建进程pcb，放进pcbTable

        //创建时间戳
        long timestamp = System.currentTimeMillis();
        PCB pcb = new PCB(pid, processName, 0, -1, CREATED, -1, -1, -1, -1, timestamp, -1, -1, 3, null, -1, -1, -1);

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
        pcb.setInstructions(list.toArray(new String[1]));
        pcb.setExpectedTime(expectedTime);      //setExpectedtime
        // 写进文件系统  --暂时可以不用做
        ProcessInfoReturnImpl processRInfo = new ProcessInfoReturnImpl();
        return processRInfo;
    }

    public void executeProcess(PCB pcb){
        //
//        int pageTable = mmu.Allocate(pcb.getPid(), pcb.getSize());
//        pcb.setRegister(pageTable);
        ThreadPoolExecutor cpuSimulatorExecutor = (ThreadPoolExecutor) x86CPUSimulator.getExecutor();
        int idleThreads = cpuSimulatorExecutor.getMaximumPoolSize() - cpuSimulatorExecutor.getActiveCount();

        if(idleThreads > 0) {
            //有空闲进程
            ProcessExecutionTask processExecutionTask = processExecutionTaskFactory.createTask(pcb);
            //唤醒调度器
            cpuSimulatorExecutor.submit(processExecutionTask);
        }else{
            //没有空闲进程
            System.out.println("进程" + pcb.getPid() + "进入就绪队列");
            readyQueue.add(pcb);
        }
    }

}