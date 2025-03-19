package newOs.service.ServiaceImpl;


import newOs.dto.req.Info.InfoImplDTO.ProcessInfoImplDTO;
import newOs.dto.req.Info.InterruptSysCallInfo;
import newOs.dto.req.ProcessManage.ProcessCreateReqDTO;
import newOs.dto.req.Info.InfoImplDTO.ProcessInfoReturnImplDTO;
import newOs.dto.resp.ProcessManage.ProcessQueryAllRespDTO;
import newOs.exception.Dispatch_Dismatch_Exception;
import newOs.exception.OSException;
import newOs.kernel.syscall.SystemCallDispatcher;
import newOs.service.ServiceInterface.ProcessManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static newOs.common.InterruptConstant.InterruptType.SYSTEM_CALL;
import static newOs.common.InterruptConstant.SystemCallType.CREATE_PROCESS;
import static newOs.common.InterruptConstant.SystemCallType.EXECUTE_PROCESS;

@Service
public class ProcessManageServiceImpl implements ProcessManageService {
    private final SystemCallDispatcher systemCallDispatcher;



    @Autowired
    public ProcessManageServiceImpl(SystemCallDispatcher systemCallDispatcher) {
        this.systemCallDispatcher = systemCallDispatcher;
    }

    //检查使用切面进行检查
    //run process
    @Override
    public ProcessInfoReturnImplDTO createProcess(ProcessCreateReqDTO processCreateReqDTO) throws OSException{     //执行创建进程，转发给kernel的syscall
        String[] instructions = processCreateReqDTO.getInstructions();

            //写入文件系统
            /*
             *
             *
             *
             * */

        //转入访管中断,传递JSON
        ProcessInfoImplDTO processInfo = new ProcessInfoImplDTO().setSystemCallType(CREATE_PROCESS).setInterruptType(SYSTEM_CALL).setInstructions(instructions);
        InterruptSysCallInfo processInfoReturnImpl = systemCallDispatcher.Dispatch(processInfo);
        if(processInfoReturnImpl instanceof ProcessInfoReturnImplDTO){
            return (ProcessInfoReturnImplDTO) processInfoReturnImpl;
        }else{
            throw new Dispatch_Dismatch_Exception("mis_match,expected processImpl","401");
        }
            //设置processInfo内容
            /*
             *
             *
             *
             * */
    }



    @Override
    public void executeProcess(String processName) throws OSException{    //执行进程，转发给kernel的syscall
        try {
            //System.out.println("execute process");
            ProcessInfoImplDTO processInfo = new ProcessInfoImplDTO().setSystemCallType(EXECUTE_PROCESS).setInterruptType(SYSTEM_CALL).setName(processName);
            systemCallDispatcher.Dispatch(processInfo);
        }catch (OSException e){
            throw e;
        }
        //直接进行返回
    }


    @Override
    public ProcessQueryAllRespDTO queryAllProcessInfo() {    //查询所有进程信息，转发给kernel的syscall
        return new ProcessQueryAllRespDTO();
    }
    @Override
    public void switchStrategy(String strategy) {    //切换调度策略，转发给kernel的syscall
    }
}
