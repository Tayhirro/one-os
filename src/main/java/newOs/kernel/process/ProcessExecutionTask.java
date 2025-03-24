package newOs.kernel.process;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import newOs.common.InterruptConstant.InterruptType;
import newOs.common.InterruptConstant.SystemCallType;
import newOs.common.fileSystemConstant.DeviceStatusType;
import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoImplDTO;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.kernel.interrupt.InterruptController;
import newOs.kernel.interrupt.hardwareHandler.ISRHandler;
import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.exception.MemoryException;
import newOs.exception.MemoryAllocationException;
import newOs.kernel.process.scheduler.SideScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import newOs.dto.req.Info.InterruptInfo;
import newOs.dto.req.Info.MemoryInterruptInfo;
import java.util.HashMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;

import static newOs.common.processConstant.InstructionConstant.*;
import static newOs.common.processConstant.processStateConstant.RUNNING;
import static newOs.common.processConstant.processStateConstant.TERMINATED;
import static newOs.kernel.process.scheduler.ProcessScheduler.strategy;


@Slf4j
public class ProcessExecutionTask implements Runnable{
    private final PCB pcb;
    private final String [] instructions;

    private final SideScheduler Sscheduler; // 调度器依赖


    private final ConcurrentHashMap<Long, InterruptRequestLine> irlTable;
    private final ConcurrentLinkedQueue<PCB> irlIO;






    // 设备控制表
    //private final LinkedList<DeviceInfo> deviceInfoTable;

    private final ISRHandler ISRHandler;
    private final InterruptController interruptController;
    
    // 添加ApplicationContext引用
    private static ApplicationContext applicationContext;
    
    // 提供静态方法设置ApplicationContext
    @Autowired
    public static void setApplicationContext(ApplicationContext context) {
        ProcessExecutionTask.applicationContext = context;
    }
    
    // 获取MemoryManager实例的辅助方法
    private MemoryManager getMemoryManager() {
        if (applicationContext != null) {
            return applicationContext.getBean(MemoryManager.class);
        }
        log.warn("ApplicationContext未设置，无法获取MemoryManager实例");
        return null;
    }

    // 获取InterruptController实例的辅助方法
    private InterruptController getInterruptController() {
        // 直接返回成员变量中的interruptController实例
        return this.interruptController;
    }

    public ProcessExecutionTask(PCB pcb, ProtectedMemory protectedMemory, ISRHandler ISRHandler, SideScheduler Sscheduler, InterruptController interruptController) {
        this.pcb = pcb;

        //暂时用pcb的模块进行模拟
        //实际上 pcb中存储 内存映射信息（页表基址）
        this.instructions = pcb.getInstructions();

        this.irlTable = protectedMemory.getIrlTable();
        this.irlIO = protectedMemory.getIrlIO();
        this.ISRHandler = ISRHandler;
        this.Sscheduler = Sscheduler;
        this.interruptController = interruptController;
    }

    @Override
    public void run() {
        try {
            InterruptRequestLine irl = irlTable.get(Thread.currentThread().getId());

            //对于run， coreID = executeservice-i

            //0表示未切换，1表示切换后放进就绪队列，2表示切换后放进等待队列
            //模拟流水线切换指令边界
            int isSwitchProcess = 0;

            Sscheduler.schedulerProcess(pcb);

            //获取当前线程的id
            for (int ir = pcb.getIr(); ir < instructions.length; ir = pcb.getIr()) {
                // 在执行每条指令前检查进程是否已被终止
                if (!isProcessExist(pcb.getPid())) {
                    log.info("进程{}已终止，停止执行任务", pcb.getPid());
                    break; // 终止循环，不再执行后续指令
                }
                
                String instruction = instructions[ir];
                //执行Q退出的时候，不需要检测时间片是否用完
                if (instruction.equals(Q)) {
                    executeInstruction(instruction);
                    break;
                } else {
                    // 执行到IO指令，一直获取不到文件资源，都会导致进程切换，ir不会+1
                    isSwitchProcess = executeInstruction(instruction);
                    
                    // 如果返回-1，表示进程已终止
                    if (isSwitchProcess == -1) {
                        log.info("进程{}已终止，停止执行任务", pcb.getPid());
                        break;
                    }

                    //2 表示进行IO等待
                    if (isSwitchProcess == 2) {
                        //进程切换等待
                        Sscheduler.Runing2Wait(pcb);
                        break;
                    } else {
                        //执行完一条指令之后
                        //检测时间片
                        pcb.setIr(ir + 1);
                        String peek = irl.peek();
                        if (peek != null) {
                            int i = ISRHandler.handlIsrInterrupt(pcb);
                            if (i != 0) {         //拷贝isSwitchProcess
                                isSwitchProcess = i; //进行进程的调度切换
                                //时间片用完,调度到等待队列
                                Sscheduler.Runing2Ready(pcb);
                                log.info(pcb.getProcessName() + "出让CPU");
                            }
                            // 时间片耗尽导致进程切换
                            if (isSwitchProcess > 0)
                                break;
                        }
                    }
                }
            }
            Sscheduler.executeNextProcess(pcb.getCoreId());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private int executeInstruction(String instruction) {
        int isSwitchProcess = 0;
        
        // 在执行指令前检查进程是否已被终止
        if (!isProcessExist(pcb.getPid())) {
            log.info("进程{}已终止，不执行指令: {}", pcb.getPid(), instruction);
            return -1; // 返回-1表示进程已终止
        }
        
        String [] parts = instruction.split(" ");
        String command = parts[0];
        try{
            log.info("当前执行指令：" + instruction);
            switch (command){
                case "M":
                    // 声明进程所需内存空间
                    String[] memoryParts = instruction.split(" ");
                    if (memoryParts.length < 2) {
                        log.error("M指令参数不足！");
                        return isSwitchProcess; // 返回当前切换状态
                    }
                    
                    try {
                        int memorySize = Integer.parseInt(memoryParts[1]);
                        log.info("进程{}声明需要{}字节内存", pcb.getPid(), memorySize);
                        
                        // 设置进程PCB中的内存大小
                        pcb.setSize(memorySize);
                        
                        // 实际分配内存
                        MemoryManager memoryManager = getMemoryManager();
                        
                        // 分配内存，并记录到PCB的内存分配表中
                        VirtualAddress virtualAddress = memoryManager.allocateMemory(pcb.getPid(), memorySize);
                        if (virtualAddress != null) {
                            log.info("进程{}成功分配{}字节内存，起始地址=0x{}", 
                                    pcb.getPid(), memorySize, Long.toHexString(virtualAddress.getValue()));
                            
                            // 确保PCB的内存分配表已初始化
                            if (pcb.getMemoryAllocationMap() == null) {
                                pcb.setMemoryAllocationMap(new HashMap<>());
                            }
                            
                            // 更新PCB的内存分配表
                            pcb.getMemoryAllocationMap().put(virtualAddress, (long)memorySize);
                        } else {
                            log.error("进程{}分配{}字节内存失败", pcb.getPid(), memorySize);
                        }
                    } catch (NumberFormatException e) {
                        log.error("M指令内存大小格式错误: {}", memoryParts[1]);
                    } catch (Exception e) {
                        log.error("M指令执行异常: {}", e.getMessage());
                    }
                    break;

                case "A":
                    // 模拟访问内存
                    String[] memoryAccessParts = instruction.split(" ");
                    if (memoryAccessParts.length < 2) {
                        log.error("A指令参数不足！");
                        return isSwitchProcess; // 返回当前切换状态
                    }

                    // 解析访问的相对地址
                    String addressStr = memoryAccessParts[1];
                    try {
                        long relativeAddress = Long.parseLong(addressStr);
                        boolean isWrite = memoryAccessParts.length > 2 && "write".equalsIgnoreCase(memoryAccessParts[2]);
                        
                        // 获取进程的内存分配表
                        Map<VirtualAddress, Long> memoryMap = pcb.getMemoryAllocationMap();
                        if (memoryMap == null || memoryMap.isEmpty()) {
                            log.error("进程{}没有分配内存，无法访问地址", pcb.getPid());
                            getInterruptController().triggerSegmentationFault(pcb.getPid(), new VirtualAddress(relativeAddress));
                            return isSwitchProcess;
                        }
                        
                        // 获取进程的所有内存块，按虚拟地址排序
                        List<Map.Entry<VirtualAddress, Long>> sortedMemoryBlocks = new ArrayList<>(memoryMap.entrySet());
                        sortedMemoryBlocks.sort(Comparator.comparingLong(entry -> entry.getKey().getValue()));
                        
                        // 确定相对地址应该映射到哪个内存块
                        VirtualAddress targetBlockAddress = null;
                        long blockSize = 0;
                        long currentOffset = 0;
                        
                        for (Map.Entry<VirtualAddress, Long> entry : sortedMemoryBlocks) {
                            VirtualAddress blockStart = entry.getKey();
                            long size = entry.getValue();
                            
                            // 如果相对地址在当前块范围内
                            if (relativeAddress >= currentOffset && relativeAddress < currentOffset + size) {
                                targetBlockAddress = blockStart;
                                blockSize = size;
                                // 调整相对地址为块内偏移
                                relativeAddress = relativeAddress - currentOffset;
                                break;
                            }
                            
                            // 增加累计偏移量
                            currentOffset += size;
                        }
                        
                        // 如果没有找到匹配的块
                        if (targetBlockAddress == null) {
                            log.error("进程{}的相对地址0x{}超出所有分配内存范围", 
                                    pcb.getPid(), Long.toHexString(relativeAddress));
                            getInterruptController().triggerSegmentationFault(
                                    pcb.getPid(), new VirtualAddress(relativeAddress));
                            return isSwitchProcess;
                        }
                        
                        // 计算实际虚拟地址 = 目标块起始地址 + 块内偏移
                        long actualAddress = targetBlockAddress.getValue() + relativeAddress;
                        VirtualAddress virtualAddress = new VirtualAddress(actualAddress);
                        
                        log.info("进程{}尝试{}虚拟地址0x{}（相对地址0x{} + 块基址0x{}）", 
                                pcb.getPid(), 
                                isWrite ? "写入" : "读取", 
                                Long.toHexString(actualAddress),
                                Long.toHexString(relativeAddress),
                                Long.toHexString(targetBlockAddress.getValue()));
                        
                        // 验证计算出的地址是否在有效范围内
                        boolean addressValid = actualAddress >= targetBlockAddress.getValue() && 
                                               actualAddress < targetBlockAddress.getValue() + blockSize;
                        
                        if (!addressValid) {
                            log.error("进程{}访问无效地址0x{}，触发段错误！", 
                                    pcb.getPid(), Long.toHexString(actualAddress));
                            
                            // 触发段错误中断
                            getInterruptController().triggerSegmentationFault(pcb.getPid(), virtualAddress);
                            return isSwitchProcess; // 返回当前切换状态
                        }
                        
                        // 检查地址映射是否存在，没有则触发缺页异常
                        MemoryManager memoryManager = getMemoryManager();
                        boolean addressMapped = false;
                        
                        try {
                            // 尝试进行地址转换，这会抛出缺页异常如果页不在内存中
                            PhysicalAddress physicalAddress = memoryManager.translateVirtualToPhysical(pcb.getPid(), virtualAddress);
                            if (physicalAddress != null) {
                                addressMapped = true;
                                log.info("进程{}成功访问虚拟地址0x{}，对应物理地址0x{}", 
                                        pcb.getPid(), 
                                        Long.toHexString(virtualAddress.getValue()),
                                        Long.toHexString(physicalAddress.getValue()));
                            }
                        } catch (Exception e) {
                            // 可能是缺页异常
                            log.info("进程{}访问地址0x{}时遇到异常: {}", 
                                    pcb.getPid(), Long.toHexString(actualAddress), e.getMessage());
                            
                            // 触发缺页中断，无论是什么类型的异常
                            MemoryInterruptInfo memoryInfo = new MemoryInterruptInfo();
                            memoryInfo.setProcessId(pcb.getPid());
                            memoryInfo.setVirtualAddress(virtualAddress);
                            memoryInfo.setAdditionalInfo("isWrite", isWrite);
                            
                            getInterruptController().triggerInterrupt(
                                    InterruptType.PAGE_FAULT, 
                                    memoryInfo);
                            
                            // 中断处理后，重试地址转换
                            try {
                                PhysicalAddress physicalAddress = memoryManager.translateVirtualToPhysical(
                                        pcb.getPid(), virtualAddress);
                                
                                if (physicalAddress != null) {
                                    addressMapped = true;
                                    log.info("缺页处理后，进程{}成功访问虚拟地址0x{}，对应物理地址0x{}", 
                                            pcb.getPid(), 
                                            Long.toHexString(virtualAddress.getValue()),
                                            Long.toHexString(physicalAddress.getValue()));
                                }
                            } catch (Exception retryException) {
                                // 重试失败
                                log.error("缺页处理后重试失败: {}", retryException.getMessage());
                            }
                        }
                        
                        if (!addressMapped) {
                            log.error("进程{}无法访问地址0x{}，即使在缺页处理后", 
                                    pcb.getPid(), Long.toHexString(actualAddress));
                        }
                        
                    } catch (NumberFormatException e) {
                        log.error("A指令地址格式错误: {}", addressStr);
                    } catch (Exception e) {
                        log.error("A指令执行异常: {}", e.getMessage());
                    }
                    break;
                case "C":
                    int computeTime = Integer.parseInt(parts[1]);
                    Thread.sleep(computeTime);
                    pcb.setRemainingTime(pcb.getRemainingTime() - computeTime);
                    log.info(pcb.getProcessName() + "：" + instruction + "执行完成");
                    break;
                case "OPEN":
                    //打开文件- 如果返回是成功，则继续 如果返回失败，则直接抛出错误 -如果等待，则isSwitchProcess = 2
                    //封装
                    DeviceInfoImplDTO deviceInfo = new DeviceInfoImplDTO();
                    deviceInfo.setDeviceName(parts[1]);
                    deviceInfo.setInterruptType(InterruptType.SYSTEM_CALL);
                    deviceInfo.setSystemCallType(SystemCallType.OPEN_FILE);
                    deviceInfo.setPcb(pcb);
                    DeviceInfoReturnImplDTO deviceInfoReturn = (DeviceInfoReturnImplDTO) interruptController.triggerSystemCall(deviceInfo);
                    if(deviceInfoReturn.getDeviceStatusType() == DeviceStatusType.FREE){
                        //继续执行
                        isSwitchProcess = 0;
                    }else if(deviceInfoReturn.getDeviceStatusType() == DeviceStatusType.BUSY) {
                        //等待
                        isSwitchProcess = 2;
                    }
                    break;
                case "READ":
                    //读取文件- 如果返回是成功，则继续 如果返回失败，则直接抛出错误 -如果等待，则isSwitchProcess = 2
                    //封装
                    DeviceInfoImplDTO deviceInfo2 = new DeviceInfoImplDTO();
                    deviceInfo2.setDeviceName(parts[1]);
                    deviceInfo2.setInterruptType(InterruptType.SYSTEM_CALL);
                    deviceInfo2.setSystemCallType(SystemCallType.READ_FILE);
                    deviceInfo2.setPcb(pcb);
                    DeviceInfoReturnImplDTO deviceInfoReturn2 = (DeviceInfoReturnImplDTO) interruptController.triggerSystemCall(deviceInfo2);
                    if(deviceInfoReturn2.getDeviceStatusType() == DeviceStatusType.FREE){
                        //继续执行
                        isSwitchProcess = 0;
                    }else if(deviceInfoReturn2.getDeviceStatusType() == DeviceStatusType.BUSY) {
                        //等待
                        isSwitchProcess = 2;
                    }
                    break;
                case "WRITE":
                    DeviceInfoImplDTO deviceInfo3 = new DeviceInfoImplDTO();
                    deviceInfo3.setDeviceName(parts[1]);
                    //写入文件信息
                    deviceInfo3.setDeviceInfo(new JSONObject().fluentPut("content", parts[2]));
                    //写入文件信息
                    deviceInfo3.setInterruptType(InterruptType.SYSTEM_CALL);
                    deviceInfo3.setSystemCallType(SystemCallType.WRITE_FILE);
                    deviceInfo3.setPcb(pcb);
                    DeviceInfoReturnImplDTO deviceInfoReturn3 = (DeviceInfoReturnImplDTO) interruptController.triggerSystemCall(deviceInfo3);
                    if(deviceInfoReturn3.getDeviceStatusType() == DeviceStatusType.FREE){
                        //继续执行
                        isSwitchProcess = 0;
                    }else if(deviceInfoReturn3.getDeviceStatusType() == DeviceStatusType.BUSY) {
                        //等待
                        isSwitchProcess = 2;
                    }
                case "CLOSE":
//                    DeviceInfoImplDTO deviceInfo4 = new DeviceInfoImplDTO();
//                    deviceInfo4.setDeviceName(parts[1]);
//                    deviceInfo4.setInterruptType(InterruptType.SYSTEM_CALL);
//                    deviceInfo4.setSystemCallType(SystemCallType.CLOSE_FILE);
//                    deviceInfo4.setPcb(pcb);

                    break;

                case "Q":
                    pcb.setIr(0);
                    pcb.setState(TERMINATED);
                    pcb.setRemainingTime(-1);
                    // TODO lyq 释放内存
//                    mmu.Free(pcb.getRegister());
//                    pcb.setRegister(-1);
                    //移出队列
                    Sscheduler.Finnished(pcb);
                    break;
                default :
                    log.info("Unknown command.");
                    break;

            }

        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Task was interrupted.");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return isSwitchProcess;
    }

    // 检查进程是否存在
    private boolean isProcessExist(int pid) {
        try {
            // 最简单的方法：查询当前PCB的pid是否与传入的pid匹配
            // 由于每个ProcessExecutionTask只负责一个进程，我们只需要检查当前的pcb是否有效
            return pcb != null && pcb.getPid() == pid && pcb.getState() != null && !pcb.getState().equals(TERMINATED);
        } catch (Exception e) {
            // 发生异常时，安全地返回false
            log.error("检查进程{}是否存在时出错: {}", pid, e.getMessage());
            return false;
        }
    }

}
