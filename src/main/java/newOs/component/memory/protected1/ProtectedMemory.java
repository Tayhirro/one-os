package newOs.component.memory.protected1;


import lombok.Data;
import newOs.common.InterruptConstant.InterruptType;
import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.kernel.interrupt.ISR;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;


/**
 * 保护内存
 */

@Component
@Data
public class ProtectedMemory {

    // 进程控制块表
    private HashMap<Integer, PCB> pcbTable; // 假设每个PCB有一个唯一的PID
    // 中断向量表
//    // 设备控制表
//    private LinkedList<DeviceInfo> deviceInfoTable;
//    // 文件信息表
//    private HashMap<FileNode, FileInfoo> fileInfoTable;
    private  ConcurrentHashMap<InterruptType, ISR> IDT;

    // 运行队列  --目前正在执行的pcb
    private  ConcurrentLinkedQueue<PCB> runningQueue;
    // 就绪队列  --低级调度就绪队列
    private  ConcurrentLinkedQueue<PCB> readyQueue;



    // 等待队列 --内存阻塞队列
    private ConcurrentLinkedQueue<PCB> waitingQueue;

    //sjf队列
    private final PriorityBlockingQueue<PCB> readySJFQueue;


    private final ConcurrentLinkedQueue<PCB> highPriorityQueue;
    private final ConcurrentLinkedQueue<PCB> mediumPriorityQueue;
    private final ConcurrentLinkedQueue<PCB> lowPriorityQueue;



    //中断请求表
    private final ConcurrentHashMap<Long, InterruptRequestLine> irlTable;

    //IO中断线
    private final ConcurrentLinkedQueue<Integer> irlIO;

    public  ProtectedMemory() {
        // 初始化数据结构
        pcbTable = new HashMap<>();
//        deviceInfoTable = new LinkedList<>();
//        fileInfoTable = new HashMap<>();
        IDT = new ConcurrentHashMap<>();
        irlTable = new ConcurrentHashMap<>();

        runningQueue = new ConcurrentLinkedQueue<>();
        readyQueue = new ConcurrentLinkedQueue<>();
        waitingQueue = new ConcurrentLinkedQueue<>();

        readySJFQueue = new PriorityBlockingQueue<>(10, (o1, o2) -> {
            if (o1.getExpectedTime() > o2.getExpectedTime()) {
                return 1;
            } else if (o1.getExpectedTime() < o2.getExpectedTime()) {
                return -1;
            } else {
                return 0;
            }
        });

        highPriorityQueue = new ConcurrentLinkedQueue<>();
        mediumPriorityQueue = new ConcurrentLinkedQueue<>();
        lowPriorityQueue = new ConcurrentLinkedQueue<>();
        irlIO = new ConcurrentLinkedQueue<>();

    }
}
