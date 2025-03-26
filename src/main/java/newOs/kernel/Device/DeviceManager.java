package newOs.kernel.device;


import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoImplDTO;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.kernel.device.DeviceImpl.DiskDriverImpl;
import newOs.kernel.interrupt.InterruptController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Data
@Slf4j
public class DeviceManager {
    private final ProtectedMemory protectedMemory;
    private final X86CPUSimulator x86CPUSimulator;
    private final InterruptController interruptController;





    @Autowired
    public DeviceManager(ProtectedMemory protectedMemory, X86CPUSimulator x86CPUSimulator, InterruptController interruptController) {
        this.protectedMemory = protectedMemory;
        this.x86CPUSimulator = x86CPUSimulator;
        this.interruptController = interruptController;
    }


    public DeviceInfoReturnImplDTO openDevice(String deviceName,PCB pcb){
        // 打开设备
        DeviceInfoReturnImplDTO deviceReturnInfo = new DeviceInfoReturnImplDTO();
        Optional<DeviceDriver> foundDevice = protectedMemory.getDeviceQueue().stream()
                .filter(device -> device.getDeviceName().equals(deviceName))
                .findFirst(); // 找到第一个匹配的对象
        if (foundDevice.isPresent()) {
            System.out.println("找到设备：" + foundDevice.get());
            deviceReturnInfo = foundDevice.get().add(pcb);
        } else {
            System.out.println("未找到该设备");
        }
        return  deviceReturnInfo;
    }

    public DeviceInfoReturnImplDTO closeDevice(String deviceName,PCB pcb){
        // 关闭设备
        // 1. 从PCB中获取进程的设备请求队列
        // 2. 将设备请求加入设备请求队列
        // 3. 将进程状态设置为等待
        DeviceInfoReturnImplDTO deviceReturnInfo = new DeviceInfoReturnImplDTO();
        Optional<DeviceDriver> foundDevice = protectedMemory.getDeviceQueue().stream()
                .filter(device -> device.getDeviceName().equals(deviceName))
                .findFirst(); // 找到第一个匹配的对象
        if (foundDevice.isPresent()) {
            System.out.println("找到设备：" + foundDevice.get());
            deviceReturnInfo = foundDevice.get().releaseDevice();
        } else {
            System.out.println("未找到该设备");
        }

        return deviceReturnInfo;
    }
    public DeviceInfoReturnImplDTO readDevice(String deviceName, PCB pcb) {
        Optional<DeviceDriver> foundDevice = protectedMemory.getDeviceQueue().stream()
                .filter(device -> device.getDeviceName().equals(deviceName))
                .findFirst(); // 找到第一个匹配的设备

        if (foundDevice.isPresent()) {
            log.info("找到设备：" + foundDevice.get().getDeviceName());
            DiskDriverImpl diskDriver = (DiskDriverImpl) foundDevice.get();
            DeviceInfoReturnImplDTO deviceInfoReturnImplDTO =  diskDriver.executeDeviceReadOperation(pcb);
            return deviceInfoReturnImplDTO;
        } else {
            System.out.println("未找到该设备");
            return null;
        }
    }

    public DeviceInfoReturnImplDTO writeDevice(String deviceName, PCB pcb, JSONObject args){
        // 写设备
        //args中的contents:对应写内容
        Optional<DeviceDriver> foundDevice = protectedMemory.getDeviceQueue().stream()
                .filter(device -> device.getDeviceName().equals(deviceName))
                .findFirst(); // 找到第一个匹配的设备

        if (foundDevice.isPresent()) {
            log.info("找到设备：" + foundDevice.get().getDeviceName());
            DiskDriverImpl diskDriver = (DiskDriverImpl) foundDevice.get();
            DeviceInfoReturnImplDTO deviceInfoReturnImplDTO = diskDriver.executeDeviceWriteOperation(args,pcb);

            return deviceInfoReturnImplDTO;
        } else {
            System.out.println("未找到该设备");
            return null;
        }

    }

    // 检测所有的设备队列
    @Scheduled(fixedRate = 1000)
    public void checkAllDeviceQueue() {
            //定时检测所有设备的状态,如果设备准备好，则调度队列中的执行
            protectedMemory.getDeviceQueue().forEach(device -> {
                if(!device.isBusy()){
                    //设备空闲，调度队列中的进程,触发irlIO,全部释放
                    ConcurrentLinkedQueue<PCB> waitQueue = device.getDeviceWaitingQueue();
                    while(!waitQueue.isEmpty()){
                        PCB pcb = waitQueue.poll();
                        log.info("设备 " + device.getDeviceName() + " 现在空闲，进程 " + pcb.getPid() + " 开始使用");
                        DeviceInfoReturnImplDTO deviceInfoReturnImplDTO = new DeviceInfoReturnImplDTO();
                        deviceInfoReturnImplDTO.setPcb(pcb).setDeviceName(device.getDeviceName());
                        interruptController.trigger(deviceInfoReturnImplDTO);
                    }
                }
            });

    }

}
