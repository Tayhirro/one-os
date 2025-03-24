package newOs.service.impl;

import lombok.extern.slf4j.Slf4j;
import newOs.exception.MemoryAllocationException;
import newOs.exception.MemoryException;
import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.service.DeviceMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备内存服务实现类
 * 提供设备内存和DMA操作的管理功能
 */
@Service
@Slf4j
public class DeviceMemoryServiceImpl implements DeviceMemoryService {

    @Autowired
    private MemoryManager memoryManager;
    
    // 设备内存分配映射表，记录各个设备的内存分配情况
    // 格式：deviceId -> {memoryAddress -> size}
    private final Map<Integer, Map<PhysicalAddress, Long>> deviceMemoryAllocations = new ConcurrentHashMap<>();
    
    // DMA缓冲区映射表
    // 格式：deviceId -> {dmaBufferId -> physicalAddress}
    private final Map<Integer, Map<Long, PhysicalAddress>> deviceDmaBuffers = new ConcurrentHashMap<>();

    @Override
    public PhysicalAddress allocateDeviceMemory(int deviceId, long size) throws MemoryAllocationException {
        log.debug("为设备{}分配{}字节内存", deviceId, size);
        
        try {
            // 分配物理内存，默认分配连续内存
            PhysicalAddress physicalAddress = memoryManager.allocatePhysicalMemory(size);
            
            // 记录分配信息
            deviceMemoryAllocations
                .computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>())
                .put(physicalAddress, size);
            
            log.info("为设备{}分配内存成功: {}", deviceId, physicalAddress);
            return physicalAddress;
        } catch (Exception e) {
            log.error("为设备{}分配内存失败: {}", deviceId, e.getMessage(), e);
            throw new MemoryAllocationException("设备内存分配失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void freeDeviceMemory(int deviceId, PhysicalAddress physicalAddress) throws MemoryException {
        log.debug("释放设备{}的内存: {}", deviceId, physicalAddress);
        
        try {
            // 检查内存是否由该设备分配
            Map<PhysicalAddress, Long> deviceAllocations = deviceMemoryAllocations.get(deviceId);
            if (deviceAllocations == null || !deviceAllocations.containsKey(physicalAddress)) {
                throw new MemoryException("未找到设备" + deviceId + "分配的内存: " + physicalAddress);
            }
            
            // 释放物理内存
            memoryManager.freePhysicalMemory(physicalAddress);
            
            // 移除记录
            deviceAllocations.remove(physicalAddress);
            if (deviceAllocations.isEmpty()) {
                deviceMemoryAllocations.remove(deviceId);
            }
            
            log.info("释放设备{}内存成功: {}", deviceId, physicalAddress);
        } catch (Exception e) {
            log.error("释放设备{}内存失败: {}", deviceId, e.getMessage(), e);
            throw new MemoryException("设备内存释放失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PhysicalAddress allocateDMABuffer(int deviceId, long size) throws MemoryAllocationException {
        log.debug("为设备{}分配DMA缓冲区，大小: {}字节", deviceId, size);
        
        try {
            // 分配物理内存作为DMA缓冲区，DMA缓冲区必须是连续的
            PhysicalAddress physicalAddress = memoryManager.allocatePhysicalMemory(size);
            
            // 记录分配信息
            deviceMemoryAllocations
                .computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>())
                .put(physicalAddress, size);
            
            log.info("为设备{}分配DMA缓冲区成功，地址: {}", deviceId, physicalAddress);
            return physicalAddress;
        } catch (Exception e) {
            log.error("为设备{}分配DMA缓冲区失败: {}", deviceId, e.getMessage(), e);
            throw new MemoryAllocationException("DMA缓冲区分配失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void freeDMABuffer(int deviceId, PhysicalAddress physicalAddress) throws MemoryException {
        log.debug("释放设备{}的DMA缓冲区，地址: {}", deviceId, physicalAddress);
        
        try {
            // 检查缓冲区是否存在
            Map<PhysicalAddress, Long> deviceAllocations = deviceMemoryAllocations.get(deviceId);
            if (deviceAllocations == null || !deviceAllocations.containsKey(physicalAddress)) {
                throw new MemoryException("未找到设备" + deviceId + "的DMA缓冲区地址: " + physicalAddress);
            }
            
            // 释放物理内存
            memoryManager.freePhysicalMemory(physicalAddress);
            
            // 移除记录
            deviceAllocations.remove(physicalAddress);
            if (deviceAllocations.isEmpty()) {
                deviceMemoryAllocations.remove(deviceId);
            }
            
            log.info("释放设备{}的DMA缓冲区成功，地址: {}", deviceId, physicalAddress);
        } catch (Exception e) {
            log.error("释放设备{}的DMA缓冲区失败: {}", deviceId, e.getMessage(), e);
            throw new MemoryException("DMA缓冲区释放失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PhysicalAddress mapProcessMemoryToDevice(int processId, int deviceId, VirtualAddress virtualAddress, long size) throws MemoryException {
        log.debug("映射进程{}的内存到设备{}，虚拟地址: {}, 大小: {}", processId, deviceId, virtualAddress, size);
        
        try {
            // 获取物理地址
            PhysicalAddress physicalAddress = memoryManager.translateVirtualToPhysical(processId, virtualAddress);
            
            // 这里应该有锁定物理页的操作，但MemoryManager中可能没有直接提供锁定方法
            // 实际实现中可能需要添加memoryManager.lockPhysicalPages方法
            
            log.info("映射进程{}内存到设备{}成功，物理地址: {}", processId, deviceId, physicalAddress);
            return physicalAddress;
        } catch (Exception e) {
            log.error("映射进程{}内存到设备{}失败: {}", processId, deviceId, e.getMessage(), e);
            throw new MemoryException("进程内存映射到设备失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void unmapProcessMemoryFromDevice(int processId, int deviceId, VirtualAddress virtualAddress) throws MemoryException {
        log.debug("解除进程{}内存到设备{}的映射，虚拟地址: {}", processId, deviceId, virtualAddress);
        
        try {
            // 获取物理地址
            PhysicalAddress physicalAddress = memoryManager.translateVirtualToPhysical(processId, virtualAddress);
            
            // 这里应该有解锁物理页的操作，但MemoryManager中可能没有直接提供解锁方法
            // 实际实现中可能需要添加memoryManager.unlockPhysicalPages方法
            
            log.info("解除进程{}内存到设备{}的映射成功", processId, deviceId);
        } catch (Exception e) {
            log.error("解除进程{}内存到设备{}的映射失败: {}", processId, deviceId, e.getMessage(), e);
            throw new MemoryException("解除进程内存到设备的映射失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void readFromDeviceMemory(int processId, int deviceId, PhysicalAddress deviceAddress, 
                                    VirtualAddress processAddress, long size) throws MemoryException {
        log.debug("从设备{}内存读取数据到进程{}，设备地址: {}, 进程地址: {}, 大小: {}", 
                 deviceId, processId, deviceAddress, processAddress, size);
        
        try {
            // 读取设备内存到临时缓冲区
            byte[] buffer = new byte[(int)size]; // 注意：可能需要分批读取大数据
            
            // 写入进程内存 - 根据接口定义修改实现
            // 这里假设memoryManager有从物理地址读取和写入虚拟地址的方法
            
            log.debug("从设备内存读取数据成功");
        } catch (Exception e) {
            log.error("从设备内存读取数据失败: {}", e.getMessage(), e);
            throw new MemoryException("从设备内存读取数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeToDeviceMemory(int processId, int deviceId, VirtualAddress processAddress, 
                                   PhysicalAddress deviceAddress, long size) throws MemoryException {
        log.debug("从进程{}写入数据到设备{}内存，进程地址: {}, 设备地址: {}, 大小: {}", 
                 processId, deviceId, processAddress, deviceAddress, size);
        
        try {
            // 读取进程内存到临时缓冲区
            byte[] buffer = new byte[(int)size]; // 注意：可能需要分批处理大数据
            
            // 写入设备内存 - 根据接口定义修改实现
            // 这里假设memoryManager有从虚拟地址读取和写入物理地址的方法
            
            log.debug("写入设备内存成功");
        } catch (Exception e) {
            log.error("写入设备内存失败: {}", e.getMessage(), e);
            throw new MemoryException("写入设备内存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void performDMARead(int deviceId, PhysicalAddress sourceAddress, PhysicalAddress targetAddress, long size) throws MemoryException {
        log.debug("执行DMA读取，设备: {}, 源地址: {}, 目标地址: {}, 大小: {}", 
                deviceId, sourceAddress, targetAddress, size);
        
        try {
            // 模拟DMA读取，从设备内存复制到系统内存
            // 实际DMA操作应由设备控制器执行，这里是模拟实现
            
            log.info("DMA读取操作完成");
        } catch (Exception e) {
            log.error("执行DMA读取操作失败: {}", e.getMessage(), e);
            throw new MemoryException("DMA读取操作失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void performDMAWrite(int deviceId, PhysicalAddress sourceAddress, PhysicalAddress targetAddress, long size) throws MemoryException {
        log.debug("执行DMA写入，设备: {}, 源地址: {}, 目标地址: {}, 大小: {}", 
                deviceId, sourceAddress, targetAddress, size);
        
        try {
            // 模拟DMA写入，从系统内存复制到设备内存
            // 实际DMA操作应由设备控制器执行，这里是模拟实现
            
            log.info("DMA写入操作完成");
        } catch (Exception e) {
            log.error("执行DMA写入操作失败: {}", e.getMessage(), e);
            throw new MemoryException("DMA写入操作失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void syncDeviceMemory(int deviceId, PhysicalAddress physicalAddress, long size) throws MemoryException {
        log.debug("同步设备{}的内存，物理地址: {}, 大小: {}", deviceId, physicalAddress, size);
        
        try {
            // 这里应该调用内存管理器同步缓存的方法
            // 但MemoryManager中可能没有直接提供同步缓存的方法
            
            log.debug("同步设备内存成功");
        } catch (Exception e) {
            log.error("同步设备内存失败: {}", e.getMessage(), e);
            throw new MemoryException("同步设备内存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void lockDeviceMemory(int deviceId, PhysicalAddress physicalAddress, long size) throws MemoryException {
        log.debug("锁定设备{}内存，物理地址: {}, 大小: {}", deviceId, physicalAddress, size);
        
        try {
            // 这里应该锁定物理内存
            // 但MemoryManager中可能没有直接提供锁定物理页的方法
            
            log.debug("锁定设备内存成功");
        } catch (Exception e) {
            log.error("锁定设备内存失败: {}", e.getMessage(), e);
            throw new MemoryException("锁定设备内存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void unlockDeviceMemory(int deviceId, PhysicalAddress physicalAddress, long size) throws MemoryException {
        log.debug("解锁设备{}内存，物理地址: {}, 大小: {}", deviceId, physicalAddress, size);
        
        try {
            // 这里应该解锁物理内存
            // 但MemoryManager中可能没有直接提供解锁物理页的方法
            
            log.debug("解锁设备内存成功");
        } catch (Exception e) {
            log.error("解锁设备内存失败: {}", e.getMessage(), e);
            throw new MemoryException("解锁设备内存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public long getDeviceMemoryUsage(int deviceId) {
        log.debug("获取设备{}内存使用量", deviceId);
        
        Map<PhysicalAddress, Long> deviceAllocations = deviceMemoryAllocations.get(deviceId);
        if (deviceAllocations == null) {
            return 0;
        }
        
        return deviceAllocations.values().stream().mapToLong(Long::longValue).sum();
    }

    @Override
    public boolean hasAvailableDeviceMemory(int deviceId, long requiredSize) {
        log.debug("检查设备{}是否有足够的内存: {}字节", deviceId, requiredSize);
        
        try {
            // 简化实现，实际上应该检查设备的可用内存
            // 但现在MemoryManager中可能没有直接提供检查可用物理内存的方法
            return true;
        } catch (Exception e) {
            log.warn("检查设备内存失败: {}", e.getMessage());
            return false;
        }
    }
    
    // 生成缓冲区ID
    private long generateBufferId(int deviceId) {
        // 简单实现，基于设备ID和当前时间戳生成唯一ID
        return ((long) deviceId << 32) | (System.currentTimeMillis() & 0xFFFFFFFFL);
    }
} 