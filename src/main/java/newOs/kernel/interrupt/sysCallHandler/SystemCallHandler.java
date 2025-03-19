package newOs.kernel.interrupt.sysCallHandler;


import newOs.common.InterruptConstant.SystemCallType;
import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoImplDTO;
import newOs.dto.req.Info.InfoImplDTO.ProcessInfoImplDTO;
import newOs.dto.req.Info.InterruptSysCallInfo;
import newOs.exception.OSException;
import newOs.kernel.device.DeviceManager;
import newOs.kernel.interrupt.ISR;
import newOs.kernel.process.ProcessManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static newOs.tools.ProcessTool.getPid;

@Component
public class SystemCallHandler implements ISR<InterruptSysCallInfo> {


    private final ProcessManager processManager;
    private final DeviceManager deviceManager;

    @Autowired
    public SystemCallHandler(ProcessManager processManager, DeviceManager deviceManager) {
        this.processManager = processManager;
        this.deviceManager = deviceManager;
    }


    @Override
    public InterruptSysCallInfo execute(InterruptSysCallInfo interruptSysCallInfo) throws OSException {
        //系统调用 进程管理
        if (interruptSysCallInfo instanceof ProcessInfoImplDTO) {
            ProcessInfoImplDTO processInfo =  (ProcessInfoImplDTO) interruptSysCallInfo;
            SystemCallType syscallType = processInfo.getSystemCallType();
            switch (syscallType) {
                case CREATE_PROCESS:
                    // 调用 ProcessManager 创建进程，并返回相应的 ProcessInfoImpl
                    return processManager.createProcess(processInfo.getName(),processInfo.getArgs(),processInfo.getInstructions());
                case EXECUTE_PROCESS:
                    //return processManager.executeProcess(args.getInt("pid"));
                    PCB pcb = processManager.getPcbTable().get(getPid(processInfo.getName()));
                    processManager.executeProcess(pcb);
                // 根据需要添加更多系统调用
                    return null;
                default:
                    System.out.println("未知的系统调用: " + syscallType);
                    throw new OSException("未知的系统调用: " , "403");
            }
        }else if(interruptSysCallInfo instanceof DeviceInfoImplDTO){
            //设备管理
            DeviceInfoImplDTO deviceInfo = (DeviceInfoImplDTO) interruptSysCallInfo;
            SystemCallType syscallType = deviceInfo.getSystemCallType();
            switch (syscallType) {
                case OPEN_FILE:
                    // 调用 DeviceManager 打开设备，并返回相应的 DeviceInfoImpl
                    return deviceManager.openDevice(deviceInfo.getDeviceName(),deviceInfo.getPcb());
                case CLOSE_FILE:
                    return deviceManager.closeDevice(deviceInfo.getDeviceName(),deviceInfo.getPcb());
                case READ_FILE:

                    return deviceManager.readDevice(deviceInfo.getDeviceName(),deviceInfo.getPcb());
                case WRITE_FILE:
                    return deviceManager.writeDevice(deviceInfo.getDeviceName(),deviceInfo.getPcb(),deviceInfo.getDeviceInfo());
                // 根据需要添加更多系统调用
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

