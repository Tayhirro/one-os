package newOs.kernel.process;


import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.interrupt.InterruptController;
import newOs.kernel.interrupt.hardwareHandler.ISRHandler;
import newOs.kernel.process.scheduler.SideScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessExecutionTaskFactory {
    private final newOs.kernel.interrupt.hardwareHandler.ISRHandler ISRHandler;
    private final ProtectedMemory protectedMemory;
    private final SideScheduler sideScheduler;
    private final InterruptController interruptController;

    @Autowired
    public ProcessExecutionTaskFactory(ISRHandler handlerISR, ProtectedMemory protectedMemory, SideScheduler sideScheduler, InterruptController interruptController){
        this.ISRHandler = handlerISR;
        this.protectedMemory = protectedMemory;
        this.sideScheduler = sideScheduler;
        this.interruptController = interruptController;
    }

    public ProcessExecutionTask createTask(PCB pcb) {
        return new ProcessExecutionTask(pcb, protectedMemory, ISRHandler, sideScheduler,interruptController);
    }
}
