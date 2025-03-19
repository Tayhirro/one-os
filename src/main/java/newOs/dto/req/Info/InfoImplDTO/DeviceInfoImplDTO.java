package newOs.dto.req.Info.InfoImplDTO;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import newOs.common.InterruptConstant.InterruptType;
import newOs.common.InterruptConstant.SystemCallType;
import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InterruptSysCallInfo;



@Data
@Accessors(chain = true)
@NoArgsConstructor
public class DeviceInfoImplDTO implements InterruptSysCallInfo{

    private SystemCallType systemCallType; // 系统调用类型
    private JSONObject deviceInfo; // 传递参数
    private InterruptType interruptType;
    private String deviceName;
    private PCB pcb;


    @Override
    public SystemCallType getSystemCallType() {
        return systemCallType;
    }
    @Override
    public InterruptType getInterruptType() {
        return interruptType;
    }

}
