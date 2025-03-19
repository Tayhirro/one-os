package newOs.kernel.device.DeviceImpl;

import com.alibaba.fastjson.JSONObject;
import newOs.common.fileSystemConstant.DeviceStatusType;
import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.dto.resp.DeviceManage.DevicePCBQueryAllRespDTO;
import newOs.kernel.device.DeviceDriver;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DiskDriverImpl implements DeviceDriver, Runnable{
    private final ConcurrentLinkedQueue<PCB> deviceWaitingQueue ;
    private final String deviceName;
    private final JSONObject deviceInfo;
    private boolean isBusy = false;

    public DiskDriverImpl(String deviceName, JSONObject deviceInfo) {
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

    @Override
    public DevicePCBQueryAllRespDTO queryAllDeviceInfo() {
        // 这里可以返回当前设备状态
        DevicePCBQueryAllRespDTO respDTO = new DevicePCBQueryAllRespDTO();
//        respDTO.setDeviceName(deviceName);
//        respDTO.setDeviceStatus(isBusy ? "Busy" : "Idle");
//        respDTO.setWaitingProcessCount(deviceWaitingQueue.size());
        return respDTO;
    }

    public DeviceInfoReturnImplDTO releaseDevice() {
        DeviceInfoReturnImplDTO deviceInfoReturnImplDTO = new DeviceInfoReturnImplDTO();
        if (deviceWaitingQueue.isEmpty()) {
            // 没有等待进程，设备变为空闲状态
            isBusy = false;
            System.out.println("设备 " + deviceName + " 现在空闲");
        } else {
            // 有等待进程，调度下一个
            PCB nextPcb = deviceWaitingQueue.poll();
            System.out.println("进程 " + nextPcb.getPid() + " 开始使用设备 " + deviceName);
            //接下来会造成硬件中断，推送给cpu irl
            //
            //
            //
            //
            //
        }
        return deviceInfoReturnImplDTO;
    }
    @Override
    public void run() {     //进行磁盘的文件读取
          //在此进行文件的读取



    }

}
