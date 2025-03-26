package newOs.component.timer;


import newOs.dto.req.Info.InfoImplDTO.TimerInfoImplDTO;
import newOs.kernel.interrupt.InterruptController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static newOs.common.InterruptConstant.InterruptType.TIMER;

@Component
public class timer {
    private final InterruptController interruptController;


    @Autowired
    public timer(InterruptController interruptController) {
        this.interruptController = interruptController;

    }
    @Scheduled(fixedRate = 600) // 每隔600ms执行一次
    public void sendInterruptRequest() {
        TimerInfoImplDTO timerInfoImplDTO = new TimerInfoImplDTO().setTimerId(1).setInterruptType(TIMER);
        interruptController.triggerTimer(timerInfoImplDTO);
    }
}
