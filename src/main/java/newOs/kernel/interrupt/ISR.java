package newOs.kernel.interrupt;


import newOs.dto.req.Info.InterruptInfo;

// InterruptServiceRoutine

public interface ISR<T extends InterruptInfo> {
    InterruptInfo execute(T interruptInfo);
}