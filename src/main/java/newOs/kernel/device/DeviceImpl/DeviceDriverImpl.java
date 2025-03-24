package newOs.kernel.device.DeviceImpl;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import newOs.common.fileSystemConstant.DeviceStatusType;
import newOs.component.device.Device;
import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.dto.resp.DeviceManage.DevicePCBQueryAllRespDTO;
import newOs.dto.resp.DeviceManage.DeviceQueryAllRespDTO;
import newOs.exception.MemoryException;
import newOs.kernel.device.DeviceDriver;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.interrupt.InterruptController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Data
@Getter
public class DeviceDriverImpl implements DeviceDriver, Runnable {
    private final ConcurrentLinkedQueue<PCB> deviceWaitingQueue;
    private final String deviceName;
    private final JSONObject deviceInfo;
    private boolean isBusy = false; // 设备状态
    private Consumer<DeviceInfoReturnImplDTO> callback;
    
    // 设备内存管理相关字段
    private final long totalDeviceMemory; // 设备总内存
    private final AtomicLong usedDeviceMemory = new AtomicLong(0); // 已使用的设备内存
    private final Map<Long, Long> allocatedMemory = new HashMap<>(); // 已分配的内存映射
    private JSONObject resultCache; // 读取缓冲区
    private JSONObject accessCache;
    private int isWrite = 0; // 读写标志
    private final InterruptController interruptController;
    private PCB nowPcb;
    private Device device;

    public DeviceDriverImpl(String deviceName, JSONObject deviceInfo, InterruptController interruptController, Device device) {
        this.deviceName = deviceName;
        this.deviceInfo = deviceInfo;
        this.deviceWaitingQueue = new ConcurrentLinkedQueue<>();
        this.interruptController = interruptController;
        this.device = device;
        
        // 从设备信息中获取设备内存大小，默认为1MB设备缓冲区
        this.totalDeviceMemory = deviceInfo.containsKey("bufferSize") ? 
                deviceInfo.getLong("bufferSize") : 1024 * 1024;
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
        // 可以返回当前设备状态
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
            // 有等待进程，将队首进程分配给设备
            PCB nextPcb = deviceWaitingQueue.poll();
            nowPcb = nextPcb;
            System.out.println("设备 " + deviceName + " 分配给下一个进程: " + nextPcb.getCoreId() + "-" + nextPcb.getPid());
        }
        return deviceInfoReturnImplDTO;
    }
    
    /**
     * 设备请求分配内存
     * @param size 请求的内存大小(字节)
     * @return 分配的虚拟地址
     * @throws MemoryException 如果内存分配失败
     */
    @Override
    public VirtualAddress allocateDeviceMemory(long size) throws MemoryException {
        // 检查是否有足够的内存
        if (!hasEnoughMemory(size)) {
            throw new MemoryException("设备 " + deviceName + " 缓冲区没有足够的内存分配 " + size + " 字节");
        }
        
        // 简单的内存分配策略：使用递增地址
        long baseAddress = usedDeviceMemory.get();
        long newAddress = baseAddress;
        
        // 更新已使用的内存
        usedDeviceMemory.addAndGet(size);
        
        // 记录分配的内存
        allocatedMemory.put(newAddress, size);
        
        System.out.println("设备 " + deviceName + " 分配缓冲区内存: " + size + " 字节, 地址: " + newAddress);
        
        // 返回虚拟地址
        return new VirtualAddress(newAddress);
    }
    
    /**
     * 释放设备内存
     * @param address 要释放的内存虚拟地址
     * @return 是否成功释放
     */
    @Override
    public boolean freeDeviceMemory(VirtualAddress address) {
        if (address == null) {
            return false;
        }
        
        long addr = address.getValue();
        if (!allocatedMemory.containsKey(addr)) {
            System.out.println("设备 " + deviceName + " 无法释放未分配的缓冲区地址: " + addr);
            return false;
        }
        
        // 获取要释放的内存大小
        long size = allocatedMemory.get(addr);
        
        // 更新已使用的内存
        usedDeviceMemory.addAndGet(-size);
        
        // 从分配映射中移除
        allocatedMemory.remove(addr);
        
        System.out.println("设备 " + deviceName + " 释放缓冲区内存: " + size + " 字节, 地址: " + addr);
        return true;
    }
    
    /**
     * 设备直接内存访问(DMA)
     * @param source 源地址
     * @param destination 目标地址
     * @param size 传输的数据大小(字节)
     * @return 是否传输成功
     */
    @Override
    public boolean dmaTransfer(PhysicalAddress source, PhysicalAddress destination, long size) {
        if (source == null || destination == null || size <= 0) {
            return false;
        }
        
        try {
            // 模拟DMA传输过程
            System.out.println("设备 " + deviceName + " 执行DMA传输: " +
                    size + " 字节, 从物理地址 " + source.getValue() + " 到 " + destination.getValue());
            
            // 实际应该是硬件层面的操作，这里简单模拟一下
            // 在真实实现中应该通过设备控制器执行真正的DMA传输
            Thread.sleep(Math.min(size / 1024, 100)); // 模拟传输时间，每MB最多100ms
            
            System.out.println("设备 " + deviceName + " DMA传输完成");
            return true;
        } catch (Exception e) {
            System.out.println("设备 " + deviceName + " DMA传输出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取设备内存使用情况
     * @return 设备内存使用情况的描述
     */
    @Override
    public String getDeviceMemoryUsage() {
        long used = usedDeviceMemory.get();
        double usagePercentage = (double) used / totalDeviceMemory * 100;
        
        return String.format("设备: %s\n" +
                        "缓冲区总内存: %d KB, 已用: %d KB (%.2f%%)",
                deviceName,
                totalDeviceMemory / 1024,
                used / 1024,
                usagePercentage);
    }
    
    /**
     * 检查设备是否有足够的内存空间
     * @param requiredSize 所需的内存大小(字节)
     * @return 是否有足够的空间
     */
    @Override
    public boolean hasEnoughMemory(long requiredSize) {
        long available = totalDeviceMemory - usedDeviceMemory.get();
        return available >= requiredSize;
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
