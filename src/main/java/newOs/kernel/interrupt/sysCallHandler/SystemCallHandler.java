package newOs.kernel.interrupt.sysCallHandler;


import com.alibaba.fastjson.JSONObject;
import newOs.common.InterruptConstant.SystemCallType;
import newOs.dto.req.Info.InfoImpl.ProcessInfoImpl;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.InterruptSysCallInfo;
import newOs.exception.OSException;
import newOs.kernel.interrupt.ISR;
import newOs.kernel.process.ProcessManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SystemCallHandler implements ISR<InterruptSysCallInfo> {


    private final ProcessManager processManager;

    @Autowired
    public SystemCallHandler(ProcessManager processManager) {
        this.processManager = processManager;
    }


    @Override
    public InterruptSysCallInfo execute(InterruptSysCallInfo interruptSysCallInfo) throws OSException {
        //系统调用 进程管理
        if (interruptSysCallInfo instanceof ProcessInfoImpl) {
            ProcessInfoImpl processInfo =  (ProcessInfoImpl) interruptSysCallInfo;
            SystemCallType syscallType = processInfo.getSystemCallType();
            switch (syscallType) {
                case CREATE_PROCESS:
                    // 调用 ProcessManager 创建进程，并返回相应的 ProcessInfoImpl
                    return (InterruptSysCallInfo) processManager.createProcess(processInfo.getName(),processInfo.getArgs(),processInfo.getInstructions());
                case EXECUTE_PROCESS:
                    //return processManager.executeProcess(args.getInt("pid"));
                // 根据需要添加更多系统调用
                default:
                    throw new OSException("未知的系统调用: " , "403");
            }
        }else{
            //暂时
            return null;
        }
    }
}

