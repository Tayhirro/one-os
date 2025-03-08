package newOs.kernel.process;

import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HandleISR {
    private  final ProtectedMemory protectedMemory;
   // private static final MemoryManagementImpl mmu = new MemoryManagementImpl();
    private final ConcurrentHashMap<Long, InterruptRequestLine> irlTable ;


    private final HashMap<Integer, PCB> pcbTable;
    private final Queue<PCB> runningQueue;
    private final Queue<PCB> readyQueue;
    private final Queue<PCB> waitingQueue;

    @Autowired
    public HandleISR(ProtectedMemory protectedMemory){
        this.protectedMemory = protectedMemory;
        this.pcbTable = protectedMemory.getPcbTable();
        this.readyQueue =protectedMemory.getReadyQueue();
        this.runningQueue = protectedMemory.getRunningQueue();
        this.waitingQueue = protectedMemory.getWaitingQueue();
        this.irlTable = protectedMemory.getIrlTable();
    }


    //用于处理ISR线上 关于IO/Timer的中断
    public int handlIsrInterrupt(PCB pcb){
        int isSwitchProcess = 0;
        InterruptRequestLine irl = irlTable.get(Thread.currentThread().getId());
        String interruptRequest;
        while((interruptRequest = irl.poll())!=null){
            if (interruptRequest.equals("TIMER_INTERRUPT")) {
                //如果中断等于定时器中断
            }
        }
        return  isSwitchProcess;
    }
    public int handlIsrInterruptIO(){
        return 1;
    }
}
