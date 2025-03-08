package newOs.kernel.process;


import lombok.extern.slf4j.Slf4j;
import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.process.scheduler.SideScheduler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static newOs.common.processConstant.InstructionConstant.*;
import static newOs.common.processConstant.processStateConstant.RUNNING;
import static newOs.kernel.process.scheduler.ProcessScheduler.strategy;


@Slf4j
public class ProcessExecutionTask implements Runnable{
    private final PCB pcb;
    private final String [] instructions;

    private final SideScheduler Sscheduler; // 调度器依赖


    private final ConcurrentHashMap<Long, InterruptRequestLine> irlTable;




    private final ConcurrentLinkedQueue<PCB> waitingQueue;
    private final ConcurrentLinkedQueue<PCB> readyQueue;
    private final ConcurrentLinkedQueue<PCB> runningQueue;


    // 设备控制表
    //private final LinkedList<DeviceInfo> deviceInfoTable;

    private final HandleISR handleISR;


    public ProcessExecutionTask(PCB pcb,ProtectedMemory protectedMemory,HandleISR handleISR,SideScheduler Sscheduler) {
        this.pcb = pcb;

        //暂时用pcb的模块进行模拟
        //实际上 pcb中存储 内存映射信息（页表基址）
        this.instructions = pcb.getInstructions();

        this.irlTable = protectedMemory.getIrlTable();
        this.waitingQueue = protectedMemory.getWaitingQueue();
        this.readyQueue = protectedMemory.getReadyQueue();
        this.runningQueue = protectedMemory.getRunningQueue();
        this.handleISR = handleISR;
        this.Sscheduler = Sscheduler;
    }

    @Override
    public void run() {
        try {
            InterruptRequestLine irl = irlTable.get(Thread.currentThread().getId());

            //0表示未切换，1表示切换后放进就绪队列，2表示切换后放进等待队列
            //模拟流水线切换指令边界
            int isSwitchProcess = 0;

            setStratgy();
            //获取当前线程的id
            for (int ir = pcb.getIr(); ir < instructions.length; ir = pcb.getIr()) {
                String instruction = instructions[ir];
                //执行Q退出的时候，不需要检测时间片是否用完
                if (instruction.equals(Q)) {
                    executeInstruction(instruction);
                    String peek = irl.peek();
                    if (peek != null) {
                        handleISR.handlIsrInterruptIO();
                    }
                    break;
                } else {
                    // 执行到IO指令，一直获取不到文件资源，都会导致进程切换，ir不会+1
                    isSwitchProcess = executeInstruction(instruction);

                    //2 表示进行IO，硬盘访问
                    if (isSwitchProcess == 2) {


                    } else {
                        // CPU每执行一条指令，都需要去检查 irl 是否有中断信号
                        String peek = irl.peek();
                        if (peek != null) {
                            // 处理硬件中断信号，CPU去执行中断处理程序了。时间片耗尽也会导致进程切换，isSwitchProcess = 1 表示时间片耗尽导致的进程切换
                            int i = handleISR.handlIsrInterrupt(pcb);
                            if (i != 0)
                                isSwitchProcess = i;
                        }
                        log.info(pcb.getProcessName() + "出让CPU");
                        // 时间片耗尽导致进程切换
                        if (isSwitchProcess > 0)
                            break;
                    }
                }
            }
            Sscheduler.executeNextProcess();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void setStratgy() {

        pcb.setState(RUNNING);
        //需要实现时间片控制
        if (strategy.equals("RR")) // 时间片轮转
            pcb.setRemainingTime(3800L);
        else if (strategy.equals("MLFQ") && pcb.getRemainingTime() < 0)    // 多级反馈队列
            pcb.setRemainingTime(3800L);
        //不需要实现时间片控制
        else if (strategy.equals("FCFS") || strategy.equals("SJF"))   //先来先服务或短作业优先
            pcb.setRemainingTime(99999999L);

        //调度
        Sscheduler.schedulerProcess(pcb);
    }
    private int executeInstruction(String instruction) {
        int isSwitchProcess = 0;
        String [] parts = instruction.split(" ");
        String command = parts[0];
        try{
            log.info("当前执行指令：" + instruction);
            switch (command){
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
