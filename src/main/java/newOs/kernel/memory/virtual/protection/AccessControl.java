package newOs.kernel.memory.virtual.protection;

import lombok.extern.slf4j.Slf4j;
import newOs.exception.MemoryProtectionException;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.virtual.PageTable;
import newOs.kernel.memory.virtual.paging.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 访问控制实现类
 * 基于页表和页面权限位实现内存保护机制
 */
@Component
@Slf4j
public class AccessControl implements MemoryProtection {

    /**
     * 页面大小掩码，用于获取页内偏移
     */
    public static final long PAGE_SIZE_MASK = Page.PAGE_SIZE - 1;
    
    /**
     * 页表引用，用于获取和修改页面的权限
     */
    private final PageTable pageTable;
    
    /**
     * 当前进程ID
     */
    private int processId;
    
    /**
     * 每个进程的内存保护区域映射
     * 键：进程ID
     * 值：该进程的保护区域映射
     */
    private final Map<Integer, Map<Long, ProtectionRegion>> protectionRegionMap;
    
    /**
     * 共享内存区域记录
     * 键：拥有者进程ID-目标进程ID-起始地址
     * 值：共享内存区域信息
     */
    private final Map<String, SharedMemoryRegion> sharedMemoryRegions;
    
    /**
     * 锁定页面记录
     * 键：进程ID
     * 值：锁定的页面地址集合
     */
    private final Map<Integer, Set<Long>> lockedPages;
    
    /**
     * 访问违规记录
     * 键：进程ID
     * 值：该进程的违规记录列表
     */
    private final Map<Integer, Queue<ViolationRecord>> violationRecords;
    
    /**
     * 统计数据
     */
    private final AtomicLong totalViolations = new AtomicLong(0);
    private final AtomicLong readViolations = new AtomicLong(0);
    private final AtomicLong writeViolations = new AtomicLong(0);
    private final AtomicLong executeViolations = new AtomicLong(0);
    
    /**
     * 访问违规计数
     */
    private final AtomicLong violationCount = new AtomicLong(0);
    
    /**
     * 读取违规计数
     */
    private final AtomicLong readViolationCount = new AtomicLong(0);
    
    /**
     * 写入违规计数
     */
    private final AtomicLong writeViolationCount = new AtomicLong(0);
    
    /**
     * 执行违规计数
     */
    private final AtomicLong executeViolationCount = new AtomicLong(0);
    
    /**
     * 按进程记录的违规信息
     * 键：进程ID
     * 值：违规次数
     */
    private final Map<Integer, AtomicLong> processViolationCounts = new HashMap<>();
    
    /**
     * 违规详情记录（最近的几次违规）
     */
    private final Queue<ViolationRecord> recentViolations = new LinkedList<>();
    private static final int MAX_RECENT_VIOLATIONS = 50;
    
    private static class ProtectionRegion {
        // 起始虚拟地址
        private final long startAddress;
        // 结束虚拟地址
        private final long endAddress;
        // 访问权限
        private AccessPermission permission;
        
        public ProtectionRegion(long startAddress, long length, AccessPermission permission) {
            this.startAddress = startAddress;
            this.endAddress = startAddress + length - 1;
            this.permission = permission;
        }
        
        /**
         * 检查地址是否在当前区域内
         * @param address 要检查的地址
         * @return 是否在区域内
         */
        public boolean contains(long address) {
            return address >= startAddress && address <= endAddress;
        }
        
        /**
         * 获取起始地址
         * @return 起始地址
         */
        public VirtualAddress getStartAddress() {
            return new VirtualAddress(startAddress);
        }
        
        /**
         * 获取结束地址
         * @return 结束地址
         */
        public VirtualAddress getEndAddress() {
            return new VirtualAddress(endAddress);
        }
    }
    
    /**
     * 共享内存区域信息
     */
    private static class SharedMemoryRegion {
        // 拥有者进程ID
        private final int ownerPid;
        // 目标进程ID
        private final int targetPid;
        // 起始虚拟地址
        private final VirtualAddress startAddress;
        // 长度
        private final long length;
        // 访问权限
        private final AccessPermission permission;
        
        /**
         * 创建共享内存区域
         * @param ownerPid 拥有者进程ID
         * @param targetPid 目标进程ID
         * @param startAddress 起始虚拟地址
         * @param length 长度
         * @param permission 访问权限
         */
        public SharedMemoryRegion(int ownerPid, int targetPid, VirtualAddress startAddress, 
                                long length, AccessPermission permission) {
            this.ownerPid = ownerPid;
            this.targetPid = targetPid;
            this.startAddress = startAddress;
            this.length = length;
            this.permission = permission;
        }
        
        /**
         * 获取拥有者进程ID
         * @return 拥有者进程ID
         */
        public int getSourceProcessId() {
            return ownerPid;
        }
        
        /**
         * 获取目标进程ID
         * @return 目标进程ID
         */
        public int getTargetProcessId() {
            return targetPid;
        }
        
        /**
         * 获取起始地址
         * @return 起始地址
         */
        public VirtualAddress getStartAddress() {
            return startAddress;
        }
        
        /**
         * 获取长度
         * @return 长度
         */
        public long getLength() {
            return length;
        }
        
        /**
         * 获取访问权限
         * @return 访问权限
         */
        public AccessPermission getPermission() {
            return permission;
        }
    }
    
    /**
     * 构造访问控制对象
     * @param pageTable 页表对象
     */
    @Autowired
    public AccessControl(PageTable pageTable) {
        this.pageTable = pageTable;
        this.processId = -1; // 默认为系统进程
        this.protectionRegionMap = new ConcurrentHashMap<>();
        this.sharedMemoryRegions = new ConcurrentHashMap<>();
        this.lockedPages = new ConcurrentHashMap<>();
        this.violationRecords = new ConcurrentHashMap<>();
    }
    
    /**
     * 设置当前进程ID
     * @param processId 进程ID
     */
    public void setProcessId(int processId) {
        this.processId = processId;
    }
    
    /**
     * 获取当前进程ID
     * @return 进程ID
     */
    public int getProcessId() {
        return processId;
    }
    
    /**
     * 设置内存权限
     * @param pid 进程ID
     * @param startAddress 起始虚拟地址
     * @param length 内存区域长度
     * @param permission 权限
     * @return 是否设置成功
     */
    public boolean setPermission(int pid, VirtualAddress startAddress, long length, AccessPermission permission) {
        // 获取或创建进程的保护区域映射
        Map<Long, ProtectionRegion> processRegions = protectionRegionMap.computeIfAbsent(
                pid, k -> new ConcurrentHashMap<>());
        
        // 创建新的保护区域
        ProtectionRegion region = new ProtectionRegion(
                startAddress.getValue(), length, permission);
        
        // 添加到映射中
        processRegions.put(startAddress.getValue(), region);
        
        // 更新页表中相应页面的权限
        try {
            int pageSize = Page.PAGE_SIZE;
            long startPage = startAddress.getValue() / pageSize;
            long endPage = (startAddress.getValue() + length - 1) / pageSize;
            
            for (long pageNum = startPage; pageNum <= endPage; pageNum++) {
                // 计算当前页的虚拟地址
                VirtualAddress pageAddr = new VirtualAddress((int) pageNum * pageSize);
                
                // 获取页面
                Page page = pageTable.getPage(pageAddr, pid);
                
                if (page != null) {
                    // 设置页面权限
                    page.setProtection(permission.canWrite(), permission.canExecute());
                }
            }
            
            log.debug("已设置进程{}的内存权限: 起始地址={}, 长度={}, 权限={}", 
                     pid, startAddress.getValue(), length, permission);
            
            return true;
        } catch (Exception e) {
            log.error("设置内存权限失败: pid={}, 起始地址={}, 长度={}, 权限={}", 
                     pid, startAddress.getValue(), length, permission, e);
            
            // 移除刚才添加的区域
            processRegions.remove(startAddress.getValue());
            
            return false;
        }
    }
    
    /**
     * 获取内存权限
     * @param pid 进程ID
     * @param address 虚拟地址
     * @return 权限
     */
    public AccessPermission getPermission(int pid, VirtualAddress address) {
        // 获取进程的保护区域映射
        Map<Long, ProtectionRegion> processRegions = protectionRegionMap.get(pid);
        
        if (processRegions != null) {
            // 查找包含该地址的所有区域
            for (ProtectionRegion region : processRegions.values()) {
                if (region.contains(address.getValue())) {
                    return region.permission;
                }
            }
        }
        
        // 如果没有找到指定的保护区域，尝试从页表获取权限信息
        try {
            Page page = pageTable.getPage(address, pid);
            
            if (page != null) {
                boolean canRead = page.isReadable();
                boolean canWrite = page.isWritable();
                boolean canExecute = page.isExecutable();
                
                if (canRead && canWrite && canExecute) {
                    return AccessPermission.READ_WRITE_EXECUTE;
                } else if (canRead && canWrite) {
                    return AccessPermission.READ_WRITE;
                } else if (canRead && canExecute) {
                    return AccessPermission.READ_EXECUTE;
                } else if (canRead) {
                    return AccessPermission.READ;
                } else if (canExecute) {
                    return AccessPermission.EXECUTE;
                } else {
                    return AccessPermission.NONE;
                }
            }
        } catch (Exception e) {
            log.error("获取内存权限失败: pid={}, 地址={}", pid, address.getValue(), e);
        }
        
        // 默认返回无权限
        return AccessPermission.NONE;
    }
    
    /**
     * 检查内存读取权限
     * @param pid 进程ID
     * @param address 虚拟地址
     * @return 是否有读取权限
     * @throws MemoryProtectionException 内存保护异常
     */
    public boolean checkReadPermission(int pid, VirtualAddress address) throws MemoryProtectionException {
        AccessPermission permission = getPermission(pid, address);
        
        if (!permission.canRead()) {
            throw new MemoryProtectionException(
                    String.format("读取内存权限不足: pid=%d, 地址=%d", 
                                 pid, address.getValue()),
                    pid,
                    address.getValue(),
                    false, true, false);
        }
        
        return true;
    }
    
    /**
     * 检查内存写入权限
     * @param pid 进程ID
     * @param address 虚拟地址
     * @return 是否有写入权限
     * @throws MemoryProtectionException 内存保护异常
     */
    public boolean checkWritePermission(int pid, VirtualAddress address) throws MemoryProtectionException {
        AccessPermission permission = getPermission(pid, address);
        
        if (!permission.canWrite()) {
            throw new MemoryProtectionException(
                    String.format("写入内存权限不足: pid=%d, 地址=%d", 
                                 pid, address.getValue()),
                    pid,
                    address.getValue(),
                    false, false, true);
        }
        
        return true;
    }
    
    /**
     * 检查内存执行权限
     * @param pid 进程ID
     * @param address 虚拟地址
     * @return 是否有执行权限
     * @throws MemoryProtectionException 内存保护异常
     */
    public boolean checkExecutePermission(int pid, VirtualAddress address) throws MemoryProtectionException {
        AccessPermission permission = getPermission(pid, address);
        
        if (!permission.canExecute()) {
            throw new MemoryProtectionException(
                    String.format("执行内存权限不足: pid=%d, 地址=%d", 
                                 pid, address.getValue()),
                    pid,
                    address.getValue(),
                    true, false, false);
        }
        
        return true;
    }
    
    /**
     * 实现MemoryProtection接口的checkAccess方法
     */
    @Override
    public boolean checkAccess(int processId, VirtualAddress virtualAddress, boolean isWrite) 
            throws MemoryProtectionException {
        if (isWrite) {
            return checkWritePermission(processId, virtualAddress);
        } else {
            return checkReadPermission(processId, virtualAddress);
        }
    }
    
    /**
     * 实现MemoryProtection接口的checkReadAccess方法
     */
    @Override
    public boolean checkReadAccess(int processId, VirtualAddress virtualAddress, long size) 
            throws MemoryProtectionException {
        // 检查范围内的所有页面的读权限
        long endAddress = virtualAddress.getValue() + size - 1;
        for (long addr = virtualAddress.getValue(); addr <= endAddress; addr += Page.PAGE_SIZE) {
            VirtualAddress currentAddr = new VirtualAddress(addr);
            checkReadPermission(processId, currentAddr);
        }
        return true;
    }
    
    /**
     * 实现MemoryProtection接口的checkWriteAccess方法
     */
    @Override
    public boolean checkWriteAccess(int processId, VirtualAddress virtualAddress, long size) 
            throws MemoryProtectionException {
        // 检查范围内的所有页面的写权限
        long endAddress = virtualAddress.getValue() + size - 1;
        for (long addr = virtualAddress.getValue(); addr <= endAddress; addr += Page.PAGE_SIZE) {
            VirtualAddress currentAddr = new VirtualAddress(addr);
            checkWritePermission(processId, currentAddr);
        }
        return true;
    }
    
    /**
     * 实现MemoryProtection接口的checkExecuteAccess方法
     */
    @Override
    public boolean checkExecuteAccess(int processId, VirtualAddress virtualAddress, long size) 
            throws MemoryProtectionException {
        // 检查范围内的所有页面的执行权限
        long endAddress = virtualAddress.getValue() + size - 1;
        for (long addr = virtualAddress.getValue(); addr <= endAddress; addr += Page.PAGE_SIZE) {
            VirtualAddress currentAddr = new VirtualAddress(addr);
            checkExecutePermission(processId, currentAddr);
        }
        return true;
    }
    
    /**
     * 实现MemoryProtection接口的setAccessControl方法
     */
    @Override
    public void setAccessControl(int processId, VirtualAddress virtualAddress, long size,
                               boolean canRead, boolean canWrite, boolean canExecute) 
            throws MemoryProtectionException {
        AccessPermission permission;
        if (canRead && canWrite && canExecute) {
            permission = AccessPermission.READ_WRITE_EXECUTE;
        } else if (canRead && canWrite) {
            permission = AccessPermission.READ_WRITE;
        } else if (canRead && canExecute) {
            permission = AccessPermission.READ_EXECUTE;
        } else if (canRead) {
            permission = AccessPermission.READ;
        } else if (canExecute) {
            permission = AccessPermission.EXECUTE;
        } else {
            permission = AccessPermission.NONE;
        }
        
        if (!setPermission(processId, virtualAddress, size, permission)) {
            throw new MemoryProtectionException(
                    String.format("设置内存权限失败: pid=%d, 地址=%d, 大小=%d", 
                                 processId, virtualAddress.getValue(), size),
                    processId,
                    virtualAddress.getValue(),
                    false, false, false);
        }
    }
    
    /**
     * 实现MemoryProtection接口的removeAccessControl方法
     */
    @Override
    public void removeAccessControl(int processId, VirtualAddress virtualAddress, long size) 
            throws MemoryProtectionException {
        // 获取进程的保护区域映射
        Map<Long, ProtectionRegion> processRegions = protectionRegionMap.get(processId);
        if (processRegions != null) {
            // 移除对应的保护区域
            processRegions.remove(virtualAddress.getValue());
            
            // 重置页表中相应页面的权限为默认权限
            try {
                int pageSize = Page.PAGE_SIZE;
                long startPage = virtualAddress.getValue() / pageSize;
                long endPage = (virtualAddress.getValue() + size - 1) / pageSize;
                
                for (long pageNum = startPage; pageNum <= endPage; pageNum++) {
                    // 计算当前页的虚拟地址
                    VirtualAddress pageAddr = new VirtualAddress((int) pageNum * pageSize);
                    
                    // 获取页面
                    Page page = pageTable.getPage(pageAddr, processId);
                    
                    if (page != null) {
                        // 设置页面为默认权限（只读）
                        page.setProtection(false, false);
                    }
                }
                
                log.debug("已移除进程{}的内存权限: 起始地址={}, 长度={}", 
                         processId, virtualAddress.getValue(), size);
            } catch (Exception e) {
                log.error("移除内存权限失败: pid={}, 起始地址={}, 长度={}", 
                         processId, virtualAddress.getValue(), size, e);
                throw new MemoryProtectionException(
                        String.format("移除内存权限失败: pid=%d, 地址=%d, 大小=%d", 
                                     processId, virtualAddress.getValue(), size),
                        processId,
                        virtualAddress.getValue(),
                        false, false, false);
            }
        }
    }
    
    /**
     * 实现MemoryProtection接口的changeAccessPermission方法
     */
    @Override
    public void changeAccessPermission(int processId, VirtualAddress virtualAddress, long size,
                                     Boolean setRead, Boolean setWrite, Boolean setExecute) 
            throws MemoryProtectionException {
        // 获取当前权限
        AccessPermission currentPermission = getPermission(processId, virtualAddress);
        
        // 计算新权限
        boolean newRead = (setRead != null) ? setRead : currentPermission.canRead();
        boolean newWrite = (setWrite != null) ? setWrite : currentPermission.canWrite();
        boolean newExecute = (setExecute != null) ? setExecute : currentPermission.canExecute();
        
        // 设置新权限
        setAccessControl(processId, virtualAddress, size, newRead, newWrite, newExecute);
    }
    
    /**
     * 为两个进程之间添加共享内存区域
     * @param ownerPid 内存拥有者进程ID
     * @param targetPid 目标进程ID
     * @param startAddress 起始地址
     * @param size 大小
     * @param permission 访问权限
     * @return 是否成功
     * @throws MemoryProtectionException 内存保护异常
     */
    public boolean addSharedMemoryRegion(int ownerPid, int targetPid, VirtualAddress startAddress,
                                      long size, AccessPermission permission) throws MemoryProtectionException {
        // 确保参数有效
        if (ownerPid < 0 || targetPid < 0 || startAddress == null || size <= 0) {
            throw new MemoryProtectionException("参数无效", -1, 0L, false, false, false);
        }
        
        // 只有进程自己或系统进程可以共享内存
        if (ownerPid != processId && processId != 0) {
            throw new MemoryProtectionException(
                String.format("进程%d无权共享进程%d的内存", processId, ownerPid),
                processId, startAddress.getValue(), false, false, false);
        }
        
        log.info("添加共享内存区域: 拥有者进程={}, 目标进程={}, 地址=0x{}, 大小={}",
            ownerPid, targetPid, Long.toHexString(startAddress.getValue()), size);
            
        // 创建共享内存区域
        SharedMemoryRegion region = new SharedMemoryRegion(ownerPid, targetPid, startAddress, size, permission);
        String key = generateSharedMemoryKey(ownerPid, targetPid, startAddress);
        sharedMemoryRegions.put(key, region);
        
        return true;
    }
    
    /**
     * 撤销共享内存区域
     * @param ownerPid 内存拥有者进程ID
     * @param targetPid 目标进程ID
     * @param startAddress 起始地址
     * @return 是否成功
     * @throws MemoryProtectionException 内存保护异常
     */
    public boolean revokeSharedMemoryRegion(int ownerPid, int targetPid, VirtualAddress startAddress) 
            throws MemoryProtectionException {
        // 确保参数有效
        if (ownerPid < 0 || targetPid < 0 || startAddress == null) {
            throw new MemoryProtectionException("参数无效", -1, 0L, false, false, false);
        }
        
        // 只有进程自己或系统进程可以撤销共享内存
        if (ownerPid != processId && processId != 0) {
            throw new MemoryProtectionException(
                String.format("进程%d无权撤销进程%d的共享内存", processId, ownerPid),
                processId, startAddress.getValue(), false, false, false);
        }
        
        String key = generateSharedMemoryKey(ownerPid, targetPid, startAddress);
        SharedMemoryRegion region = sharedMemoryRegions.get(key);
        
        // 检查共享内存区域是否存在
        checkSharedMemoryPermission(region);
        
        log.info("撤销共享内存区域: 拥有者进程={}, 目标进程={}, 地址=0x{}",
            ownerPid, targetPid, Long.toHexString(startAddress.getValue()));
            
        sharedMemoryRegions.remove(key);
        return true;
    }
    
    /**
     * 将内存区域设置为只读
     * @param processId 进程ID
     * @param startAddress 起始地址
     * @param size 大小
     * @return 是否成功
     * @throws MemoryProtectionException 内存保护异常
     */
    public boolean setReadOnly(int processId, VirtualAddress startAddress, long size) 
            throws MemoryProtectionException {
        // 确保参数有效
        if (processId < 0 || startAddress == null || size <= 0) {
            throw new MemoryProtectionException("参数无效", -1, 0L, false, false, false);
        }
        
        // 只有进程自己或系统进程可以修改保护
        if (processId != this.processId && this.processId != 0) {
            throw new MemoryProtectionException(
                String.format("进程%d无权修改进程%d的内存保护", this.processId, processId),
                this.processId, startAddress.getValue(), false, false, false);
        }
        
        log.info("设置内存区域为只读: 进程={}, 地址=0x{}, 大小={}",
            processId, Long.toHexString(startAddress.getValue()), size);
            
        VirtualAddress currentAddress = new VirtualAddress(startAddress.getValue());
        VirtualAddress endAddress = new VirtualAddress(startAddress.getValue() + size - 1);
        
        while (currentAddress.getValue() <= endAddress.getValue()) {
            try {
                Page page = pageTable.getPage(currentAddress, processId);
                if (page != null) {
                    page.setProtection(true, false, false);
                }
                
                // 移动到下一页
                currentAddress = new VirtualAddress(
                    (currentAddress.getValue() & ~Page.PAGE_SIZE) + Page.PAGE_SIZE);
            } catch (Exception e) {
                log.error("设置内存区域只读失败", e);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 将内存区域设置为可写
     * @param processId 进程ID
     * @param startAddress 起始地址
     * @param size 大小
     * @return 是否成功
     * @throws MemoryProtectionException 内存保护异常
     */
    public boolean setWritable(int processId, VirtualAddress startAddress, long size) 
            throws MemoryProtectionException {
        // 确保参数有效
        if (processId < 0 || startAddress == null || size <= 0) {
            throw new MemoryProtectionException("参数无效", -1, 0L, false, false, false);
        }
        
        // 只有进程自己或系统进程可以修改保护
        if (processId != this.processId && this.processId != 0) {
            throw new MemoryProtectionException(
                String.format("进程%d无权修改进程%d的内存保护", this.processId, processId),
                this.processId, startAddress.getValue(), false, false, false);
        }
        
        log.info("设置内存区域为可写: 进程={}, 地址=0x{}, 大小={}",
            processId, Long.toHexString(startAddress.getValue()), size);
            
        VirtualAddress currentAddress = new VirtualAddress(startAddress.getValue());
        VirtualAddress endAddress = new VirtualAddress(startAddress.getValue() + size - 1);
        
        while (currentAddress.getValue() <= endAddress.getValue()) {
            try {
                Page page = pageTable.getPage(currentAddress, processId);
                if (page != null) {
                    page.setProtection(true, true, false);
                }
                
                // 移动到下一页
                currentAddress = new VirtualAddress(
                    (currentAddress.getValue() & ~Page.PAGE_SIZE) + Page.PAGE_SIZE);
            } catch (Exception e) {
                log.error("设置内存区域可写失败", e);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 锁定内存区域，防止被换出
     * @param processId 进程ID
     * @param startAddress 起始地址
     * @param size 大小
     * @return 是否成功
     * @throws MemoryProtectionException 内存保护异常
     */
    public boolean lockMemory(int processId, VirtualAddress startAddress, long size) 
            throws MemoryProtectionException {
        // 确保参数有效
        if (processId < 0 || startAddress == null || size <= 0) {
            throw new MemoryProtectionException("参数无效", -1, 0L, false, false, false);
        }
        
        // 只有系统进程可以锁定内存
        if (this.processId != 0) {
            throw new MemoryProtectionException(
                "只有系统进程可以锁定内存", this.processId, startAddress.getValue(), 
                false, false, false);
        }
        
        log.info("锁定内存区域: 进程={}, 地址=0x{}, 大小={}",
            processId, Long.toHexString(startAddress.getValue()), size);
            
        VirtualAddress currentAddress = new VirtualAddress(startAddress.getValue());
        VirtualAddress endAddress = new VirtualAddress(startAddress.getValue() + size - 1);
        
        while (currentAddress.getValue() <= endAddress.getValue()) {
            try {
                Page page = pageTable.getPage(currentAddress, processId);
                if (page != null) {
                    page.setLocked(true);
                }
                
                // 移动到下一页
                currentAddress = new VirtualAddress(
                    (currentAddress.getValue() & ~Page.PAGE_SIZE) + Page.PAGE_SIZE);
            } catch (Exception e) {
                log.error("锁定内存区域失败", e);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 解锁内存区域
     * @param processId 进程ID
     * @param startAddress 起始地址
     * @param size 大小
     * @return 是否成功
     * @throws MemoryProtectionException 内存保护异常
     */
    public boolean unlockMemory(int processId, VirtualAddress startAddress, long size) 
            throws MemoryProtectionException {
        // 确保参数有效
        if (processId < 0 || startAddress == null || size <= 0) {
            throw new MemoryProtectionException("参数无效", -1, 0L, false, false, false);
        }
        
        // 只有系统进程可以解锁内存
        if (this.processId != 0) {
            throw new MemoryProtectionException(
                "只有系统进程可以解锁内存", this.processId, startAddress.getValue(), 
                false, false, false);
        }
        
        log.info("解锁内存区域: 进程={}, 地址=0x{}, 大小={}",
            processId, Long.toHexString(startAddress.getValue()), size);
            
        VirtualAddress currentAddress = new VirtualAddress(startAddress.getValue());
        VirtualAddress endAddress = new VirtualAddress(startAddress.getValue() + size - 1);
        
        while (currentAddress.getValue() <= endAddress.getValue()) {
            try {
                Page page = pageTable.getPage(currentAddress, processId);
                if (page != null) {
                    page.setLocked(false);
                }
                
                // 移动到下一页
                currentAddress = new VirtualAddress(
                    (currentAddress.getValue() & ~Page.PAGE_SIZE) + Page.PAGE_SIZE);
            } catch (Exception e) {
                log.error("解锁内存区域失败", e);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public boolean handleProtectionFault(int pid, VirtualAddress address, 
                                       boolean isRead, boolean isWrite, boolean isExecute) {
        try {
            // 获取页面
            Page page = pageTable.getPage(address, pid);
            
            if (page == null) {
                log.error("处理内存保护异常失败: 页面不存在, pid={}, 地址={}", 
                         pid, address.getValue());
                return false;
            }
            
            // 检查访问类型和权限
            if (isRead && !page.isReadable()) {
                log.warn("内存保护异常: 读取无权限页面, pid={}, 地址={}", 
                        pid, address.getValue());
                return false;
            }
            
            if (isWrite && !page.isWritable()) {
                // Copy-on-Write 情况的特殊处理
                if (page.isShared() && page.isReadable()) {
                    // 复制页面内容到新页面
                    boolean success = pageTable.copyOnWrite(pid, address);
                    
                    if (success) {
                        log.info("Copy-on-Write处理成功: pid={}, 地址={}", 
                                pid, address.getValue());
                        return true;
                    }
                }
                
                log.warn("内存保护异常: 写入只读页面, pid={}, 地址={}", 
                        pid, address.getValue());
                return false;
            }
            
            if (isExecute && !page.isExecutable()) {
                log.warn("内存保护异常: 执行无执行权限页面, pid={}, 地址={}", 
                        pid, address.getValue());
                return false;
            }
            
            log.warn("无法处理的内存保护异常: pid={}, 地址={}, 读={}, 写={}, 执行={}", 
                    pid, address.getValue(), isRead, isWrite, isExecute);
            
            return false;
        } catch (Exception e) {
            log.error("处理内存保护异常失败: pid={}, 地址={}, 读={}, 写={}, 执行={}", 
                     pid, address.getValue(), isRead, isWrite, isExecute, e);
            
            return false;
        }
    }
    
    /**
     * 生成共享内存区域的唯一键
     * @param ownerPid 拥有者进程ID
     * @param targetPid 目标进程ID
     * @param startAddress 起始地址
     * @return 唯一键
     */
    private String generateSharedMemoryKey(int ownerPid, int targetPid, VirtualAddress startAddress) {
        return String.format("%d-%d-%d", ownerPid, targetPid, startAddress.getValue());
    }

    /**
     * 检查共享内存的权限
     * @param region 共享区域
     * @throws MemoryProtectionException 如果权限检查失败
     */
    private void checkSharedMemoryPermission(SharedMemoryRegion region) throws MemoryProtectionException {
        if (region == null) {
            throw new MemoryProtectionException("共享内存区域不存在", -1, 0L, 
                false, false, false);
        }
        
        // 检查当前进程是否有权限访问该共享内存区域
        if (region.getSourceProcessId() != processId && region.getTargetProcessId() != processId) {
            throw new MemoryProtectionException(
                    String.format("进程%d无权访问进程%d和%d之间的共享内存", 
                            processId, region.getSourceProcessId(), region.getTargetProcessId()),
                    processId, region.getStartAddress().getValue(), false, false, false);
        }
    }

    /**
     * 检查保护区域是否合法
     * @param region 保护区域
     * @param operation 操作类型
     * @throws MemoryProtectionException 如果权限检查失败
     */
    private void validateProtectionRegion(ProtectionRegion region, String operation) throws MemoryProtectionException {
        if (region == null) {
            throw new MemoryProtectionException(
                    "保护区域不能为空", -1, 0L, false, false, false);
        }
        
        if (region.getStartAddress() == null || region.getEndAddress() == null) {
            throw new MemoryProtectionException(
                    "保护区域的起始或结束地址不能为空", -1, 0L, false, false, false);
        }
        
        if (region.getStartAddress().getValue() > region.getEndAddress().getValue()) {
            throw new MemoryProtectionException(
                    String.format("保护区域的起始地址(0x%X)不能大于结束地址(0x%X)",
                            region.getStartAddress().getValue(), region.getEndAddress().getValue()),
                    processId, region.getStartAddress().getValue(), false, false, false);
        }
    }

    /**
     * 记录访问违规
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param accessType 访问类型描述
     */
    @Override
    public void recordViolation(int processId, VirtualAddress virtualAddress, String accessType) {
        // 增加总违规计数
        totalViolations.incrementAndGet();
        
        // 根据访问类型增加相应计数
        if (accessType.contains("读")) {
            readViolations.incrementAndGet();
        } else if (accessType.contains("写")) {
            writeViolations.incrementAndGet();
        } else if (accessType.contains("执行")) {
            executeViolations.incrementAndGet();
        }
        
        // 创建违规记录
        ViolationRecord record = new ViolationRecord(
            System.currentTimeMillis(),
            processId,
            virtualAddress.getValue(),
            accessType
        );
        
        // 添加到违规记录列表
        violationRecords.computeIfAbsent(processId, k -> new LinkedList<>()).add(record);
        
        // 如果队列过长，移除最旧的记录
        Queue<ViolationRecord> records = violationRecords.get(processId);
        if (records.size() > 100) { // 只保留最近100条记录
            records.poll();
        }
        
        // 日志记录
        log.warn("内存访问违规: 进程 {} 尝试{}地址 0x{}, 操作类型: {}", 
                processId, accessType, Long.toHexString(virtualAddress.getValue()), accessType);
    }
    
    /**
     * 获取统计信息
     * @return 保护机制统计信息
     */
    @Override
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("内存保护统计信息:\n");
        stats.append("总违规次数: ").append(totalViolations.get()).append("\n");
        stats.append("读取违规: ").append(readViolations.get()).append("\n");
        stats.append("写入违规: ").append(writeViolations.get()).append("\n");
        stats.append("执行违规: ").append(executeViolations.get()).append("\n");
        
        // 添加进程级别的统计信息
        stats.append("\n进程级别违规统计:\n");
        for (Map.Entry<Integer, Queue<ViolationRecord>> entry : violationRecords.entrySet()) {
            int pid = entry.getKey();
            int count = entry.getValue().size();
            stats.append("进程 ").append(pid).append(": ").append(count).append(" 次违规\n");
        }
        
        // 添加保护区域统计
        stats.append("\n保护区域统计:\n");
        int totalRegions = 0;
        for (Map<Long, ProtectionRegion> regions : protectionRegionMap.values()) {
            totalRegions += regions.size();
        }
        stats.append("总保护区域数: ").append(totalRegions).append("\n");
        stats.append("共享内存区域数: ").append(sharedMemoryRegions.size()).append("\n");
        
        return stats.toString();
    }
    
    /**
     * 重置统计信息
     */
    @Override
    public void resetStatistics() {
        // 重置所有计数器
        totalViolations.set(0);
        readViolations.set(0);
        writeViolations.set(0);
        executeViolations.set(0);
        
        // 清空所有违规记录
        violationRecords.clear();
        
        log.info("内存保护统计信息已重置");
    }
    
    /**
     * 违规记录内部类
     */
    private static class ViolationRecord {
        private final long timestamp;
        private final int processId;
        private final long address;
        private final String accessType;
        
        public ViolationRecord(long timestamp, int processId, long address, String accessType) {
            this.timestamp = timestamp;
            this.processId = processId;
            this.address = address;
            this.accessType = accessType;
        }
    }
} 