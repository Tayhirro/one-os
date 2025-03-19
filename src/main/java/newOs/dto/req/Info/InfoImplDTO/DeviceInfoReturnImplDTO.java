package newOs.dto.req.Info.InfoImplDTO;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import newOs.common.InterruptConstant.InterruptType;
import newOs.common.InterruptConstant.SystemCallType;
import newOs.common.fileSystemConstant.DeviceStatusType;
import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InterruptSysCallInfo;


@Data
@Accessors(chain = true)
@NoArgsConstructor
public class DeviceInfoReturnImplDTO implements InterruptSysCallInfo {
    private SystemCallType systemCallType; // 系统调用类型


    private JSONObject args; // 传递参数 返回的具体内容
    private InterruptType interruptType;
    private String deviceName;
    private PCB pcb;
    private DeviceStatusType deviceStatusType;

    @Override
    public SystemCallType getSystemCallType() {
        return systemCallType;
    }
    @Override
    public InterruptType getInterruptType() {
        return interruptType;
    }

}
