package newOs.kernel.process.scheduler;

import lombok.extern.slf4j.Slf4j;
import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.process.HandleISR;
import newOs.kernel.process.ProcessExecutionTaskFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;


@Slf4j
@Component
public class ProcessScheduler {
    private final Queue<PCB> readyQueue;
    private final ConcurrentHashMap<Long, InterruptRequestLine> irlTable;
    private ExecutorService cpuSimulatorExecutor;
    private final ProtectedMemory protectedMemory;
    private final ProcessExecutionTaskFactory processExecutionTaskFactory;
    private final HandleISR handleISR;

    public static String strategy = "FCFS"; //不能实现多级反馈队列调度

    //依赖注入
    @Autowired
    public ProcessScheduler(ProtectedMemory protectedMemory, X86CPUSimulator x86CPUSimulator, ProcessExecutionTaskFactory processExecutionTaskFactory,HandleISR handleISR) {
        this.protectedMemory = protectedMemory;
        this.readyQueue = protectedMemory.getReadyQueue();
        this.irlTable = protectedMemory.getIrlTable();
        this.cpuSimulatorExecutor = x86CPUSimulator.getExecutor();
        this.processExecutionTaskFactory = processExecutionTaskFactory;
        this.handleISR = handleISR;
    }

    public void spanWait(ExecutorService cpuSimulatorExecutor){
        cpuSimulatorExecutor.submit(() -> {
            irlTable.put(Thread.currentThread().getId(), new InterruptRequestLine());
            try {
                while(true) {
                    Thread.sleep(10);
                    log.info("running spanWait: " + Thread.currentThread().getId());
                    long threadId = Thread.currentThread().getId();
                    InterruptRequestLine irl = irlTable.get(threadId);
                    if (irl.peek() != null) {       //如果irl有内容，则说明有IO中断
                        handleISR.handlIsrInterruptIO();
                    }
                    try {       //一旦有进程就绪，就执行
                        PCB pcb = readyQueue.poll();
                        if (pcb != null) {
                            cpuSimulatorExecutor.submit(processExecutionTaskFactory.createTask(pcb));
                            //退出spanWait状态
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

}