package newOs.kernel.interrupt.hardwareHandler;

import lombok.extern.slf4j.Slf4j;
import newOs.common.InterruptConstant.InterruptType;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.MemoryInterruptInfo;
import newOs.exception.MemoryProtectionException;
import newOs.kernel.interrupt.HardwareInterruptHandler;
import newOs.kernel.interrupt.InterruptContext;
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
public class MemoryProtectionHandler implements HardwareInterruptHandler {

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
    public boolean handle(InterruptContext context) {
        if (context == null) {
            log.error("中断上下文为空，无法处理内存保护中断");
            return false;
        }
        
        try {
            // 从中断上下文中获取信息
            int pid = context.getProcessId();
            long faultAddress = context.getFaultAddress();
            boolean isWrite = context.isWriteAccess();
            boolean isExecute = context.isExecuteAccess();
            
            VirtualAddress virtualAddress = new VirtualAddress(faultAddress);
            
            log.debug("处理内存保护中断: 进程={}, 虚拟地址={}, 写访问={}, 执行访问={}",
                    pid, faultAddress, isWrite, isExecute);
            
            // 尝试处理保护异常
            boolean success = memoryProtection.handleProtectionFault(
                pid, virtualAddress, !isWrite && !isExecute, isWrite, isExecute);
            
            if (success) {
                log.debug("已处理内存保护异常: 进程={}, 虚拟地址={}", pid, faultAddress);
                return true;
            }
            
            // 如果无法处理，向进程发送段错误信号
            log.debug("内存保护异常无法处理，发送段错误信号: 进程={}, 虚拟地址={}", pid, faultAddress);
            // 使用反射确保ProcessManager有这个方法
            try {
                processManager.getClass().getMethod("sendSignal", int.class, String.class, String.class)
                    .invoke(processManager, pid, "SIGSEGV", "内存访问权限不足");
            } catch (Exception e) {
                log.error("无法发送段错误信号: {}", e.getMessage());
                // 如果sendSignal方法不存在，使用其他方式通知进程
            }
            
            return true;
        } catch (Exception e) {
            log.error("处理内存保护中断时发生异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public InterruptInfo handle(InterruptInfo info) {
        if (info instanceof MemoryInterruptInfo) {
            MemoryInterruptInfo memoryInfo = (MemoryInterruptInfo) info;
            
            try {
                int pid = memoryInfo.getProcessId();
                String addressStr = memoryInfo.getAddress();
                long address = Long.parseLong(addressStr.replace("0x", ""), 16);
                
                // 获取访问类型
                String accessType = (String) memoryInfo.getAdditionalInfo("accessType");
                boolean isWrite = "WRITE".equals(accessType);
                boolean isExecute = "EXECUTE".equals(accessType);
                
                // 创建中断上下文
                InterruptContext context = new InterruptContext();
                context.setProcessId(pid);
                context.setFaultAddress(address);
                context.setWriteAccess(isWrite);
                context.setExecuteAccess(isExecute);
                context.setInterruptType(newOs.kernel.interrupt.InterruptType.GENERAL_PROTECTION);
                
                // 调用上下文处理方法
                boolean success = handle(context);
                
                // 设置处理结果
                memoryInfo.setAdditionalInfo("success", success);
                
                return memoryInfo;
            } catch (Exception e) {
                log.error("处理内存保护中断信息时发生异常: {}", e.getMessage(), e);
                memoryInfo.setAdditionalInfo("error", e.getMessage());
                return memoryInfo;
            }
        }
        
        return info; // 如果类型不匹配，直接返回原信息
    }
} 