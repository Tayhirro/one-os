package newOs.dto.req.Info;

import newOs.common.InterruptConstant.SystemCallType;

public interface InterruptSysCallInfo extends InterruptInfo{
    SystemCallType getSystemCallType();
}
