package newOs.bootloader;

import newOs.component.cpu.Interrupt.IDTableImpl.X86IDTableCreate;
import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.process.ProcessManager;
import newOs.kernel.process.scheduler.ProcessScheduler;
import newOs.service.ServiaceImpl.ProcessManageServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static newOs.common.processConstant.processStateConstant.CREATED;
import static newOs.tools.ProcessTool.getPid;

@Component
public class bootLoader implements ApplicationRunner {
    // 注入核心组件
    private final X86CPUSimulator x86CPUSimulator;
    private final ProtectedMemory protectedMemory;
    private final ProcessScheduler processScheduler;
    private final ProcessManageServiceImpl processManageServiceImpl;

    //注入初始化组件
    private final X86IDTableCreate x86IDTableCreate;


    @Autowired
    public bootLoader(X86CPUSimulator x86CPUSimulator,
                      ProtectedMemory protectedMemory,
                      ProcessScheduler processScheduler,
                      ProcessManageServiceImpl processManageServiceImpl,
                      X86IDTableCreate x86IDTableCreate
                        ) {
        this.x86CPUSimulator = x86CPUSimulator;
        this.protectedMemory = protectedMemory;
        this.processScheduler = processScheduler;
        this.processManageServiceImpl = processManageServiceImpl;
        this.x86IDTableCreate = x86IDTableCreate;
    }


    @Override
    public void run(ApplicationArguments args) {
        // 通过 protectedMemory 访问共享资源
        ConcurrentHashMap<Long, InterruptRequestLine> irlTable =
                protectedMemory.getIrlTable();

        ExecutorService executor = x86CPUSimulator.getExecutor();
        // 创建 CountDownLatch 计数器，初始值为 4（4 个线程）
        CountDownLatch latch = new CountDownLatch(4);
        // 初始化逻辑
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                long threadId = Thread.currentThread().getId();
                irlTable.put(threadId, new InterruptRequestLine("TIMER_INTERRUPT"));
                try {
                    Thread.sleep(10);
                    System.out.println("initializing" + Thread.currentThread().getId());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    latch.countDown();  // 线程完成后，计数器减 1
                }
            });
        }
        // 等待所有线程初始化完成
        try {
            latch.await();  // 阻塞，直到 countDown() 调用 4 次
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //注入IDT表，进行IDT表的创建
        x86IDTableCreate.createIDTable();


        // 调度逻辑
       // for (int i = 0; i < 4; i++) {
           // processScheduler.spanWait(executor);

      //  }

        //提交任务让他阻塞
//        if(protectedMemory.getReadyQueue().isEmpty()){
//
//        }



        // 初始化文件
        String[] inst1 = {
                "C 1000",
                "A 10240",
                "C 1000",
                "A 10240",
                "C 1000",
                "A 10240",
                "Q"
        };
        String[] inst2 = {
                "M 10",
                "C 500",
                "A 1024",
                "C 1000",
                "Q"
        };
        String[] inst3 = {
                "M 10",
                "C 1000",
                "Q"
        };
        int pid1 = getPid("process1");
        int pid2 = getPid("process2");
        int pid3 = getPid("process3");

        PCB pcb1 = new PCB(pid1, "process1", 0, -1,CREATED,-1 ,-1 ,-1, 3, -1,-1,3, inst1,-1,-1,-1);
        PCB pcb2 = new PCB(pid2, "process2", 0, -1,CREATED,-1 ,-1 ,-1, 3, -1,-1,3, inst2,-1,-1,-1);
        PCB pcb3 = new PCB(pid3, "process3", 0, -1,CREATED,-1 ,-1 ,-1, 3, -1,-1,3, inst3,-1,-1,-1);


        //放置process到PCB表中
        protectedMemory.getPcbTable().put(pid1, pcb1);
        protectedMemory.getPcbTable().put(pid2, pcb2);
        protectedMemory.getPcbTable().put(pid3, pcb3);


        processManageServiceImpl.executeProcess("process1");

    }
}