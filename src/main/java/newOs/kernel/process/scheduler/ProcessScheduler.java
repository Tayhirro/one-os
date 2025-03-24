package newOs.kernel.process.scheduler;

import lombok.extern.slf4j.Slf4j;
import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.interrupt.hardwareHandler.ISRHandler;
import newOs.kernel.process.ProcessExecutionTaskFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
@Component
public class ProcessScheduler {
    private final Queue<PCB> readyQueue;
    private final ConcurrentHashMap<Long, InterruptRequestLine> irlTable;
    private ExecutorService[] cpuSimulatorExecutors;
    private final ProtectedMemory protectedMemory;
    private ProcessExecutionTaskFactory processExecutionTaskFactory;
    private final ISRHandler ISRHandler;

    private static final Semaphore cpuSemaphore = new Semaphore(4);

    //检测是否有新进程进入就绪队列，否则立刻调度
    private final Lock lock = new ReentrantLock();
    private final Condition newProcessArrived = lock.newCondition();


    public static String strategy = "FCFS"; //不能实现多级反馈队列调度



    //依赖注入
    @Autowired
    public ProcessScheduler(ProtectedMemory protectedMemory, X86CPUSimulator x86CPUSimulator, ISRHandler ISRHandler) {
        this.protectedMemory = protectedMemory;
        this.readyQueue = protectedMemory.getReadyQueue();
        this.irlTable = protectedMemory.getIrlTable();
        this.cpuSimulatorExecutors = x86CPUSimulator.getExecutors();
        this.ISRHandler = ISRHandler;
    }
    
    @Autowired
    public void setProcessExecutionTaskFactory(ProcessExecutionTaskFactory processExecutionTaskFactory) {
        this.processExecutionTaskFactory = processExecutionTaskFactory;
    }
//    public void spanWait(ExecutorService cpuSimulatorExecutor){
//        cpuSimulatorExecutor.submit(() -> {
//            irlTable.put(Thread.currentThread().getId(), new InterruptRequestLine("TIMER_INTERRUPT"));
//            try {
//                while(true) {
//                    Thread.sleep(10);
//                    log.info("running spanWait: " + Thread.currentThread().getId());
//                    long threadId = Thread.currentThread().getId();
//                    InterruptRequestLine irl = irlTable.get(threadId);
//                    if (irl.peek() != null) {       //如果irl有内容，则说明有IO中断
//                        ISRHandler.handlIsrInterruptIO();
//                    }
//                    try {       //一旦有进程就绪，就执行
//                        PCB pcb = readyQueue.poll();
//                        if (pcb != null) {
//                            cpuSimulatorExecutor.submit(processExecutionTaskFactory.createTask(pcb));
//                            //退出spanWait状态
//                            break;
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
//    }

    /**
     * 从调度器中移除进程
     * @param pcb 要移除的进程控制块
     * @return 是否成功移除
     */
    public boolean removeProcess(PCB pcb) {
        if (pcb == null) {
            return false;
        }
        
        boolean removed = false;
        
        // 从就绪队列移除
        if (protectedMemory.getReadyQueue().remove(pcb)) {
            removed = true;
        }
        
        // 从运行队列移除
        if (protectedMemory.getRunningQueue().remove(pcb)) {
            removed = true;
        }
        
        // 从等待队列移除
        if (protectedMemory.getWaitingQueue().remove(pcb)) {
            removed = true;
        }
        
        // 从SJF就绪队列移除
        if (protectedMemory.getReadySJFQueue().remove(pcb)) {
            removed = true;
        }
        
        // 如果确实移除了进程，记录日志
        if (removed) {
            log.info("进程{}已从调度器移除", pcb.getPid());
        }
        
        return removed;
    }

}