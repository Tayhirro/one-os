package newOs.kernel.syscall;


import newOs.dto.req.Info.InfoImplDTO.ProcessInfoImplDTO;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.InterruptSysCallInfo;
import newOs.exception.OSException;
import newOs.kernel.interrupt.InterruptController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SystemCallDispatcher{

    private final InterruptController interruptController;

    @Autowired
    public SystemCallDispatcher(InterruptController interruptController) {
        this.interruptController = interruptController;
    }

    public InterruptSysCallInfo Dispatch(InterruptInfo interruptInfo) throws OSException {
            // 触发系统调用 0x80，并传递调用号
        if(interruptInfo instanceof ProcessInfoImplDTO) {
            ProcessInfoImplDTO processInfo = (ProcessInfoImplDTO) interruptInfo;
            InterruptSysCallInfo interruptSysCallInfo = interruptController.triggerSystemCall(processInfo);
            return interruptSysCallInfo;
        }else{
        //否则只能是InterruptInfo
            return null;
        }
    }
}