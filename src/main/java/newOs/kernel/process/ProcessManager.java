package newOs.kernel.process;


import com.alibaba.fastjson.JSONObject;
import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.InfoImpl.ProcessInfoReturnImpl;
import newOs.tools.ProcessTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import static newOs.common.processConstant.processStateConstant.CREATED;

@Component
public class ProcessManager{
    //依赖注入:
    private final HashMap<Integer, PCB> pcbTable;
    private final Queue<PCB> readyQueue;
    private final Queue<PCB> runningQueue;
    private final Queue<PCB> waitingQueue;
    //没有实现中级调度




    @Autowired
    public ProcessManager(ProtectedMemory protectedMemory){
        this.pcbTable = protectedMemory.getPcbTable();
        this.readyQueue = protectedMemory.getReadyQueue();
        this.runningQueue = protectedMemory.getRunningQueue();
        this.waitingQueue = protectedMemory.getWaitingQueue();
    }

    public ProcessInfoReturnImpl createProcess(String processName, JSONObject args,String[] instructions){
        // 创建进程
        int pid = ProcessTool.getPid(processName);
        // 创建进程pcb，放进pcbTable
        PCB pcb = new PCB(pid, processName, 0, -1, CREATED, -1, -1, -1, -1,-1,-1,3,null,-1,-1,-1);
        pcbTable.put(pid, pcb);
        LinkedList<String> list = new LinkedList<>();
        long expectedTime = 0;
        //设置pcb的基础内容
        for (String inst : instructions) {
            if (inst.charAt(0) == 'M') {
                pcb.setSize(inst.charAt(2) * 1024);
            } else {
                list.add(inst);
                if (inst.charAt(0) == 'C') {
                    expectedTime += Long.parseLong(inst.split(" ")[1]);
                }
                if (inst.charAt(0) == 'R') {
                    expectedTime += Long.parseLong(inst.split(" ")[2]);
                }
                if (inst.charAt(0) == 'W') {
                    expectedTime += Long.parseLong(inst.split(" ")[2]);
                }
            }
        }
        pcb.setInstructions(list.toArray(new String[1]));
        pcb.setExpectedTime(expectedTime);      //setExpectedtime
        // 写进文件系统  --暂时可以不用做
        ProcessInfoReturnImpl processRInfo = new ProcessInfoReturnImpl();
        return processRInfo;
    }

}