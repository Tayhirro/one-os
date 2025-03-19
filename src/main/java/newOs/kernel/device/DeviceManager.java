package newOs.kernel.device;


import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoImplDTO;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Data
public class DeviceManager {
    private final ProtectedMemory protectedMemory;
    private final X86CPUSimulator x86CPUSimulator;





    @Autowired
    public DeviceManager(ProtectedMemory protectedMemory, X86CPUSimulator x86CPUSimulator){
        this.protectedMemory = protectedMemory;
        this.x86CPUSimulator = x86CPUSimulator;
    }


    public DeviceInfoReturnImplDTO openDevice(String deviceName,PCB pcb){
        // 打开设备
        // 1. 从PCB中获取进程的设备请求队列
        // 2. 将设备请求加入设备请求队列
        // 3. 将进程状态设置为等待



    }

    public DeviceInfoReturnImplDTO closeDevice(String deviceName,PCB pcb){
        // 关闭设备
        // 1. 从PCB中获取进程的设备请求队列
        // 2. 将设备请求加入设备请求队列
        // 3. 将进程状态设置为等待
    }
    public DeviceInfoReturnImplDTO readDevice(String deviceName,PCB pcb){
        // 读设备
        // 1. 从PCB中获取进程的设备请求队列
        // 2. 将设备请求加入设备请求队列
        // 3. 将进程状态设置为等待
    }
    public DeviceInfoReturnImplDTO writeDevice(String deviceName, PCB pcb, JSONObject args){
        // 写设备
        // 1. 从PCB中获取进程的设备请求队列
        // 2. 将设备请求加入设备请求队列
        // 3. 将进程状态设置为等待
    }

    // 检测所有的设备队列
    @Scheduled(fixedRate = 1000)
    public void checkAllDeviceQueue() {
            //定时检测所有设备的状态,如果设备准备好，则调度队列中的执行


    }

}
