package newOs.kernel.interrupt;

import newOs.common.InterruptConstant.InterruptType;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.InfoImplDTO.InterruptContextDTO;

/**
 * 硬件中断处理器接口
 * 处理硬件级别的中断，如页错误、保护错误等
 */
public interface HardwareInterruptHandler {
    
    /**
     * 获取中断处理器处理的中断类型
     * @return 中断类型
     */
    InterruptType getType();
    
    /**
     * 处理中断信息
     * @param info 中断信息
     * @return 处理后的中断信息
     */
    InterruptInfo handle(InterruptInfo info);
} 