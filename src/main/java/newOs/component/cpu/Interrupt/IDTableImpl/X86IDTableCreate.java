package newOs.component.cpu.Interrupt.IDTableImpl;

import lombok.RequiredArgsConstructor;
import newOs.common.InterruptConstant.InterruptType;
import newOs.component.cpu.Interrupt.IDTableCreate;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.component.timer.timer;
import newOs.kernel.interrupt.ISR;
import newOs.kernel.interrupt.hardwareHandler.IOInterruptHandler;
import newOs.kernel.interrupt.memoryHandler.PageFaultHandler;
import newOs.kernel.interrupt.sysCallHandler.SystemCallHandler;
import newOs.kernel.interrupt.timerHandler.TimerHandler;
import newOs.kernel.process.ProcessManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


@Component
public class X86IDTableCreate implements IDTableCreate{
    private final ConcurrentHashMap<InterruptType, ISR> IDT;

    //依赖注入 处理方法
    private final SystemCallHandler systemCallHandler;
    private final IOInterruptHandler iOInterruptHandler;
    private final TimerHandler timerHandler;
    private final PageFaultHandler pageFaultHandler;

    !private final MissTLBHandler missTLBHandler;

    @Autowired
    public X86IDTableCreate(ProtectedMemory protectedMemory, SystemCallHandler systemCallHandler,
                            IOInterruptHandler IOInterruptHandler,
                            TimerHandler timerHandler,
                           ! PageFaultHandler PageFaultHandler,
                           ! MissTLBHandler missTLBHandler) {
        this.IDT = protectedMemory.getIDT();
        this.systemCallHandler = systemCallHandler;
        this.iOInterruptHandler = IOInterruptHandler;
        this.timerHandler = timerHandler;
        !this.pageFaultHandler = PageFaultHandler;
        !this.missTLBHandler = missTLBHandler;
    }

    @Override
    public void createIDTable() {
        System.out.println("X86IDTableCreate createIDTable");
        //添加中断向量表
        IDT.put(InterruptType.SYSTEM_CALL, systemCallHandler);
        IDT.put(InterruptType.IO_INTERRUPT, iOInterruptHandler);
        IDT.put(InterruptType.TIMER,timerHandler);
      !  IDT.put(InterruptType.PAGE_FAULT,pageFaultHandler);
      !  IDT.put(InterruptType.MISS_TLB,missTLBHandler);
    }
}
