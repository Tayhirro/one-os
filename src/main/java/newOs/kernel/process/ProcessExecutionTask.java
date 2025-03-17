package newOs.kernel.process;


import lombok.extern.slf4j.Slf4j;
import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
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







    // 设备控制表
    //private final LinkedList<DeviceInfo> deviceInfoTable;

    private final newOs.kernel.interrupt.hardwareHandler.ISRHandler ISRHandler;


    public ProcessExecutionTask(PCB pcb, ProtectedMemory protectedMemory, ISRHandler ISRHandler, SideScheduler Sscheduler) {
        this.pcb = pcb;

        //暂时用pcb的模块进行模拟
        //实际上 pcb中存储 内存映射信息（页表基址）
        this.instructions = pcb.getInstructions();

        this.irlTable = protectedMemory.getIrlTable();
        this.ISRHandler = ISRHandler;
        this.Sscheduler = Sscheduler;
    }

    @Override
    public void run() {
        try {
            InterruptRequestLine irl = irlTable.get(Thread.currentThread().getId());

            //0表示未切换，1表示切换后放进就绪队列，2表示切换后放进等待队列
            //模拟流水线切换指令边界
            int isSwitchProcess = 0;

            Sscheduler.schedulerProcess(pcb);   //调度进程

            //获取当前线程的id
            for (int ir = pcb.getIr(); ir < instructions.length; ir = pcb.getIr()) {
                String instruction = instructions[ir];
                //执行Q退出的时候，不需要检测时间片是否用完
                if (instruction.equals(Q)) {
                    executeInstruction(instruction);
                    String peek = irl.peek();
                    if (peek != null) {
                        ISRHandler.handlIsrInterruptIO();
                    }
                    break;
                } else {
                    // 执行到IO指令，一直获取不到文件资源，都会导致进程切换，ir不会+1
                    isSwitchProcess = executeInstruction(instruction);

                    //2 表示进行IO，硬盘访问
                    if (isSwitchProcess == 2) {


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
                            }
                            log.info(pcb.getProcessName() + "出让CPU");
                            // 时间片耗尽导致进程切换
                            if (isSwitchProcess > 0)
                                break;
                        }
                    }
                }
            }
            Sscheduler.executeNextProcess();
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
                    //设置信息


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
                case "K":       //IO中断
                    break;
                case "D":         //硬盘查询中断
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
