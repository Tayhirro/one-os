package newOs.kernel.interrupt;

import newOs.common.InterruptConstant.InterruptType;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.InterruptSysCallInfo;
import newOs.dto.req.Info.MemoryInterruptInfo;
import newOs.dto.req.Info.TimerInfo;
import newOs.dto.req.Info.TimerInfoImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import newOs.kernel.interrupt.hardwareHandler.PageFaultHandler;
import newOs.kernel.interrupt.hardwareHandler.MemoryProtectionHandler;
import newOs.kernel.memory.model.VirtualAddress;

import java.util.concurrent.ConcurrentHashMap;

import static newOs.common.InterruptConstant.InterruptType.SYSTEM_CALL;
import static newOs.common.InterruptConstant.InterruptType.TIMER;
import static newOs.common.InterruptConstant.InterruptType.PAGE_FAULT;
import static newOs.common.InterruptConstant.InterruptType.GENERAL_PROTECTION_FAULT;

// InterruptController.java
@Component
public class InterruptController {

    private final ConcurrentHashMap<InterruptType, ISR<? extends InterruptInfo>> IDT; // 中断描述符表

    private PageFaultHandler pageFaultHandler;
    
    private MemoryProtectionHandler memoryProtectionHandler;

    @Autowired
    public InterruptController(ProtectedMemory protectedMemory,PageFaultHandler pageFaultHandler,MemoryProtectionHandler memoryProtectionHandler) {
        IDT = protectedMemory.getIDT();
        this.pageFaultHandler = pageFaultHandler;
        this.memoryProtectionHandler = memoryProtectionHandler;
        if(IDT != null){
            registerMemoryInterruptHandlers();
            registerTimerInterruptHandler();
        }
    }
    
    /**
     * 注册定时器中断处理程序
     */
    private void registerTimerInterruptHandler() {
        // 注册TIMER中断处理程序
        if (!IDT.containsKey(TIMER)) {
            IDT.put(TIMER, new ISR<TimerInfo>() {
                @Override
                public InterruptInfo execute(TimerInfo info) {
                    // 如果传入的是TimerInfoImpl，则可以设置时间戳
                    if (info instanceof TimerInfoImpl) {
                        TimerInfoImpl timerImpl = (TimerInfoImpl) info;
                        timerImpl.setTimestamp(System.currentTimeMillis());
                        System.out.println("时钟中断触发：" + timerImpl.getTimestamp());
                    } else {
                        System.out.println("时钟中断触发，但信息类型不是TimerInfoImpl");
                    }
                    return info;
                }
            });
        }
    }


    public InterruptSysCallInfo triggerSystemCall(InterruptSysCallInfo sysCallInfo) {    //对系统中断的处理
        //默认是0x80，直接调用SytemCallHandler
        ISR<InterruptSysCallInfo> handler = (ISR<InterruptSysCallInfo>) IDT.get(SYSTEM_CALL);
        InterruptInfo interruptInfo = handler.execute(sysCallInfo);
        return (InterruptSysCallInfo) interruptInfo;
    }


    public void triggerTimer(TimerInfo timerInfo) {
        //默认是0x20，直接调用TimerHandler
        ISR<TimerInfo> handler = (ISR<TimerInfo>) IDT.get(TIMER);
        handler.execute(timerInfo);
    }

    //返回信息trigger

    public void trigger(DeviceInfoReturnImplDTO deviceInfoReturnImplDTO) {
        ISR<DeviceInfoReturnImplDTO> handler = (ISR<DeviceInfoReturnImplDTO>) IDT.get(InterruptType.IO_INTERRUPT);
        handler.execute(deviceInfoReturnImplDTO);
    }



    private void registerMemoryInterruptHandlers() {
        // 注册缺页中断处理程序
        if (!IDT.containsKey(PAGE_FAULT)) {
            IDT.put(PAGE_FAULT, new ISR<MemoryInterruptInfo>() {
                @Override
                public InterruptInfo execute(MemoryInterruptInfo info) {
                    return pageFaultHandler.handle(info);
                }
            });
        }
        
        // 注册内存保护违规中断处理程序
        if (!IDT.containsKey(GENERAL_PROTECTION_FAULT)) {
            IDT.put(GENERAL_PROTECTION_FAULT, new ISR<MemoryInterruptInfo>() {
                @Override
                public InterruptInfo execute(MemoryInterruptInfo info) {
                    return memoryProtectionHandler.handle(info);
                }
            });
        }
        
        // 注册TLB缺失中断处理程序 - 合并到页错误处理
        if (!IDT.containsKey(PAGE_FAULT)) {
            IDT.put(PAGE_FAULT, new ISR<MemoryInterruptInfo>() {
                @Override
                public InterruptInfo execute(MemoryInterruptInfo info) {
                    // 对于TLB缺失，可能只是更新TLB，然后重新执行指令
                    // 在这里简单地返回原信息，实际处理应该在PageFaultHandler中完成
                    return info;
                }
            });
        }
    }
    
    @SuppressWarnings("unchecked")
    public InterruptInfo triggerPageFault(VirtualAddress virtualAddress, int processId) {
        // 创建内存中断信息对象
        MemoryInterruptInfo info = new MemoryInterruptInfo();
        info.setAddress(virtualAddress.toString());
        info.setProcessId(processId);
        info.setType("PAGE_FAULT");
        info.setMessage("页面不存在");
        info.setInterruptType(InterruptType.PAGE_FAULT);
        
        ISR<MemoryInterruptInfo> handler = (ISR<MemoryInterruptInfo>) IDT.get(PAGE_FAULT);
        return handler.execute(info);
    }
    
    @SuppressWarnings("unchecked")
    public InterruptInfo triggerProtectionFault(VirtualAddress virtualAddress, int processId, String accessType) {
        // 创建内存中断信息对象
        MemoryInterruptInfo info = new MemoryInterruptInfo();
        info.setAddress(virtualAddress.toString());
        info.setProcessId(processId);
        info.setType("PROTECTION_FAULT");
        info.setMessage("内存访问权限不足: " + accessType);
        info.setAdditionalInfo("accessType", accessType);
        info.setInterruptType(InterruptType.GENERAL_PROTECTION_FAULT);
        
        ISR<MemoryInterruptInfo> handler = (ISR<MemoryInterruptInfo>) IDT.get(GENERAL_PROTECTION_FAULT);
        return handler.execute(info);
    }
    
    @SuppressWarnings("unchecked")
    public InterruptInfo triggerTLBMiss(VirtualAddress virtualAddress, int processId) {
        // 创建内存中断信息对象
        MemoryInterruptInfo info = new MemoryInterruptInfo();
        info.setAddress(virtualAddress.toString());
        info.setProcessId(processId);
        info.setType("TLB_MISS");
        info.setMessage("TLB缺失");
        info.setInterruptType(InterruptType.PAGE_FAULT); // TLB缺失最终会导致页错误
        
        ISR<MemoryInterruptInfo> handler = (ISR<MemoryInterruptInfo>) IDT.get(PAGE_FAULT);
        return handler.execute(info);
    }
    
    /**
     * 触发段错误中断
     * @param processId 进程ID
     * @param virtualAddress 导致段错误的虚拟地址
     * @return 中断处理结果
     */
    @SuppressWarnings("unchecked")
    public InterruptInfo triggerSegmentationFault(int processId, VirtualAddress virtualAddress) {
        // 创建内存中断信息对象
        MemoryInterruptInfo info = new MemoryInterruptInfo();
        info.setAddress(virtualAddress.toString());
        info.setProcessId(processId);
        info.setType("SEGMENTATION_FAULT");
        info.setMessage("段错误：访问无效地址");
        info.setInterruptType(InterruptType.GENERAL_PROTECTION_FAULT);
        
        ISR<MemoryInterruptInfo> handler = (ISR<MemoryInterruptInfo>) IDT.get(GENERAL_PROTECTION_FAULT);
        return handler.execute(info);
    }
    
    /**
     * 触发一般中断
     * @param interruptType 中断类型
     * @param info 中断信息
     * @return 中断处理结果
     */
    @SuppressWarnings("unchecked")
    public InterruptInfo triggerInterrupt(InterruptType interruptType, InterruptInfo info) {
        ISR<InterruptInfo> handler = (ISR<InterruptInfo>) IDT.get(interruptType);
        if (handler == null) {
            throw new RuntimeException("未找到中断处理程序：" + interruptType);
        }
        return handler.execute(info);
    }
}
