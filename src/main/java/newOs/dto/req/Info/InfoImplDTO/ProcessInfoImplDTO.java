package newOs.dto.req.Info.InfoImplDTO;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import newOs.common.InterruptConstant.InterruptType;
import newOs.common.InterruptConstant.SystemCallType;
import newOs.dto.req.Info.InterruptSysCallInfo;


@Data
@Accessors(chain = true)
@NoArgsConstructor
public class ProcessInfoImplDTO implements InterruptSysCallInfo {
    private String name;      // 进程名称（如 "bash"）
    private int priority;     // 优先级（0-100）
    private SystemCallType systemCallType; // 系统调用类型
    private JSONObject args; // 传递参数
    private InterruptType interruptType;
    private String [] instructions;

    @Override
    public SystemCallType getSystemCallType() {
        return systemCallType;
    }
    @Override
    public InterruptType getInterruptType() {
        return interruptType;
    }
}
