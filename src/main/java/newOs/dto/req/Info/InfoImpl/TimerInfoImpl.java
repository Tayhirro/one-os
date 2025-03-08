package newOs.dto.req.Info.InfoImpl;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import newOs.common.InterruptConstant.InterruptType;
import newOs.dto.req.Info.TimerInfo;

@Data
@Accessors(chain = true)
public class TimerInfoImpl implements TimerInfo {
    //private final long timestamp; // 触发时间戳
    private int timerId; // 定时器 ID
    //private final boolean periodic; // 是否是周期性定时器
    private InterruptType interruptType;
    @Override
    public InterruptType getInterruptType() {
        return interruptType;
    }

}
