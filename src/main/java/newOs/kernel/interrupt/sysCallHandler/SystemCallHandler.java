package newOs.kernel.interrupt.sysCallHandler;


import com.alibaba.fastjson.JSONObject;
import newOs.common.InterruptConstant.SystemCallType;
import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InfoImpl.ProcessInfoImpl;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.InterruptSysCallInfo;
import newOs.exception.OSException;
import newOs.kernel.interrupt.ISR;
import newOs.kernel.process.ProcessManager;
import newOs.tools.ProcessTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static newOs.tools.ProcessTool.getPid;

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
                    System.out.println("execute process1");
                    //return processManager.executeProcess(args.getInt("pid"));
                    PCB pcb = processManager.getPcbTable().get(getPid(processInfo.getName()));
                    processManager.executeProcess(pcb);
                // 根据需要添加更多系统调用
                    return null;
                default:
                    System.out.println("未知的系统调用: " + syscallType);
                    throw new OSException("未知的系统调用: " , "403");
            }
        }else{
            //暂时
            return null;
        }
    }
}

