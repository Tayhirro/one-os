package newOs.bootloader;

import newOs.component.cpu.Interrupt.IDTableImpl.X86IDTableCreate;
import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.device.Disk;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.device.DeviceDriver;
import newOs.kernel.device.DeviceImpl.DeviceDriverImpl;
import newOs.kernel.device.DeviceImpl.DiskDriverImpl;
import newOs.kernel.interrupt.InterruptController;
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
    private final Disk disk;

    //注入初始化组件
    private final X86IDTableCreate x86IDTableCreate;
    private final InterruptController interruptController;


    @Autowired
    public bootLoader(X86CPUSimulator x86CPUSimulator,
                      ProtectedMemory protectedMemory,
                      ProcessScheduler processScheduler,
                      ProcessManageServiceImpl processManageServiceImpl,
                      X86IDTableCreate x86IDTableCreate,
                      InterruptController interruptController,
                      Disk disk){
        this.x86CPUSimulator = x86CPUSimulator;
        this.protectedMemory = protectedMemory;
        this.processScheduler = processScheduler;
        this.processManageServiceImpl = processManageServiceImpl;
        this.x86IDTableCreate = x86IDTableCreate;
        this.interruptController = interruptController;
        this.disk = disk;
    }


    @Override
    public void run(ApplicationArguments args) {
        // 通过 protectedMemory 访问共享资源

        ExecutorService[] executors = x86CPUSimulator.getExecutors();
        // 创建 CountDownLatch 计数器，初始值为 4（4 个线程）
        CountDownLatch latch = new CountDownLatch(4);
        // 初始化逻辑
        for(int t = 1; t<=4; t++) {
            for (int i = 0; i < 1; i++) {
                executors[t].submit(() -> {
                    long threadId = Thread.currentThread().getId();
                    protectedMemory.getIrlTable().put(threadId, new InterruptRequestLine("TIMER_INTERRUPT"));
                    try {
                        Thread.sleep(10);
                        System.out.println("initializing" + Thread.currentThread().getId());
                        //打印irltable
                        for (Long key : protectedMemory.getIrlTable().keySet()) {
                            System.out.println("key: " + key + " value: " + protectedMemory.getIrlTable().get(key));
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();  // 线程完成后，计数器减 1
                    }
                });
            }
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
                "C 1000",
                "A 10240",
                "C 1000",
                "A 10240",
                "C 1000",
                "A 10240",
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
                "C 1000",
                "A 10240",
                "C 1000",
                "A 10240",
                "C 1000",
                "A 10240",
                "C 1000",
                "A 10240",
                "C 1000",
                "A 10240",
                "C 1000",
                "A 10240",
                "Q"
        };
        String[] inst3 = {
                "M 10",
                "C 1000",
                "C 1000",
                "A 10240",
                "C 1000",
                "OPEN disk1",
                "READ disk1",
                "CLOSE disk1",
                "A 10240",
                "C 1000",
                "A 10240",
                "Q"
        };
        int pid1 = getPid("process1");
        int pid2 = getPid("process2");
        int pid3 = getPid("process3");
        int pid4 = getPid("process4");
        int pid5 = getPid("process5");




        PCB pcb1 = new PCB(pid1, "process1", 0, -1,CREATED,-1 ,-1 ,-1, 3, -1,-1,-1,3, inst3,-1,-1,-1,-1);
        PCB pcb2 = new PCB(pid2, "process2", 0, -1,CREATED,-1 ,-1 ,-1, 3, -1,-1,-1,3, inst3,-1,-1,-1,-1);
        PCB pcb3 = new PCB(pid3, "process3", 0, -1,CREATED,-1 ,-1 ,-1, 3, -1,-1,-1,3, inst3,-1,-1,-1,-1);
        PCB pcb4 = new PCB(pid4, "process4", 0, -1,CREATED,-1 ,-1 ,-1, 3, -1,-1,-1,3, inst3,-1,-1,-1,-1);
        PCB pcb5 = new PCB(pid5, "process5", 0, -1,CREATED,-1 ,-1 ,-1, 3, -1,-1,-1,3, inst3,-1,-1,-1,-1);


        //放置process到PCB表中
        protectedMemory.getPcbTable().put(pid1, pcb1);
        protectedMemory.getPcbTable().put(pid2, pcb2);
        protectedMemory.getPcbTable().put(pid3, pcb3);
        protectedMemory.getPcbTable().put(pid4, pcb4);
        protectedMemory.getPcbTable().put(pid5, pcb5);


        DeviceDriver deviceDriver1 = new DiskDriverImpl("disk1", null,interruptController,disk);

        protectedMemory.getDeviceQueue().add(deviceDriver1);



        processManageServiceImpl.executeProcess("process1");
        processManageServiceImpl.executeProcess("process2");
        processManageServiceImpl.executeProcess("process3");
        processManageServiceImpl.executeProcess("process4");
        processManageServiceImpl.executeProcess("process5");

        


    }
}