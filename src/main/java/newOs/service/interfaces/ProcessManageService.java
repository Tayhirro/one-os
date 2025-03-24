package newOs.service.interfaces;





import newOs.dto.req.ProcessManage.ProcessCreateReqDTO;
import newOs.dto.req.Info.InfoImplDTO.ProcessInfoReturnImplDTO;
import newOs.dto.resp.ProcessManage.ProcessQueryAllRespDTO;


/*
*  进程管理服务接口
* 1. 创建进程
* 2. 执行进程
* 3. 查询所有进程信息
* 4. 切换调度策略
 */

public interface ProcessManageService {
    ProcessInfoReturnImplDTO createProcess(ProcessCreateReqDTO processCreateReqDTO);

    void executeProcess(String processName);

    ProcessQueryAllRespDTO queryAllProcessInfo();

    void switchStrategy(String strategy);
} 