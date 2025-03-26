package newOs.kernel.device.DeviceImpl;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import newOs.common.fileSystemConstant.DeviceStatusType;
import newOs.component.device.Disk;
import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoImplDTO;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.resp.DeviceManage.DevicePCBQueryAllRespDTO;
import newOs.kernel.device.DeviceDriver;
import newOs.kernel.filesystem.FileReader;
import newOs.kernel.filesystem.FileWriter;
import newOs.kernel.interrupt.InterruptController;


import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

@Data
public class DiskDriverImpl implements DeviceDriver, Runnable {
    private final ConcurrentLinkedQueue<PCB> deviceWaitingQueue ;
    private final String deviceName;
    private final JSONObject deviceInfo;
    private boolean isBusy = false;

    private JSONObject resultCache; // 读取缓冲区

    private JSONObject accessCache;
    private int isWrite = 0; // 读写标志
    private final InterruptController interruptController;
    private PCB nowPcb;
    private final Disk disk;

    public DiskDriverImpl(String deviceName, JSONObject deviceInfo, InterruptController interruptController, Disk disk) {
        this.deviceName = deviceName;
        this.deviceInfo = deviceInfo;
        this.deviceWaitingQueue = new ConcurrentLinkedQueue<>();
        this.interruptController = interruptController;
        this.disk = disk;
    }

    @Override
    public DeviceInfoReturnImplDTO add(PCB pcb) {
        System.out.println("进程 " + pcb.getCoreId()+"-"+pcb.getPid() + " 请求使用设备 " + deviceName);
        DeviceInfoReturnImplDTO deviceInfoReturnImplDTO = new DeviceInfoReturnImplDTO();
        deviceInfoReturnImplDTO.setDeviceName(deviceName).setPcb(pcb);
        if (!isBusy) {
            // 设备空闲，直接使用
            isBusy = true;
            nowPcb = pcb;
            System.out.println("进程 " + pcb.getCoreId() +"-"+pcb.getPid()+ " 直接使用设备 " + deviceName);
            deviceInfoReturnImplDTO.setDeviceStatusType(DeviceStatusType.FREE);
            isBusy = false;
        } else {
            // 设备忙，将进程加入等待队列
            deviceWaitingQueue.offer(pcb);
            System.out.println("进程 " + pcb.getCoreId() +"-"+pcb.getPid()+ " 等待设备 " + deviceName + " 可用");
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
        }
        interruptController.trigger(deviceInfoReturnImplDTO);

        return deviceInfoReturnImplDTO;
    }
    @Override
    public void run() {
        if(isWrite==0) {
            System.out.println("设备 " + deviceName + " 读取中...");
            try {
                // readDevice()
                FileReader fileReader = FileReader.getFileReader();
                boolean readSuccess = fileReader.readDevice(deviceName, resultCache);

                if (readSuccess) {
                    System.out.println("设备信息读取成功 ");
                } else {
                    System.err.println("设备 " + deviceName + " 读取失败 " );
                }
                Thread.sleep(2000); // 模拟设备访问时间（阻塞）

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // **设备读取完成后，生成结果**
            System.out.println("设备 " + deviceName + " 读取完成！");
        }else{
            System.out.println("设备 " + deviceName + " 写入中...");
            try {

                // writeDevice()
                FileWriter fileWriter = FileWriter.getFileWriter();
                String writeResult = fileWriter.writeToDevice(deviceName, accessCache);

                if (writeResult.startsWith("Success")) {
                    System.out.println("设备写入成功 " );
                } else {
                    System.err.println("设备写入失败 ");
                }
                Thread.sleep(2000); // 模拟设备访问时间（阻塞）
                //

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // **设备写入完成后，生成结果**
            System.out.println("设备 " + deviceName + " 写入完成！");
        }
    }
    // **直接执行 run()，阻塞进程**
    public DeviceInfoReturnImplDTO executeDeviceReadOperation(PCB pcb){
        //不会执行run，而是提交到线程池中
        DeviceInfoReturnImplDTO deviceInfoReturnImplDTO = new DeviceInfoReturnImplDTO();
        if (!isBusy) {
            // 设备空闲，直接使用
            isBusy = true;
            nowPcb = pcb;
            System.out.println("进程 " + pcb.getCoreId() +"-"+pcb.getPid()+ " 开始对设备进行   读    操作 " + deviceName);
            run();
            deviceInfoReturnImplDTO.setArgs(resultCache).setDeviceStatusType(DeviceStatusType.FREE);
            isBusy = false;
        }else{
            deviceWaitingQueue.offer(pcb);
            System.out.println("进程 " + pcb.getCoreId() +"-"+pcb.getPid()+ " 在读的时候 等待设备 " + deviceName + " 可用");
            deviceInfoReturnImplDTO.setDeviceStatusType(DeviceStatusType.BUSY);
        }
        return deviceInfoReturnImplDTO;
    }
    public DeviceInfoReturnImplDTO executeDeviceWriteOperation(JSONObject args,PCB pcb) {
        // **写入操作**
        DeviceInfoReturnImplDTO deviceInfoReturnImplDTO = new DeviceInfoReturnImplDTO();
        if (!isBusy) {
            isWrite = 1; // 设置写入标志
            accessCache = args; // 写入内容
            System.out.println("进程 " + pcb.getCoreId() +"-"+pcb.getPid()+ " 开始对设备进行   写   操作 " + deviceName);
            isBusy = true;
            run(); // 直接调用 run()，进程会阻塞等待执行完成
            isBusy = false;
            deviceInfoReturnImplDTO.setArgs(resultCache).setDeviceStatusType(DeviceStatusType.FREE);
        }else{
            deviceWaitingQueue.offer(pcb);
            System.out.println("进程 " + pcb.getCoreId() +"-"+pcb.getPid()+ " 在读的时候 等待设备 " + deviceName + " 可用");
            deviceInfoReturnImplDTO.setDeviceStatusType(DeviceStatusType.BUSY);
        }
        return deviceInfoReturnImplDTO;
    }
}
