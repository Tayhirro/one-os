package newOs.common.InterruptConstant;



import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum SystemCallType {
    CREATE_PROCESS(1),  // 创建进程
    EXECUTE_PROCESS(2), // 执行进程
    TERMINATE_PROCESS(3), // 终止进程
    READ_FILE(4), // 读取文件
    WRITE_FILE(5), // 写入文件
    ALLOCATE_MEMORY(6), // 分配内存
    DEALLOCATE_MEMORY(7); // 释放内存

    private final int syscallNumber;
    SystemCallType(int syscallNumber) {
        this.syscallNumber = syscallNumber;
    }

    public static SystemCallType fromNumber(int number) {
        for (SystemCallType type : values()) {
            if (type.getSyscallNumber() == number) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid system call number: " + number);
    }
}

