package newOs.controller;



import newOs.dto.result.Result;

import newOs.service.ServiaceImpl.ProcessManageServiceImpl;
import newOs.dto.req.ProcessManage.ProcessCreateReqDTO;
import newOs.dto.req.Info.InfoImplDTO.ProcessInfoReturnImplDTO;
import newOs.exception.ProcessException.ProcessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/one-os/cmd/process")
public class CmdCallController {


    private final ProcessManageServiceImpl processService; // 进程相关服务
//    @Autowired
//    private FileManageService fileService;       // 文件相关服务

    @Autowired
    public CmdCallController(ProcessManageServiceImpl processService) {
        this.processService = processService;
    }


    @PostMapping("/run") // 处理进程指令
    public ResponseEntity<Result> handleProcessCommand(@RequestBody ProcessCreateReqDTO cmd) {
        try{
            ProcessInfoReturnImplDTO process = processService.createProcess(cmd);
            Result result = Result.ok();
            result.setData(process).setRequestId(UUID.randomUUID().toString());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }catch (ProcessException e){
            return ResponseEntity.status(Integer.parseInt(e.getErrorCode())).body(Result.fail(e.getMessage(), e.getErrorCode()));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail("internal error", "400"));
        }
    }





    //    @PostMapping("/file")
//    public ResponseEntity<String> handleFileCommand(@RequestBody FileCommand cmd) {
//        return fileService.readFile(cmd.getFileName());
//    }
}