package newOs.component.memory.protected1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
/**
 *
 * 8 12 12
 * 段表基址寄存器 页表基址寄存器 页内偏移
 *  256段数  4k页数  4k大小的页
 */
public class PCB {
    // 进程id
    private int pid;
    // 进程名
    private String processName;

    //上下文
    // 指令寄存器，保存当前正在执行指令地址
    private int ir;
    // 进程大小，单位为B
    private int size;
    // 进程状态
    private String state;
    // 页表基址寄存器
    private int PBTR;
    // 段表基址寄存器
    private int SBTR;
    // 页表大小
    private int pageTableSize;
    //段表大小
    private int segmentTableSize;

    // 剩余可执行时间，单位ms
    private long remainingTime;
    // 预期运行时间
    private long expectedTime;
    // 进程优先级
    private int priority;
    // 指令集
    private String[] instructions;



    //LRU算法执行换入换出时间
    private int swapInTime;

    private int swapOutTime;

    private int pageFaultRate;
}
