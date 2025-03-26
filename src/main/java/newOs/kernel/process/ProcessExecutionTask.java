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
import newOs.kernel.process.scheduler.SideScheduler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
                String instruction = instructions[ir];
                //执行Q退出的时候，不需要检测时间片是否用完
                if (instruction.equals(Q)) {
                    executeInstruction(instruction);
                    break;
                } else {
                    // 执行到IO指令，一直获取不到文件资源，都会导致进程切换，ir不会+1
                    isSwitchProcess = executeInstruction(instruction);

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
        String [] parts = instruction.split(" ");
        String command = parts[0];
        try{
            log.info("当前执行指令：" + instruction);
            switch (command){
                case "M":
                    //设置PCB大小信息


                case "A":       //进行逻辑地址的解析
                    int logicAddress = Integer.parseInt(parts[1]);
                    // 8 12 12
                    if (logicAddress == 8024) {
                        System.out.println("nihoa");
                    }
                    byte[] byteArray = new byte[4];
                    System.out.println("----------------------testtestetset:   " + pcb.getSBTR());

                    //申请资源
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

}
