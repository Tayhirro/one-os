package newOs.kernel.device;


import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.dto.resp.DeviceManage.DevicePCBQueryAllRespDTO;
import newOs.dto.resp.DeviceManage.DeviceQueryAllRespDTO;
import org.springframework.scheduling.annotation.Scheduled;

import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.exception.MemoryException;

import java.util.concurrent.ConcurrentLinkedQueue;

public interface DeviceDriver {
    String getDeviceName();
    DeviceInfoReturnImplDTO add(PCB pcb);
    DevicePCBQueryAllRespDTO queryAllDeviceInfo();
    DeviceInfoReturnImplDTO releaseDevice();
    boolean isBusy();
    ConcurrentLinkedQueue<PCB> getDeviceWaitingQueue();
    
    /**
     * 设备请求分配内存
     * @param size 请求的内存大小(字节)
     * @return 分配的虚拟地址
     * @throws MemoryException 如果内存分配失败
     */
    VirtualAddress allocateDeviceMemory(long size) throws MemoryException;
    
    /**
     * 释放设备内存
     * @param address 要释放的内存虚拟地址
     * @return 是否成功释放
     */
    boolean freeDeviceMemory(VirtualAddress address);
    
    /**
     * 设备直接内存访问(DMA)
     * @param source 源地址
     * @param destination 目标地址
     * @param size 传输的数据大小(字节)
     * @return 是否传输成功
     */
    boolean dmaTransfer(PhysicalAddress source, PhysicalAddress destination, long size);
    
    /**
     * 获取设备内存使用情况
     * @return 设备内存使用情况的描述
     */
    String getDeviceMemoryUsage();
    
    /**
     * 检查设备是否有足够的内存空间
     * @param requiredSize 所需的内存大小(字节)
     * @return 是否有足够的空间
     */
    boolean hasEnoughMemory(long requiredSize);
}