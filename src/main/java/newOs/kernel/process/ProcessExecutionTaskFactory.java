package newOs.kernel.process;


import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.process.scheduler.SideScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessExecutionTaskFactory {
    private final HandleISR handleISR;
    private final ProtectedMemory protectedMemory;
    private final SideScheduler sideScheduler;

    @Autowired
    public ProcessExecutionTaskFactory(HandleISR handlerISR, ProtectedMemory protectedMemory, SideScheduler sideScheduler) {
        this.handleISR = handlerISR;
        this.protectedMemory = protectedMemory;
        this.sideScheduler = sideScheduler;
    }

    public ProcessExecutionTask createTask(PCB pcb) {
        return new ProcessExecutionTask(pcb, protectedMemory, handleISR, sideScheduler);
    }
}
