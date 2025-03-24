package newOs.kernel.interrupt;

/**
 * 中断类型枚举
 * 定义系统中不同类型的中断
 */
public enum InterruptType {
    // 处理器异常
    DIVIDE_ERROR,              // 除零错误
    DEBUG,                     // 调试异常
    NMI,                       // 不可屏蔽中断
    BREAKPOINT,                // 断点
    OVERFLOW,                  // 溢出
    BOUND_RANGE_EXCEEDED,      // 边界检查失败
    INVALID_OPCODE,            // 无效操作码
    DEVICE_NOT_AVAILABLE,      // 设备不可用
    DOUBLE_FAULT,              // 双重故障
    COPROCESSOR_SEGMENT_OVERRUN, // 协处理器段越界
    INVALID_TSS,               // 无效TSS
    SEGMENT_NOT_PRESENT,       // 段不存在
    STACK_SEGMENT_FAULT,       // 栈段故障
    GENERAL_PROTECTION,        // 通用保护
    PAGE_FAULT,                // 页错误
    RESERVED,                  // 保留
    FLOATING_POINT_ERROR,      // 浮点错误
    ALIGNMENT_CHECK,           // 对齐检查
    MACHINE_CHECK,             // 机器检查
    SIMD_FLOATING_POINT,       // SIMD浮点异常
    VIRTUALIZATION_EXCEPTION,  // 虚拟化异常
    CONTROL_PROTECTION,        // 控制保护异常
    
    // 外部硬件中断
    HARDWARE_INTERRUPT,        // 硬件中断
    TIMER_INTERRUPT,           // 时钟中断
    KEYBOARD_INTERRUPT,        // 键盘中断
    DISK_INTERRUPT,            // 磁盘中断
    NETWORK_INTERRUPT,         // 网络中断
    
    // 软件中断
    SOFTWARE_INTERRUPT,        // 软件中断
    SYSTEM_CALL,               // 系统调用
    
    // 其他中断
    UNKNOWN                    // 未知中断
} 