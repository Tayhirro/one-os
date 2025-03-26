package newOs.kernel.interrupt;

import newOs.common.InterruptConstant.InterruptType;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.dto.req.Info.InfoImplDTO.ProcessInfoImplDTO;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.InterruptSysCallInfo;
import newOs.dto.req.Info.TimerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

import static newOs.common.InterruptConstant.InterruptType.SYSTEM_CALL;
import static newOs.common.InterruptConstant.InterruptType.TIMER;


// InterruptController.java
@Component
public class InterruptController {


    private final ConcurrentHashMap<InterruptType, ISR> IDT; // 中断描述符表


    @Autowired
    public InterruptController(ProtectedMemory protectedMemory) {
        IDT = protectedMemory.getIDT();
    }



    public InterruptSysCallInfo triggerSystemCall(InterruptSysCallInfo sysCallInfo) {    //对系统中断的处理
        //默认是0x80，直接调用SytemCallHandler
        InterruptInfo interruptInfo = IDT.get(SYSTEM_CALL).execute(sysCallInfo);
        return (InterruptSysCallInfo) interruptInfo;
    }
    public void triggerTimer(TimerInfo timerInfo) {
        //默认是0x20，直接调用TimerHandler
        IDT.get(TIMER).execute(timerInfo);

    }
    //返回信息trigger
    public void trigger(DeviceInfoReturnImplDTO deviceInfoReturnImplDTO) {
        IDT.get(InterruptType.IO_INTERRUPT).execute(deviceInfoReturnImplDTO);
    }

    public InterruptMissTLBReturnInfo trigger ...
    !{
    !    ...
    !}

    !public InterruptPageFaultReturnInfo triggerPageFault(InterruptPageFaultInfo pageFaultInfo){
    !       InterruptPageFaultReturnInfo interuptPageFaultReturnInfo = IDT.get(InterruptType.PAGE_FAULT).execute(pageFaultInfo);
    !       return interruptPageFaultReturnInfo;
    !}
}
