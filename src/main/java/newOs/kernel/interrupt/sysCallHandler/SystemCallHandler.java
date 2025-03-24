package newOs.kernel.interrupt.sysCallHandler;

import newOs.common.InterruptConstant.SystemCallType;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoImplDTO;
import newOs.dto.req.Info.InfoImplDTO.ProcessInfoImplDTO;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.InterruptSysCallInfo;
import newOs.exception.OSException;
import newOs.kernel.device.DeviceManager;
import newOs.kernel.interrupt.ISR;
import newOs.kernel.process.ProcessManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static newOs.tools.ProcessTool.getPid;

/**
 * 系统调用处理程序
 */
@Component
public class SystemCallHandler implements ISR<InterruptSysCallInfo> {

    private final ProcessManager processManager;
    private final DeviceManager deviceManager;
    private final ProtectedMemory protectedMemory;

    @Autowired
    public SystemCallHandler(ProcessManager processManager, DeviceManager deviceManager, ProtectedMemory protectedMemory) {
        this.processManager = processManager;
        this.deviceManager = deviceManager;
        this.protectedMemory = protectedMemory;
    }

    @Override
    public InterruptInfo execute(InterruptSysCallInfo interruptInfo) throws OSException {
        try {
            // 根据信息类型分发处理
            if (interruptInfo instanceof ProcessInfoImplDTO) {
                return (InterruptInfo) handleProcessSystemCall((ProcessInfoImplDTO) interruptInfo);
            } else if (interruptInfo instanceof DeviceInfoImplDTO) {
                return (InterruptInfo) handleDeviceSystemCall((DeviceInfoImplDTO) interruptInfo);
            } else {
                throw new OSException("未知的系统调用信息类型", "403");
            }
        } catch (Exception e) {
            throw new OSException("系统调用处理失败: " + e.getMessage(), "500");
        }
    }

    /**
     * 处理进程相关系统调用
     * @param processInfo 进程信息
     * @return 处理结果
     * @throws OSException 系统异常
     */
    public Object handleProcessSystemCall(ProcessInfoImplDTO processInfo) throws OSException {
        SystemCallType syscallType = processInfo.getSystemCallType();
        
        switch (syscallType) {
            case CREATE_PROCESS:
                // 调用 ProcessManager 创建进程，并返回相应的 ProcessInfoImpl
                return processManager.createProcess(processInfo.getName(), processInfo.getArgs(), processInfo.getInstructions());
                
            case EXECUTE_PROCESS:
                PCB pcb = protectedMemory.getPcbTable().get(getPid(processInfo.getName()));
                processManager.executeProcess(pcb);
                return null;
                
            case TERMINATE_PROCESS:
                return processManager.terminateProcess(getPid(processInfo.getName()));
                
            case ALLOCATE_MEMORY:
                // 内存分配逻辑
                // 这里需要根据实际参数结构进行调用
                return null;
                
            default:
                throw new OSException("未知的进程系统调用: " + syscallType, "403");
        }
    }
    
    /**
     * 处理设备相关系统调用
     * @param deviceInfo 设备信息
     * @return 处理结果
     * @throws OSException 系统异常
     */
    public Object handleDeviceSystemCall(DeviceInfoImplDTO deviceInfo) throws OSException {
        SystemCallType syscallType = deviceInfo.getSystemCallType();
        
        switch (syscallType) {
            case OPEN_FILE:
                return deviceManager.openDevice(deviceInfo.getDeviceName(), deviceInfo.getPcb());
                
            case CLOSE_FILE:
                return deviceManager.closeDevice(deviceInfo.getDeviceName(), deviceInfo.getPcb());
                
            case READ_FILE:
                return deviceManager.readDevice(deviceInfo.getDeviceName(), deviceInfo.getPcb());
                
            case WRITE_FILE:
                return deviceManager.writeDevice(deviceInfo.getDeviceName(), deviceInfo.getPcb(), deviceInfo.getDeviceInfo());
                
            default:
                throw new OSException("未知的设备系统调用: " + syscallType, "403");
        }
    }
}

