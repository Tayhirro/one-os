package newOs.dto.req.Info;

import lombok.Data;
import newOs.common.InterruptConstant.InterruptType;

/**
 * 定时器中断信息实现类
 */
@Data
public class TimerInfoImpl implements TimerInfo {
    
    private InterruptType interruptType = InterruptType.TIMER;
    private long timestamp;
    
    @Override
    public InterruptType getInterruptType() {
        return interruptType;
    }
} 