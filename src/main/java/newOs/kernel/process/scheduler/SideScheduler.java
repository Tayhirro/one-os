package newOs.kernel.process.scheduler;


import lombok.extern.slf4j.Slf4j;
import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.interrupt.InterruptController;
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
import java.util.concurrent.*;

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
    private final ConcurrentLinkedQueue<PCB> irlIO;

    private final ISRHandler isrHandler;

    private InterruptController interruptController;
    
    private ProcessExecutionTaskFactory processExecutionTaskFactory;

    /*
    * 1实现调度下一个进程 ----放入runningQueue
    * 2实现初始调度进程
    *
     */

    @Autowired
    public SideScheduler(ProtectedMemory protectedMemory, X86CPUSimulator x86CPUSimulator, ISRHandler isrHandler){
        this.readyQueue = protectedMemory.getReadyQueue();
        this.runningQueue = protectedMemory.getRunningQueue();
        this.waitingQueue = protectedMemory.getWaitingQueue();
        this.highPriorityQueue = protectedMemory.getHighPriorityQueue();
        this.mediumPriorityQueue = protectedMemory.getMediumPriorityQueue();
        this.lowPriorityQueue = protectedMemory.getLowPriorityQueue();
        this.readySJFQueue = protectedMemory.getReadySJFQueue();
        this.protectedMemory = protectedMemory;
        this.x86CPUSimulator = x86CPUSimulator;
        this.isrHandler = isrHandler;
        this.irlTable = protectedMemory.getIrlTable();
        this.irlIO = protectedMemory.getIrlIO();
    }
    
    @Autowired
    public void setInterruptController(InterruptController interruptController) {
        this.interruptController = interruptController;
    }
    
    @Autowired
    public void setProcessExecutionTaskFactory(ProcessExecutionTaskFactory processExecutionTaskFactory) {
        this.processExecutionTaskFactory = processExecutionTaskFactory;
    }

    public void schedulerProcess(PCB pcb){

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
                System.out.println("找到了匹配 coreId=" + coreId + " 的进程: " + pcb.getPid());
                break;
            }else if(pcb.getCoreId().equals(-1) && firstCorePcb == null){
                firstCorePcb  = pcb;
            }
        }

        // 如果找到了，就执行
        if (matchedPcb != null) {
            Ready2Running(matchedPcb);
            cpuSimulatorExecutors[coreId].submit(
                    new ProcessExecutionTask(matchedPcb, protectedMemory, isrHandler, this,interruptController)
            );
        } else {
            System.out.println("队列中没有匹配 coreId=" + coreId + " 的进程。");
            //如果有-1的，也就是刚进来的进程，也执行
            if(firstCorePcb != null) {
                //将这个pcb移除
                if(strategy.equals("SJF")||strategy.equals("SRJF")) {
                    readySJFQueue.remove(firstCorePcb);
                }else{
                    readyQueue.remove(firstCorePcb);
                }
                System.out.println("执行刚进来的进程");
                firstCorePcb.setCoreId(coreId);
                System.out.println("进程" + firstCorePcb.getCoreId()+"-"+firstCorePcb.getPid() + "进入运行队列");
                //
                x86CPUSimulator.getExecutorServiceReady().get(0).decrementAndGet();
                //
                cpuSimulatorExecutors[coreId].submit(
                        new ProcessExecutionTask(firstCorePcb , protectedMemory, isrHandler, this,interruptController)
                );
            }

        }
    }


    public void Ready2Running(PCB pcb){
        System.out.println("进程" + pcb.getCoreId()+"-"+pcb.getPid() + "进入运行队列");
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
            System.out.println("进程" + pcb.getCoreId() +"-"+pcb.getPid() + "进入就绪队列");
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
        if(irlIO.peek() != null){       //说明IO触发
            //处理IO中断
            //队列中    存储pid
            PCB pcb = irlIO.poll();
            //修改pcb状态
            Waiting2Ready(pcb);
            System.out.println("IO中断处理完成");
            System.out.println("进程" + pcb.getCoreId() +"-"+pcb.getPid() + "完成io等待后进入就绪队列");
        }
    }
    @Scheduled(fixedRate = 10) // 每隔 0.01 秒执行一次
    public void checkReadyQueue(){
        //检测就绪队列是否有进程，同时对应的核心是否空闲
        for(int i = 1; i < x86CPUSimulator.getExecutors().length; i++){
            ThreadPoolExecutor executor = (ThreadPoolExecutor) x86CPUSimulator.getExecutors()[i];
            int idleThreads = executor.getCorePoolSize() - executor.getActiveCount();
            if(idleThreads > 0) {
                //有空闲进程
                PCB pcb = null;
                if(strategy.equals("SJF")||strategy.equals("SRJF")) {
                    for(PCB pcb1 : readySJFQueue){
                        if(pcb1.getCoreId() == i || pcb1.getCoreId() == -1){
                            pcb1.setCoreId(i);
                            pcb = pcb1;
                            break;
                        }
                    }
                }else{
                    for(PCB pcb1 : readyQueue){
                        if(pcb1.getCoreId() == i || pcb1.getCoreId() == -1){
                            pcb1.setCoreId(i);
                            pcb = pcb1;
                            break;
                        }
                    }
                }
                if(pcb != null){
                    executeNextProcess(i);
                }
            }
        }

    }



    @Scheduled(fixedRate = 1000) // 每隔 1 秒执行一次
    public void loadBalance() {
        // executorServiceReady: 其中第0个一般可能是"新进/未分配"的数量，
        // 后续1~n个表示各个核心的负载统计(ready中的PCB数量、或者其他衡量标准)
        // 先把这几个 AtomicInteger 的值取出来
        int numberOfCores = x86CPUSimulator.getExecutors().length;  // 假设=5
        List<Integer> loads = new ArrayList<>(numberOfCores);
        for (int coreId = 0; coreId < numberOfCores; coreId++) {
            loads.add(x86CPUSimulator.getExecutorServiceReady().get(coreId).get());
        }
        System.out.println("各核心负载：" + loads);
        // 找到最大负载和最小负载的核心
        int maxLoad = -1;
        int maxCoreId = -1;
        int minLoad = Integer.MAX_VALUE;
        int minCoreId = -1;
        for (int i = 1; i < loads.size(); i++) {
            if (loads.get(i) > maxLoad) {
                maxLoad = loads.get(i);
                maxCoreId = i;
            }
            if (loads.get(i) < minLoad) {
                minLoad = loads.get(i);
                minCoreId = i;
            }
        }

         //设定一个差值阈值，比如：差值 > 2 就做负载均衡
        int THRESHOLD = 2;
        if ((maxLoad - minLoad) > THRESHOLD) {
            // 计算应该搬多少个进程过来，可以是一半/三分之一等策略
            int moveCount = (maxLoad - minLoad) / 2;
            if (moveCount <= 0) {
                return;  // 不需要搬动
            }

            log.info("开始执行负载均衡: 将从Core[{}]转移 {} 个任务到Core[{}]", maxCoreId, moveCount, minCoreId);

            // 需要遍历 readyQueue，或者你的 SJFQueue/MLFQ 中相应的队列
            // 以找到核心是 maxCoreId 的 PCB，然后重新分配给 minCoreId。
            // 注意：这里仅演示从 readyQueue 里移动，若你还有 SRJF/MLFQ 等其他队列也类似。
            int moved = 0;
            if(strategy.equals("SJF")||strategy.equals("SRJF")) {
                //
            }else{
                for (PCB pcb : readyQueue) {
                    // 如果这个 pcb 的 coreId 正好是 maxCoreId，则把它分给 minCoreId
                    if (pcb.getCoreId() == maxCoreId) {
                        pcb.setCoreId(minCoreId);
                        // 更新计数
                        x86CPUSimulator.getExecutorServiceReady().get(maxCoreId).decrementAndGet();
                        x86CPUSimulator.getExecutorServiceReady().get(minCoreId).incrementAndGet();

                        moved++;
                        if (moved >= moveCount) {
                            break;
                        }
                    }
                }
            }
        }
    }



}
