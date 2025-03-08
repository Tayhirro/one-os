package newOs.kernel.process.scheduler;


import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import static newOs.kernel.process.scheduler.ProcessScheduler.strategy;

@Component
public class SideScheduler {
    private final ConcurrentLinkedQueue<PCB> readyQueue;
    private final ConcurrentLinkedQueue<PCB> runningQueue;
    private final ConcurrentLinkedQueue<PCB> waitingQueue;

    //mlfq的边缘调度
    private final ConcurrentLinkedQueue<PCB> highPriorityQueue;
    private final ConcurrentLinkedQueue<PCB> mediumPriorityQueue;
    private final ConcurrentLinkedQueue<PCB> lowPriorityQueue;


    private ExecutorService cpuSimulatorExecutor;
    private final ProtectedMemory protectedMemory;
    private final ConcurrentHashMap<Long, InterruptRequestLine> irlTable;


    /*
    * 1实现调度下一个进程 ----放入runningQueue
    * 2实现初始调度进程
    *
     */



    @Autowired
    public SideScheduler(ProtectedMemory protectedMemory, X86CPUSimulator x86CPUSimulator) {
        this.readyQueue = protectedMemory.getReadyQueue();
        this.runningQueue = protectedMemory.getRunningQueue();
        this.waitingQueue = protectedMemory.getWaitingQueue();
        this.highPriorityQueue = protectedMemory.getHighPriorityQueue();
        this.mediumPriorityQueue = protectedMemory.getMediumPriorityQueue();
        this.lowPriorityQueue = protectedMemory.getLowPriorityQueue();

        this.protectedMemory = protectedMemory;
        this.irlTable = protectedMemory.getIrlTable();
    }


    public void schedulerProcess(PCB pcb){
        readyQueue.removeIf(p->p.equals(pcb));
        //多级调度
        if(strategy == "MLFQ") {
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
    // 直接取出readyQueue中的第一个进程
    public void executeNextProcess(){

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

}
