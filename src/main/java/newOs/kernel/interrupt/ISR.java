package newOs.kernel.interrupt;


import com.alibaba.fastjson.JSONObject;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.InterruptSysCallInfo;

// InterruptServiceRoutine

public interface ISR<T extends InterruptInfo> {
    InterruptInfo execute(T interruptInfo);
}