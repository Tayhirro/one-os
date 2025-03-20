package newOs.kernel.device.DeviceImpl;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import newOs.common.fileSystemConstant.DeviceStatusType;
import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.dto.resp.DeviceManage.DevicePCBQueryAllRespDTO;
import newOs.dto.resp.DeviceManage.DeviceQueryAllRespDTO;
import newOs.kernel.device.DeviceDriver;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

@Data
@Getter
public class DeviceDriverImpl implements DeviceDriver, Runnable {
    private final ConcurrentLinkedQueue<PCB> deviceWaitingQueue ;
    private final String deviceName;
    private final JSONObject deviceInfo;
    private boolean isBusy = false; // 设备状态
    private Consumer<DeviceInfoReturnImplDTO> callback;

    public DeviceDriverImpl(String deviceName, JSONObject deviceInfo) {
        this.deviceName = deviceName;
        this.deviceInfo = deviceInfo;
        this.deviceWaitingQueue = new ConcurrentLinkedQueue<>();
    }


    @Override
    public DeviceInfoReturnImplDTO add(PCB pcb) {
        System.out.println("进程 " + pcb.getPid() + " 请求使用设备 " + deviceName);
        DeviceInfoReturnImplDTO deviceInfoReturnImplDTO = new DeviceInfoReturnImplDTO();
        deviceInfoReturnImplDTO.setDeviceName(deviceName).setPcb(pcb);
        if (!isBusy) {
            // 设备空闲，直接使用
            isBusy = true;
            System.out.println("进程 " + pcb.getPid() + " 直接使用设备 " + deviceName);
            deviceInfoReturnImplDTO.setDeviceStatusType(DeviceStatusType.FREE);
        } else {
            // 设备忙，将进程加入等待队列
            deviceWaitingQueue.offer(pcb);
            System.out.println("进程 " + pcb.getPid() + " 等待设备 " + deviceName + " 可用");
            deviceInfoReturnImplDTO.setDeviceStatusType(DeviceStatusType.BUSY);
        }
        return deviceInfoReturnImplDTO;
    }

    public DeviceInfoReturnImplDTO releaseDevice() {
        DeviceInfoReturnImplDTO deviceInfoReturnImplDTO = new DeviceInfoReturnImplDTO();
        if (deviceWaitingQueue.isEmpty()) {
            // 没有等待进程，设备变为空闲状态
            isBusy = false;
            System.out.println("设备 " + deviceName + " 现在空闲");
        }
        return deviceInfoReturnImplDTO;
    }

    @Override
    public DevicePCBQueryAllRespDTO queryAllDeviceInfo() {
        // 这里可以返回当前设备状态
        DevicePCBQueryAllRespDTO respDTO = new DevicePCBQueryAllRespDTO();
//        respDTO.setDeviceName(deviceName);
//        respDTO.setDeviceStatus(isBusy ? "Busy" : "Idle");
//        respDTO.setWaitingProcessCount(deviceWaitingQueue.size());
        return respDTO;
    }
    @Override
    public void run() {
//        while (true) {
//            if (isBusy) {
//                try {
//                    Thread.sleep(1000);
//                    releaseDevice();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }

}
