package newOs.kernel.memory.virtual;

import lombok.extern.slf4j.Slf4j;
import newOs.exception.AddressTranslationException;
import newOs.exception.MemoryException;
import newOs.exception.PageFaultException;
import newOs.kernel.memory.allocation.MemoryBlock;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于页表的地址转换默认实现
 */
@Component
@Slf4j
public class PageTableAddressTranslation implements AddressTranslation {

    // 进程页表映射
    private final Map<Integer, PageTable> processPageTables = new ConcurrentHashMap<>();
    
    // 虚拟地址到物理地址的直接映射缓存
    private final Map<Integer, Map<Long, MemoryBlock>> processMemoryBlocks = new ConcurrentHashMap<>();
    
    // 进程内存块大小映射
    private final Map<Integer, Map<Long, Integer>> processMemoryBlockSizes = new ConcurrentHashMap<>();
    
    @Override
    public PhysicalAddress translateToPhysical(int processId, VirtualAddress virtualAddress)
            throws AddressTranslationException, PageFaultException {
        // 获取页表
        PageTable pageTable = getPageTable(processId);
        if (pageTable == null) {
            throw new AddressTranslationException("进程页表不存在", processId, virtualAddress);
        }
        
        try {
            // 使用页表进行地址转换
            return pageTable.translate(processId, virtualAddress);
        } catch (Exception e) {
            if (e instanceof PageFaultException) {
                throw (PageFaultException) e;
            }
            throw new AddressTranslationException("地址转换失败: " + e.getMessage(), processId, virtualAddress, e);
        }
    }

    @Override
    public VirtualAddress mapMemoryBlock(int processId, MemoryBlock memoryBlock)
            throws AddressTranslationException {
        try {
            // 创建新的虚拟地址
            VirtualAddress virtualAddress = new VirtualAddress();
            virtualAddress.setValue(generateNextVirtualAddress(processId));
            
            // 将内存块与虚拟地址关联
            processMemoryBlocks.computeIfAbsent(processId, k -> new HashMap<>())
                .put(virtualAddress.getValue(), memoryBlock);
            
            // 更新页表
            PageTable pageTable = getOrCreatePageTable(processId);
            pageTable.addMapping(virtualAddress, memoryBlock);
            
            return virtualAddress;
        } catch (Exception e) {
            throw new AddressTranslationException("映射内存块失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void unmapMemoryBlock(int processId, VirtualAddress virtualAddress)
            throws AddressTranslationException {
        try {
            // 记录映射大小用于日志
            Integer size = null;
            Map<Long, Integer> sizeMap = processMemoryBlockSizes.get(processId);
            if (sizeMap != null) {
                size = sizeMap.get(virtualAddress.getValue());
            }
            
            // 移除映射
            PageTable pageTable = getPageTable(processId);
            if (pageTable != null) {
                pageTable.removeMapping(virtualAddress);
            }
            
            // 从内存块映射中移除
            Map<Long, MemoryBlock> blocks = processMemoryBlocks.get(processId);
            if (blocks != null) {
                blocks.remove(virtualAddress.getValue());
            }
            
            // 从内存块大小映射中移除
            if (sizeMap != null) {
                sizeMap.remove(virtualAddress.getValue());
                log.debug("为进程{}移除了虚拟地址0x{}的映射，大小{}字节", 
                          processId, Long.toHexString(virtualAddress.getValue()), 
                          size != null ? size : "未知");
            }
        } catch (Exception e) {
            throw new AddressTranslationException("解除内存块映射失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void createPrivateMapping(int processId, VirtualAddress virtualAddress,
                               PhysicalAddress physicalAddress, long size)
            throws AddressTranslationException {
        try {
            // 获取或创建页表
            PageTable pageTable = getOrCreatePageTable(processId);
            
            // 创建私有映射 - 这里简化为基本映射，实际应实现写时复制机制
            long physAddr = physicalAddress.getValue();
            pageTable.mapPage(virtualAddress.getValue(), physAddr, true, true, false);
        } catch (Exception e) {
            throw new AddressTranslationException("创建私有映射失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void createSharedMapping(int processId, VirtualAddress virtualAddress,
                              PhysicalAddress physicalAddress, long size)
            throws AddressTranslationException {
        try {
            // 获取或创建页表
            PageTable pageTable = getOrCreatePageTable(processId);
            
            // 创建共享映射 - 这里简化实现
            long physAddr = physicalAddress.getValue();
            pageTable.mapPage(virtualAddress.getValue(), physAddr, true, true, false);
        } catch (Exception e) {
            throw new AddressTranslationException("创建共享映射失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeMapping(int processId, VirtualAddress virtualAddress, long size)
            throws AddressTranslationException {
        try {
            // 获取页表
            PageTable pageTable = getPageTable(processId);
            if (pageTable == null) {
                throw new AddressTranslationException("进程页表不存在", processId, virtualAddress);
            }
            
            // 简化实现 - 仅移除起始地址的映射
            pageTable.removeMapping(virtualAddress);
        } catch (Exception e) {
            throw new AddressTranslationException("移除映射失败: " + e.getMessage(), e);
        }
    }

    @Override
    public VirtualAddress mapPhysicalAddress(int processId, PhysicalAddress physicalAddress, int size) throws MemoryException {
        try {
            // 获取或创建页表
            PageTable pageTable = getOrCreatePageTable(processId);
            
            // 创建新的虚拟地址
            VirtualAddress virtualAddress = new VirtualAddress();
            virtualAddress.setValue(pageTable.getNextAvailableAddress());
            
            // 计算需要映射的页数
            int pageSize = VirtualAddress.PAGE_SIZE;
            int pageCount = (size + pageSize - 1) / pageSize; // 向上取整
            
            // 物理地址起始位置
            long physAddr = physicalAddress.getValue();
            // 虚拟地址起始位置
            long virtAddr = virtualAddress.getValue();
            
            log.debug("为进程{}创建映射：大小={}字节，需要{}个页面", processId, size, pageCount);
            
            // 为每个页面创建映射
            for (int i = 0; i < pageCount; i++) {
                // 计算当前页的物理地址和虚拟地址
                long currentPhysAddr = physAddr + (i * pageSize);
                long currentVirtAddr = virtAddr + (i * pageSize);
                
                // 创建映射
                pageTable.mapPage(currentVirtAddr, currentPhysAddr, true, true, false);
                
                log.debug("映射第{}个页面：虚拟地址=0x{}，物理地址=0x{}", 
                          i+1, Long.toHexString(currentVirtAddr), Long.toHexString(currentPhysAddr));
            }
            
            // 更新页表中的下一个可用地址
            pageTable.updateNextAvailableAddress(size);
            
            // 保存内存块信息 - 在PageTable中添加大小信息
            // 使用模型包中的MemoryBlock类
            Map<Long, Integer> memoryBlockSizes = processMemoryBlockSizes.computeIfAbsent(processId, pid -> new ConcurrentHashMap<>());
            memoryBlockSizes.put(virtualAddress.getValue(), size);
            
            log.debug("为进程{}创建了虚拟地址0x{}到物理地址0x{}的映射，总大小{}字节", 
                      processId, Long.toHexString(virtualAddress.getValue()), 
                      Long.toHexString(physAddr), size);
            
            return virtualAddress;
        } catch (Exception e) {
            throw new MemoryException("映射物理地址失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取进程的页表
     * @param processId 进程ID
     * @return 页表，如果不存在则返回null
     */
    private PageTable getPageTable(int processId) {
        return processPageTables.get(processId);
    }
    
    /**
     * 获取或创建进程的页表
     * @param processId 进程ID
     * @return 页表
     */
    private PageTable getOrCreatePageTable(int processId) {
        return processPageTables.computeIfAbsent(processId, PageTable::new);
    }
    
    /**
     * 为指定进程生成下一个可用的虚拟地址
     * @param processId 进程ID
     * @return 虚拟地址值
     */
    private long generateNextVirtualAddress(int processId) {
        PageTable pageTable = getOrCreatePageTable(processId);
        return pageTable.getNextAvailableAddress();
    }
} 