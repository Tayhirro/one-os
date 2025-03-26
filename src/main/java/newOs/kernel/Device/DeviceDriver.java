package newOs.kernel.device;


import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.dto.resp.DeviceManage.DevicePCBQueryAllRespDTO;
import newOs.dto.resp.DeviceManage.DeviceQueryAllRespDTO;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.ConcurrentLinkedQueue;

public interface DeviceDriver {
    String getDeviceName();
    DeviceInfoReturnImplDTO add(PCB pcb);
    DevicePCBQueryAllRespDTO queryAllDeviceInfo();
    DeviceInfoReturnImplDTO releaseDevice();
    boolean isBusy();
    ConcurrentLinkedQueue<PCB> getDeviceWaitingQueue();
}