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
import static newOs.kernel.process.scheduler.ProcessScheduler.strategy;

@Component
@Data
public class ProcessManager{
    //依赖注入:
    private final HashMap<Integer, PCB> pcbTable;
    private final Queue<PCB> readyQueue;
    private final Queue<PCB> runningQueue;
    private final Queue<PCB> waitingQueue;
    private final Queue<PCB> readySJFQueue;
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
        this.readySJFQueue = protectedMemory.getReadySJFQueue();


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
        PCB pcb = new PCB(pid, processName, 0, -1, CREATED, -1, -1, -1, -1, timestamp, -1, -1, 3, null, -1, -1, -1,-1);

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
        try{
    //        int pageTable = mmu.Allocate(pcb.getPid(), pcb.getSize());
    //        pcb.setRegister(pageTable);
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
                }else{  //没有就继续
                    continue;
                }
            }
            //循环完都没有
            if(i == cpuSimulatorExecutors.length) {
                //加入就绪队列
                System.out.println("进程" + pcb.getPid() + "进入就绪队列");
                if(strategy.equals("SJRF")||strategy.equals("SJF")){
                    readySJFQueue.add(pcb);
                }else{
                    readyQueue.add(pcb);
                }

                x86CPUSimulator.getExecutorServiceReady().get(0).incrementAndGet(); //进行安全自增
            }
        } finally { //延时一段用于 activecount的数值更新
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}