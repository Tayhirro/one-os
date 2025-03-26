package newOs.kernel.interrupt.hardwareHandler;

import lombok.extern.slf4j.Slf4j;
import newOs.common.InterruptConstant.InterruptType;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.MemoryInterruptInfo;
import newOs.dto.req.Info.InfoImplDTO.InterruptContextDTO;
import newOs.exception.MemoryProtectionException;
import newOs.kernel.interrupt.HardwareInterruptHandler;
import newOs.kernel.interrupt.ISR;
import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.virtual.protection.MemoryProtection;
import newOs.kernel.process.ProcessManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 内存保护中断处理器
 * 处理内存访问权限违规引起的中断
 */
@Component
@Slf4j
public class MemoryProtectionHandler implements HardwareInterruptHandler, ISR<MemoryInterruptInfo> {

    /**
     * 内存管理器
     */
    private final MemoryManager memoryManager;
    
    /**
     * 进程管理器
     */
    private final ProcessManager processManager;
    
    /**
     * 内存保护机制
     */
    private final MemoryProtection memoryProtection;
    
    /**
     * 构造函数
     * @param memoryManager 内存管理器
     * @param processManager 进程管理器
     * @param memoryProtection 内存保护机制
     */
    @Autowired
    public MemoryProtectionHandler(MemoryManager memoryManager,
                                 ProcessManager processManager,
                                 MemoryProtection memoryProtection) {
        this.memoryManager = memoryManager;
        this.processManager = processManager;
        this.memoryProtection = memoryProtection;
        log.info("内存保护中断处理器初始化完成");
    }

    @Override
    public InterruptType getType() {
        return InterruptType.GENERAL_PROTECTION_FAULT;
    }

    @Override
    public InterruptInfo handle(InterruptInfo info) {
        if (info == null) {
            log.error("中断信息为空，无法处理内存保护中断");
            return null;
        }
        
        if (info instanceof MemoryInterruptInfo) {
            MemoryInterruptInfo memoryInfo = (MemoryInterruptInfo) info;
            
            try {
                int pid = memoryInfo.getProcessId();
                String addressStr = memoryInfo.getAddress();
                VirtualAddress virtualAddress = memoryInfo.getVirtualAddress();
                if (virtualAddress == null) {
                    long address = Long.parseLong(addressStr.replace("0x", ""), 16);
                    virtualAddress = new VirtualAddress((int)address);
                }
                
                // 获取访问类型
                String accessType = (String) memoryInfo.getAdditionalInfo("accessType");
                boolean isRead = "READ".equals(accessType);
                boolean isWrite = "WRITE".equals(accessType);
                boolean isExecute = "EXECUTE".equals(accessType);
                
                log.debug("处理内存保护中断: 进程={}, 虚拟地址={}, 读访问={}, 写访问={}, 执行访问={}",
                        pid, virtualAddress, isRead, isWrite, isExecute);
                
                // 尝试处理保护异常
                boolean success = memoryProtection.handleProtectionFault(
                    pid, virtualAddress, isRead, isWrite, isExecute);
                
                if (success) {
                    log.debug("已处理内存保护异常: 进程={}, 虚拟地址={}", pid, virtualAddress);
                    memoryInfo.setAdditionalInfo("success", true);
                    return memoryInfo;
                }
                
                // 如果无法处理，向进程发送段错误信号
                log.debug("内存保护异常无法处理，发送段错误信号: 进程={}, 虚拟地址={}", pid, virtualAddress);
                try {
                    processManager.getClass().getMethod("sendSignal", int.class, String.class, String.class)
                        .invoke(processManager, pid, "SIGSEGV", "内存访问权限不足");
                } catch (Exception e) {
                    log.error("无法发送段错误信号: {}", e.getMessage());
                    // 如果sendSignal方法不存在，使用其他方式通知进程
                }
                
                memoryInfo.setAdditionalInfo("success", false);
                memoryInfo.setAdditionalInfo("error", "内存访问权限不足");
                return memoryInfo;
            } catch (Exception e) {
                log.error("处理内存保护中断时发生异常: {}", e.getMessage(), e);
                memoryInfo.setAdditionalInfo("error", e.getMessage());
                memoryInfo.setAdditionalInfo("success", false);
                return memoryInfo;
            }
        } else if (info instanceof InterruptContextDTO) {
            InterruptContextDTO contextDTO = (InterruptContextDTO) info;
            
            try {
                // 从中断上下文中获取信息
                int pid = contextDTO.getProcessId();
                long faultAddress = contextDTO.getFaultAddress();
                boolean isWrite = contextDTO.isWriteAccess();
                boolean isExecute = contextDTO.isExecuteAccess();
                boolean isRead = !isWrite && !isExecute;
                
                VirtualAddress virtualAddress = contextDTO.getVirtualAddress();
                if (virtualAddress == null) {
                    virtualAddress = new VirtualAddress((int)faultAddress);
                }
                
                log.debug("处理内存保护中断: 进程={}, 虚拟地址={}, 写访问={}, 执行访问={}",
                        pid, faultAddress, isWrite, isExecute);
                
                // 尝试处理保护异常
                boolean success = memoryProtection.handleProtectionFault(
                    pid, virtualAddress, isRead, isWrite, isExecute);
                
                if (success) {
                    log.debug("已处理内存保护异常: 进程={}, 虚拟地址={}", pid, faultAddress);
                    contextDTO.setAdditionalInfo(true);
                    return contextDTO;
                }
                
                // 如果无法处理，向进程发送段错误信号
                log.debug("内存保护异常无法处理，发送段错误信号: 进程={}, 虚拟地址={}", pid, faultAddress);
                try {
                    processManager.getClass().getMethod("sendSignal", int.class, String.class, String.class)
                        .invoke(processManager, pid, "SIGSEGV", "内存访问权限不足");
                } catch (Exception e) {
                    log.error("无法发送段错误信号: {}", e.getMessage());
                }
                
                contextDTO.setAdditionalInfo(false);
                return contextDTO;
            } catch (Exception e) {
                log.error("处理内存保护中断时发生异常: {}", e.getMessage(), e);
                return contextDTO;
            }
        }
        
        return info; // 如果类型不匹配，直接返回原信息
    }

    @Override
    public InterruptInfo execute(MemoryInterruptInfo info) {
        return handle(info);
    }
} 