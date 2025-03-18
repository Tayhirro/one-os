package newOs.kernel.process.scheduler;


import lombok.extern.slf4j.Slf4j;
import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.interrupt.hardwareHandler.ISRHandler;
import newOs.kernel.process.ProcessExecutionTask;
import newOs.kernel.process.ProcessExecutionTaskFactory;
import newOs.kernel.process.ProcessManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;

import static newOs.common.processConstant.processStateConstant.*;
import static newOs.kernel.process.scheduler.ProcessScheduler.strategy;

@Component
@Slf4j
public class SideScheduler {
    private final ConcurrentLinkedQueue<PCB> readyQueue;
    private final ConcurrentLinkedQueue<PCB> runningQueue;
    private final ConcurrentLinkedQueue<PCB> waitingQueue;

    //mlfq的边缘调度
    private final ConcurrentLinkedQueue<PCB> highPriorityQueue;
    private final ConcurrentLinkedQueue<PCB> mediumPriorityQueue;
    private final ConcurrentLinkedQueue<PCB> lowPriorityQueue;

    private final PriorityBlockingQueue<PCB> readySJFQueue;
    private final X86CPUSimulator x86CPUSimulator;


    private ExecutorService cpuSimulatorExecutor;
    private final ProtectedMemory protectedMemory;
    private final ConcurrentHashMap<Long, InterruptRequestLine> irlTable;
    private final ConcurrentLinkedQueue<Integer> irlIO;

    private final ISRHandler isrHandler;

    /*
    * 1实现调度下一个进程 ----放入runningQueue
    * 2实现初始调度进程
    *
     */



    @Autowired
    public SideScheduler(ProtectedMemory protectedMemory, X86CPUSimulator x86CPUSimulator, ISRHandler isrHandler) {



        this.readyQueue = protectedMemory.getReadyQueue();
        this.runningQueue = protectedMemory.getRunningQueue();
        this.waitingQueue = protectedMemory.getWaitingQueue();
        this.highPriorityQueue = protectedMemory.getHighPriorityQueue();
        this.mediumPriorityQueue = protectedMemory.getMediumPriorityQueue();
        this.lowPriorityQueue = protectedMemory.getLowPriorityQueue();
        this.readySJFQueue = protectedMemory.getReadySJFQueue();


        this.protectedMemory = protectedMemory;
        this.irlTable = protectedMemory.getIrlTable();
        this.irlIO  = protectedMemory.getIrlIO();

        //用于创建进程
        this.x86CPUSimulator = x86CPUSimulator;
        this.isrHandler = isrHandler;
    }


    public void schedulerProcess(PCB pcb){
        if(strategy.equals("SJF")||strategy.equals("SRJF")) {
            readySJFQueue.removeIf(p -> p.equals(pcb));
        }else {
            readyQueue.removeIf(p -> p.equals(pcb));
        }
        //自减
        x86CPUSimulator.getExecutorServiceReady().get(pcb.getCoreId()).decrementAndGet();
        //自减

        pcb.setState(RUNNING);
        //需要实现时间片控制
        if (strategy.equals("RR")) // 时间片轮转
            pcb.setRemainingTime(3800L);
        else if (strategy.equals("MLFQ") && pcb.getRemainingTime() < 0)    // 多级反馈队列
            pcb.setRemainingTime(3800L);
            //不需要实现时间片控制
        else if (strategy.equals("FCFS") || strategy.equals("SJF") || strategy.equals("SRJF"))
            pcb.setRemainingTime(99999999L);
        //多级调度
        if(strategy.equals("MLFQ")) {
            if (pcb.getPriority() == 1) {
                lowPriorityQueue.add(pcb);
            } else if (pcb.getPriority() == 2) {
                mediumPriorityQueue.add(pcb);
            } else if (pcb.getPriority() == 3) {
                highPriorityQueue.add(pcb);
            }
        }else{      //单队列调度
            runningQueue.add(pcb);
        }


    }
    // 中转操作-取出pcb
    public void executeNextProcess(int coreId) {
        ExecutorService[] cpuSimulatorExecutors = x86CPUSimulator.getExecutors();
        PCB matchedPcb = null;
        Iterator<PCB> it;
        PCB firstCorePcb = null;
        if(strategy.equals("SJF")||strategy.equals("SRJF")) {
            it = readySJFQueue.iterator();
        }else {
            // 遍历 readyQueue 找到第一个 pcb.coreId == coreId 的进程
            it = readyQueue.iterator();
        }
        while (it.hasNext()) {
            PCB pcb = it.next();
            // 注意空指针判断
            if (pcb.getCoreId() != null && pcb.getCoreId() == coreId) {
                matchedPcb = pcb;
                break;
            }else if(pcb.getCoreId() == -1 && firstCorePcb == null){
                firstCorePcb  = pcb;
            }
        }

        // 如果找到了，就执行
        if (matchedPcb != null) {
            Ready2Running(matchedPcb);
            cpuSimulatorExecutors[coreId].submit(
                    new ProcessExecutionTask(matchedPcb, protectedMemory, isrHandler, this)
            );
        } else {
            System.out.println("队列中没有匹配 coreId=" + coreId + " 的进程。");
            //如果有-1的，也就是刚进来的进程，也执行
            if(firstCorePcb != null) {
                System.out.println("执行刚进来的进程");
                cpuSimulatorExecutors[coreId].submit(
                        new ProcessExecutionTask(firstCorePcb , protectedMemory, isrHandler, this)
                );
            }

        }
    }


    public void Ready2Running(PCB pcb){
        System.out.println("进程" + pcb.getPid() + "进入运行队列");
        pcb.setState(RUNNING);
       if(strategy.equals("MLFQ")) {
           int priority = pcb.getPriority();
           if (priority == 1) {
               lowPriorityQueue.remove(pcb);
           } else if (priority == 2) {
               mediumPriorityQueue.remove(pcb);
           } else if (priority == 3) {
               highPriorityQueue.remove(pcb);
           }
           runningQueue.add(pcb);
       }else if(strategy.equals("SRJF")||strategy.equals("SJF")){
           readySJFQueue.remove(pcb);
           runningQueue.add(pcb);
       }else{  //fcfs,rr
              readyQueue.remove(pcb);
              runningQueue.add(pcb);
       }
       x86CPUSimulator.getExecutorServiceReady().get(pcb.getCoreId()).decrementAndGet();
    }
    public void Runing2Wait(PCB pcb){
        pcb.setState(WAITING);
        if(strategy.equals("MLFQ")) {
            int priority = pcb.getPriority();
            if (priority == 1) {
                lowPriorityQueue.remove(pcb);
            } else if (priority == 2) {
                mediumPriorityQueue.remove(pcb);
            } else if (priority == 3) {
                highPriorityQueue.remove(pcb);
            }
            waitingQueue.add(pcb);
        }else{
            runningQueue.remove(pcb);
            waitingQueue.add(pcb);
        }
    }
    public void Runing2Ready(PCB pcb){
        pcb.setState(READY);
        if(strategy.equals("MLFQ")) {
            int priority = pcb.getPriority();
            if (priority == 1) {
                lowPriorityQueue.remove(pcb);
            } else if (priority == 2) {
                mediumPriorityQueue.remove(pcb);
            } else if (priority == 3) {
                highPriorityQueue.remove(pcb);
            }
            readyQueue.add(pcb);
        }else if(strategy.equals("SRJF")){  //SRJF
            runningQueue.remove(pcb);
            readySJFQueue.add(pcb);
        }else{               //rr
            runningQueue.remove(pcb);
            readyQueue.add(pcb);
        }
        x86CPUSimulator.getExecutorServiceReady().get(pcb.getCoreId()).incrementAndGet();
    }
    public void Waiting2Ready(PCB pcb){

        pcb.setState(READY);
        pcb.setIr(pcb.getIr()+1);
        if(strategy.equals("SRJF") || strategy.equals("SJF")) {  //SRJF
            waitingQueue.remove(pcb);
            readySJFQueue.add(pcb);
        }else{
            waitingQueue.remove(pcb);
            readyQueue.add(pcb);
        }
        x86CPUSimulator.getExecutorServiceReady().get(pcb.getCoreId()).incrementAndGet();
    }
    public void Finnished(PCB pcb){
        runningQueue.remove(pcb);
        log.info(pcb.getProcessName() + "：" + "执行完成，***进程结束***");
    }


    @Scheduled(fixedRate = 13000) // 每隔 13 秒执行一次
    public void boostPriority() {
        // 将低优先级队列中的进程提升到中优先级
        List<PCB> toPromote = new ArrayList<>();
        for (PCB pcb : lowPriorityQueue) {
            pcb.setPriority(2); // 提升优先级
            toPromote.add(pcb);
        }
        lowPriorityQueue.removeAll(toPromote);
        mediumPriorityQueue.addAll(toPromote);

        // 将中优先级队列中的进程提升到高优先级
        toPromote.clear();
        for (PCB pcb : mediumPriorityQueue) {
            pcb.setPriority(3);
            toPromote.add(pcb);
        }
        mediumPriorityQueue.removeAll(toPromote);
        highPriorityQueue.addAll(toPromote);
    }
    @Scheduled(fixedRate = 100) // 每隔 0.1 秒执行一次
    public void checkIOInterrupt(){ //检测IO是否完成
        //查询
        System.out.println("检测IO中断");
        if(irlIO.peek() != null){       //说明IO触发
            //处理IO中断
            //队列中    存储pid
            int pid = irlIO.poll();
            PCB pcb = protectedMemory.getPcbTable().get(pid);
            //修改pcb状态
            Waiting2Ready(pcb);
        }
    }
    @Scheduled(fixedRate = 1000) // 每隔 1 秒执行一次
    //负载均衡
    public void loadBalance(){
        //进行判断

    }


}
