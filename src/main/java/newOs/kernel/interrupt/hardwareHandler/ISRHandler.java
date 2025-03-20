package newOs.kernel.interrupt.hardwareHandler;

import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.process.scheduler.SideScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class ISRHandler {
    private  final ProtectedMemory protectedMemory;
   // private static final MemoryManagementImpl mmu = new MemoryManagementImpl();
    private final ConcurrentHashMap<Long, InterruptRequestLine> irlTable ;


    private final HashMap<Integer, PCB> pcbTable;

    private final ConcurrentLinkedQueue<PCB> irlIO;

    @Autowired
    public ISRHandler(ProtectedMemory protectedMemory) {
        this.protectedMemory = protectedMemory;
        this.pcbTable = protectedMemory.getPcbTable();
        this.irlTable = protectedMemory.getIrlTable();
        this.irlIO = protectedMemory.getIrlIO();
    }


    //用于处理IRL线上 关于TIMRER的中断
    public int handlIsrInterrupt(PCB pcb){
        int isSwitchProcess = 0;
        InterruptRequestLine irl = irlTable.get(Thread.currentThread().getId());
        String interruptRequest;
        while((interruptRequest = irl.poll())!=null){
            if (interruptRequest.equals("TIMER_INTERRUPT")) {
                //如果中断等于定时器中断
                if (pcb.getRemainingTime() < 0) {   //时间片耗尽
                    isSwitchProcess = 1; //设置切换位为1
                }
            }
        }


        return  isSwitchProcess;
    }

}
