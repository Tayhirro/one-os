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
import newOs.kernel.memory.model.VirtualAddress;
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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson.JSONObject;

import static newOs.common.processConstant.processStateConstant.CREATED;
import static newOs.tools.ProcessTool.getPid;

@Component
public class bootLoader implements ApplicationRunner {
    // 注入核心组件
    private final X86CPUSimulator x86CPUSimulator;
    private final ProtectedMemory protectedMemory;
    private ProcessScheduler processScheduler;
    private final ProcessManageServiceImpl processManageServiceImpl;
    private final Disk disk;

    //注入初始化组件
    private final X86IDTableCreate x86IDTableCreate;
    private InterruptController interruptController;


    @Autowired
    public bootLoader(X86CPUSimulator x86CPUSimulator,
                      ProtectedMemory protectedMemory,
                      ProcessManageServiceImpl processManageServiceImpl,
                      X86IDTableCreate x86IDTableCreate,
                      Disk disk){
        this.x86CPUSimulator = x86CPUSimulator;
        this.protectedMemory = protectedMemory;
        this.processManageServiceImpl = processManageServiceImpl;
        this.x86IDTableCreate = x86IDTableCreate;
        this.disk = disk;
    }
    
    @Autowired
    public void setProcessScheduler(ProcessScheduler processScheduler) {
        this.processScheduler = processScheduler;
    }
    
    @Autowired
    public void setInterruptController(InterruptController interruptController) {
        this.interruptController = interruptController;
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
//        String[] inst2 = {
//                "M 10",
//                "C 500",
//                "A 1024",
//                "C 1000",
//                "C 1000",
//                "A 10240",
//                "C 1000",
//                "A 10240",
//                "C 1000",
//                "A 10240",
//                "C 1000",
//                "A 10240",
//                "C 1000",
//                "A 10240",
//                "C 1000",
//                "A 10240",
//                "Q"
//        };
        //测试多次分配后的访问
        String[] inst2 = {
                "M 10",
                "C 500",
                "A 8190",
                "A 8193",
                "M 10",
                "A 8208",
                "A 8211",
                "A 8212",
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
                "A 1024",
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




        PCB pcb1 = new PCB();
        pcb1.setPid(pid1);
        pcb1.setProcessName("process1");
        pcb1.setIr(0);
        pcb1.setSize(-1);
        pcb1.setState(CREATED);
        pcb1.setPBTR(-1);
        pcb1.setSBTR(-1);
        pcb1.setPageTableSize(-1);
        pcb1.setSegmentTableSize(3);
        pcb1.setTimeStamp(-1);
        pcb1.setRemainingTime(-1);
        pcb1.setExpectedTime(-1);
        pcb1.setPriority(3);
        pcb1.setInstructions(inst1);
        pcb1.setSwapInTime(-1);
        pcb1.setSwapOutTime(-1);
        pcb1.setPageFaultRate(-1);
        pcb1.setCoreId(-1);
        pcb1.setMemoryAllocationMap(new HashMap<>());
        pcb1.setAllocatedPageFrames(new ArrayList<>());
        pcb1.setMemoryAccessRights(new HashMap<>());
        pcb1.setMemorySegments(new ArrayList<>());
        
        PCB pcb2 = new PCB();
        pcb2.setPid(pid2);
        pcb2.setProcessName("process2");
        pcb2.setIr(0);
        pcb2.setSize(-1);
        pcb2.setState(CREATED);
        pcb2.setPBTR(-1);
        pcb2.setSBTR(-1);
        pcb2.setPageTableSize(-1);
        pcb2.setSegmentTableSize(3);
        pcb2.setTimeStamp(-1);
        pcb2.setRemainingTime(-1);
        pcb2.setExpectedTime(-1);
        pcb2.setPriority(3);
        pcb2.setInstructions(inst2);
        pcb2.setSwapInTime(-1);
        pcb2.setSwapOutTime(-1);
        pcb2.setPageFaultRate(-1);
        pcb2.setCoreId(-1);
        pcb2.setMemoryAllocationMap(new HashMap<>());
        pcb2.setAllocatedPageFrames(new ArrayList<>());
        pcb2.setMemoryAccessRights(new HashMap<>());
        pcb2.setMemorySegments(new ArrayList<>());
        
        PCB pcb3 = new PCB();
        pcb3.setPid(pid3);
        pcb3.setProcessName("process3");
        pcb3.setIr(0);
        pcb3.setSize(-1);
        pcb3.setState(CREATED);
        pcb3.setPBTR(-1);
        pcb3.setSBTR(-1);
        pcb3.setPageTableSize(-1);
        pcb3.setSegmentTableSize(3);
        pcb3.setTimeStamp(-1);
        pcb3.setRemainingTime(-1);
        pcb3.setExpectedTime(-1);
        pcb3.setPriority(3);
        pcb3.setInstructions(inst3);
        pcb3.setSwapInTime(-1);
        pcb3.setSwapOutTime(-1);
        pcb3.setPageFaultRate(-1);
        pcb3.setCoreId(-1);
        pcb3.setMemoryAllocationMap(new HashMap<>());
        pcb3.setAllocatedPageFrames(new ArrayList<>());
        pcb3.setMemoryAccessRights(new HashMap<>());
        pcb3.setMemorySegments(new ArrayList<>());
        
        PCB pcb4 = new PCB();
        pcb4.setPid(pid4);
        pcb4.setProcessName("process4");
        pcb4.setIr(0);
        pcb4.setSize(-1);
        pcb4.setState(CREATED);
        pcb4.setPBTR(-1);
        pcb4.setSBTR(-1);
        pcb4.setPageTableSize(-1);
        pcb4.setSegmentTableSize(3);
        pcb4.setTimeStamp(-1);
        pcb4.setRemainingTime(-1);
        pcb4.setExpectedTime(-1);
        pcb4.setPriority(3);
        pcb4.setInstructions(inst3);
        pcb4.setSwapInTime(-1);
        pcb4.setSwapOutTime(-1);
        pcb4.setPageFaultRate(-1);
        pcb4.setCoreId(-1);
        pcb4.setMemoryAllocationMap(new HashMap<>());
        pcb4.setAllocatedPageFrames(new ArrayList<>());
        pcb4.setMemoryAccessRights(new HashMap<>());
        pcb4.setMemorySegments(new ArrayList<>());
        
        PCB pcb5 = new PCB();
        pcb5.setPid(pid5);
        pcb5.setProcessName("process5");
        pcb5.setIr(0);
        pcb5.setSize(-1);
        pcb5.setState(CREATED);
        pcb5.setPBTR(-1);
        pcb5.setSBTR(-1);
        pcb5.setPageTableSize(-1);
        pcb5.setSegmentTableSize(3);
        pcb5.setTimeStamp(-1);
        pcb5.setRemainingTime(-1);
        pcb5.setExpectedTime(-1);
        pcb5.setPriority(3);
        pcb5.setInstructions(inst3);
        pcb5.setSwapInTime(-1);
        pcb5.setSwapOutTime(-1);
        pcb5.setPageFaultRate(-1);
        pcb5.setCoreId(-1);
        pcb5.setMemoryAllocationMap(new HashMap<>());
        pcb5.setAllocatedPageFrames(new ArrayList<>());
        pcb5.setMemoryAccessRights(new HashMap<>());
        pcb5.setMemorySegments(new ArrayList<>());


        //放置process到PCB表中
        protectedMemory.getPcbTable().put(pid1, pcb1);
        protectedMemory.getPcbTable().put(pid2, pcb2);
        protectedMemory.getPcbTable().put(pid3, pcb3);
        protectedMemory.getPcbTable().put(pid4, pcb4);
        protectedMemory.getPcbTable().put(pid5, pcb5);


        // 创建磁盘设备信息
        JSONObject diskDeviceInfo = new JSONObject();
        diskDeviceInfo.put("name", "disk1");
        diskDeviceInfo.put("type", "DISK");
        diskDeviceInfo.put("status", "READY");
        diskDeviceInfo.put("bufferSize", 4 * 1024 * 1024); // 4MB缓冲区

        DeviceDriver deviceDriver1 = new DiskDriverImpl("disk1", diskDeviceInfo, interruptController, disk);

        protectedMemory.getDeviceQueue().add(deviceDriver1);

        // 为每个进程分配内存
        try {
            // 为进程分配基本内存区域
            for (PCB pcb : protectedMemory.getPcbTable().values()) {
                // 分配代码段、数据段、堆和栈
                int pid = pcb.getPid();
                
                // 默认分配8K代码段、8K数据段、16K堆区和8K栈区
                VirtualAddress codeSegment = protectedMemory.allocateMemory(pid, 8 * 1024);
                
                // 更新PCB中的内存映射信息
                pcb.setCodeSegmentStart(codeSegment);
                pcb.setCodeSegmentSize(8 * 1024);
                pcb.setStackSize(8 * 1024);
                
                // 堆和栈会在需要时自动分配
                
                // 创建内存分配映射
                Map<VirtualAddress, Long> memoryMap = new HashMap<>();
                memoryMap.put(codeSegment, (long)(8 * 1024));
                pcb.setMemoryAllocationMap(memoryMap);
                
                // 记录已分配页帧
                pcb.setAllocatedPageFrames(new ArrayList<>());
                
                // 设置内存访问权限
                pcb.setMemoryAccessRights(new HashMap<>());
                
                // 添加内存段
                List<PCB.MemorySegment> segments = new ArrayList<>();
                segments.add(new PCB.MemorySegment(codeSegment, 8 * 1024, "CODE", "RX"));
                pcb.setMemorySegments(segments);
            }
        } catch (Exception e) {
            System.err.println("为进程分配内存失败: " + e.getMessage());
            e.printStackTrace();
        }

        processManageServiceImpl.executeProcess("process1");
        processManageServiceImpl.executeProcess("process2");
        processManageServiceImpl.executeProcess("process3");
        processManageServiceImpl.executeProcess("process4");
        processManageServiceImpl.executeProcess("process5");

        


    }
}