package newOs.kernel.interrupt;

import newOs.dto.req.Info.InterruptInfo;

public interface IRQHandler extends ISR<InterruptInfo> {
    //中断处理程序
    InterruptInfo execute(InterruptInfo interruptDeviceInfo);
}