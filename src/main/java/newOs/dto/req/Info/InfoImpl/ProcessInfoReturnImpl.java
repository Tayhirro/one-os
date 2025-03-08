package newOs.dto.req.Info.InfoImpl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import newOs.common.InterruptConstant.InterruptType;
import newOs.common.InterruptConstant.SystemCallType;
import newOs.dto.req.Info.InterruptSysCallInfo;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ProcessInfoReturnImpl implements InterruptSysCallInfo {
    private int pid;          // 进程ID（类似 fork() 返回值）
    private String name;      // 进程名称（如 "bash"）
    private String state;     // 进程状态（"RUNNING", "WAITING"）
    private int priority;     // 优先级（0-100）
    private long memoryUsage; // 内存占用（模拟 RSS）
    private String filePath;  // 进程对应的文件路径（如 "/bin/myapp"）
    private LocalDateTime createTime; //
    // 创建时间
    private SystemCallType systemCallType; // 系统调用类型
    private InterruptType interruptType;
    @Override
    public SystemCallType getSystemCallType() {
        return systemCallType;
    }
    @Override
    public InterruptType getInterruptType() {
        return interruptType;
    }

}
