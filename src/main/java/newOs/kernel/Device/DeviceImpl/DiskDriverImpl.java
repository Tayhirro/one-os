package newOs.kernel.Device.DeviceImpl;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import newOs.common.fileSystemConstant.DeviceStatusType;
import newOs.component.device.Disk;
import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.dto.resp.DeviceManage.DevicePCBQueryAllRespDTO;
import newOs.exception.MemoryException;
import newOs.kernel.Device.DeviceDriver;
import newOs.kernel.interrupt.InterruptController;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class DiskDriverImpl implements DeviceDriver, Runnable {
    private final ConcurrentLinkedQueue<PCB> deviceWaitingQueue ;
    private final String deviceName;
    private final JSONObject deviceInfo;
    private boolean isBusy = false;
    private final ReentrantLock Busylock = new ReentrantLock();
    private JSONObject resultCache; // 读取缓冲区
    private JSONObject accessCache;
    private int isWrite = 0; // 读写标志
    //private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private final InterruptController interruptController;
    private PCB nowPcb;
    private final Disk disk;

    // 设备内存管理相关字段
    private final long totalDeviceMemory; // 磁盘缓冲区总内存
    private final AtomicLong usedDeviceMemory = new AtomicLong(0); // 已使用的设备内存
    private final Map<Long, Long> allocatedMemory = new HashMap<>(); // 已分配的内存映射

    public DiskDriverImpl(String deviceName, JSONObject deviceInfo, InterruptController interruptController, Disk disk) {
        this.deviceName = deviceName;
        this.deviceInfo = deviceInfo;
        this.deviceWaitingQueue = new ConcurrentLinkedQueue<>();
        this.interruptController = interruptController;
        this.disk = disk;

        // 从设备信息中获取设备内存大小，默认为4MB磁盘缓冲区
        this.totalDeviceMemory = deviceInfo.containsKey("bufferSize") ?
                deviceInfo.getLong("bufferSize") : 4 * 1024 * 1024;
    }

    @Override
    public DeviceInfoReturnImplDTO add(PCB pcb) {
        System.out.println("进程 " + pcb.getCoreId()+"-"+pcb.getPid() + " 请求使用设备 " + deviceName);
        DeviceInfoReturnImplDTO deviceInfoReturnImplDTO = new DeviceInfoReturnImplDTO();
        deviceInfoReturnImplDTO.setDeviceName(deviceName).setPcb(pcb);
        Busylock.lock();
        try {
            if (!isBusy) {
                // 设备空闲，直接使用
                isBusy = true;
                nowPcb = pcb;
                System.out.println("进程 " + pcb.getCoreId() + "-" + pcb.getPid() + " 直接使用设备 " + deviceName);
                deviceInfoReturnImplDTO.setDeviceStatusType(DeviceStatusType.FREE);
                isBusy = false;
            } else {
                // 设备忙，将进程加入等待队列
                deviceWaitingQueue.offer(pcb);
                System.out.println("进程 " + pcb.getCoreId() + "-" + pcb.getPid() + " 等待设备 " + deviceName + " 可用");
                deviceInfoReturnImplDTO.setDeviceStatusType(DeviceStatusType.BUSY);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            Busylock.unlock();
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
            Busylock.lock();
            try {
                isBusy = false;
            }finally {
                Busylock.unlock();
            }
            System.out.println("设备 " + deviceName + " 现在空闲");
        }
        interruptController.trigger(deviceInfoReturnImplDTO);

        return deviceInfoReturnImplDTO;
    }
    @Override
    public void run() {
        //加锁设置-
            if (isWrite == 0) {
                System.out.println("设备 " + deviceName + " 读取中...");
                try {
                    Thread.sleep(2000); // 模拟设备访问时间（阻塞）
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // **设备读取完成后，生成结果**
                System.out.println("设备 " + deviceName + " 读取完成！");
            } else {
                System.out.println("设备 " + deviceName + " 写入中...");
                try {
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
        boolean shouldExecute = false;
        Busylock.lock();
        try {
            if (!isBusy) {
                // 设置状态，不在锁内做耗时操作
                isWrite = 0;
                isBusy = true;
                nowPcb = pcb;
                shouldExecute = true;
                System.out.println("进程 " + pcb.getCoreId() + "-" + pcb.getPid() + " 开始对设备进行   读    操作 " + deviceName);
            } else {
                deviceWaitingQueue.offer(pcb);
                System.out.println("进程 " + pcb.getCoreId() + "-" + pcb.getPid() + " 在读的时候 等待设备 " + deviceName + " 可用");
                deviceInfoReturnImplDTO.setDeviceStatusType(DeviceStatusType.BUSY);
            }
        } finally {
            Busylock.unlock();
        }
        if(shouldExecute){
            run();
            deviceInfoReturnImplDTO.setArgs(resultCache).setDeviceStatusType(DeviceStatusType.FREE);
            // 恢复设备空闲状态
            Busylock.lock();
            try {
                isBusy = false;
            } finally {
                Busylock.unlock();
            }
        }
        return deviceInfoReturnImplDTO;
    }
    public DeviceInfoReturnImplDTO executeDeviceWriteOperation(JSONObject args, PCB pcb) {
        // **写入操作**
        DeviceInfoReturnImplDTO deviceInfoReturnImplDTO = new DeviceInfoReturnImplDTO();
        boolean shouldExecute = false;
        Busylock.lock();
        try {
            if (!isBusy) {
                isWrite = 1; // 设置写入标志
                shouldExecute = true;
                accessCache = args; // 写入内容
                System.out.println("进程 " + pcb.getCoreId() + "-" + pcb.getPid() + " 开始对设备进行   写   操作 " + deviceName);
            } else {
                deviceWaitingQueue.offer(pcb);
                System.out.println("进程 " + pcb.getCoreId() + "-" + pcb.getPid() + " 在读的时候 等待设备 " + deviceName + " 可用");
                deviceInfoReturnImplDTO.setDeviceStatusType(DeviceStatusType.BUSY);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            Busylock.unlock();
        }
        if(shouldExecute){
            run();
            deviceInfoReturnImplDTO.setArgs(resultCache).setDeviceStatusType(DeviceStatusType.FREE);
            // 恢复设备空闲状态
            Busylock.lock();
            try {
                isBusy = false;
            } finally {
                Busylock.unlock();
            }
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
            throw new MemoryException("磁盘设备 " + deviceName + " 缓冲区没有足够的内存分配 " + size + " 字节");
        }

        // 简单的内存分配策略：使用递增地址
        long baseAddress = usedDeviceMemory.get();
        long newAddress = baseAddress;

        // 更新已使用的内存
        usedDeviceMemory.addAndGet(size);

        // 记录分配的内存
        allocatedMemory.put(newAddress, size);

        System.out.println("磁盘设备 " + deviceName + " 分配缓冲区内存: " + size + " 字节, 地址: " + newAddress);

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
            System.out.println("磁盘设备 " + deviceName + " 无法释放未分配的缓冲区地址: " + addr);
            return false;
        }

        // 获取要释放的内存大小
        long size = allocatedMemory.get(addr);

        // 更新已使用的内存
        usedDeviceMemory.addAndGet(-size);

        // 从分配映射中移除
        allocatedMemory.remove(addr);

        System.out.println("磁盘设备 " + deviceName + " 释放缓冲区内存: " + size + " 字节, 地址: " + addr);
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
            // 模拟磁盘DMA传输过程
            System.out.println("磁盘设备 " + deviceName + " 执行DMA传输: " +
                    size + " 字节, 从 " + source.getValue() + " 到 " + destination.getValue());

            // 在实际实现中，这里应该调用磁盘控制器执行实际的DMA传输
            // 这里我们可以利用disk实例
            boolean success = disk.transferData(source.getValue(), destination.getValue(), size);

            if (success) {
                System.out.println("磁盘设备 " + deviceName + " DMA传输完成");
            } else {
                System.out.println("磁盘设备 " + deviceName + " DMA传输失败");
            }

            return success;
        } catch (Exception e) {
            System.out.println("磁盘设备 " + deviceName + " DMA传输出错: " + e.getMessage());
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

        // 获取磁盘使用信息
        long diskTotal = disk != null ? disk.getTotalCapacity() : 0;
        long diskUsed = disk != null ? disk.getUsedCapacity() : 0;
        double diskUsagePercentage = diskTotal > 0 ? (double) diskUsed / diskTotal * 100 : 0;

        return String.format("磁盘设备: %s\n" +
                        "缓冲区总内存: %d KB, 已用: %d KB (%.2f%%)\n" +
                        "磁盘总容量: %d MB, 已用: %d MB (%.2f%%)",
                deviceName,
                totalDeviceMemory / 1024,
                used / 1024,
                usagePercentage,
                diskTotal / (1024 * 1024),
                diskUsed / (1024 * 1024),
                diskUsagePercentage);
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
}
