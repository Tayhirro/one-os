package newOs.common.InterruptConstant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * 中断类型枚举
 * 定义系统支持的各种中断类型及其向量号
 */
@Getter
public enum InterruptType {
    /** CPU 级异常 **/
    DIVIDE_BY_ZERO(0),
    DEBUG(1),
    NMI(2),
    BREAKPOINT(3),
    OVERFLOW(4),
    BOUND_RANGE(5),
    INVALID_OPCODE(6),
    DEVICE_NOT_AVAILABLE(7),
    DOUBLE_FAULT(8),
    INVALID_TSS(10),
    SEGMENT_NOT_PRESENT(11),
    STACK_SEGMENT_FAULT(12),
    GENERAL_PROTECTION_FAULT(13),
    PAGE_FAULT(14),

    /** 硬件中断（IRQ） **/
    TIMER(32),
    KEYBOARD(33),
    SERIAL_PORT_2(34),
    SERIAL_PORT_1(35),
    PARALLEL_PORT(36),
    FLOPPY_DISK(38),
    HARD_DISK(39),
    /*IO中断*/
    IO_INTERRUPT(40),

    /** 软件中断（系统调用） **/
    SYSTEM_CALL(0x80);

    private final int vector;

    InterruptType(int vector) {
        this.vector = vector;
    }
}
