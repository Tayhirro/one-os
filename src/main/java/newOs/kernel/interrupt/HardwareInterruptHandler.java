                                                                                                                        package newOs.kernel.interrupt;

import newOs.common.InterruptConstant.InterruptType;
import newOs.dto.req.Info.InterruptInfo;

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
     * 处理中断
     * @param context 中断上下文
     * @return 处理是否成功
     */
    boolean handle(InterruptContext context);
    
    /**
     * 处理中断信息
     * @param info 中断信息
     * @return 处理后的中断信息
     */
    default InterruptInfo handle(InterruptInfo info) {
        // 默认实现，子类可以覆盖
        return info;
    }
} 