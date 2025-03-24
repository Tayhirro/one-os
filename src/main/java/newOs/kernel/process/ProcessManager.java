package newOs.kernel.process;

import com.alibaba.fastjson.JSONObject;
import newOs.component.memory.protected1.PCB;
import newOs.dto.req.Info.InfoImplDTO.ProcessInfoReturnImplDTO;
import newOs.kernel.memory.model.VirtualAddress;

/**
 * 进程管理器接口
 * 定义进程管理相关的核心功能
 */
public interface ProcessManager {

    /**
     * 获取进程堆的起始地址
     * @param processId 进程ID
     * @return 堆的起始虚拟地址
     */
    VirtualAddress getHeapStart(int processId);

    /**
     * 获取进程堆的当前结束地址
     * @param processId 进程ID
     * @return 堆的结束虚拟地址
     */
    VirtualAddress getHeapEnd(int processId);

    /**
     * 获取进程栈的起始地址
     * @param processId 进程ID
     * @return 栈的起始虚拟地址
     */
    VirtualAddress getStackStart(int processId);

    /**
     * 获取进程栈的当前结束地址
     * @param processId 进程ID
     * @return 栈的结束虚拟地址
     */
    VirtualAddress getStackEnd(int processId);

    /**
     * 获取进程代码段的起始地址
     * @param processId 进程ID
     * @return 代码段的起始虚拟地址
     */
    VirtualAddress getCodeStart(int processId);

    /**
     * 获取进程代码段的结束地址
     * @param processId 进程ID
     * @return 代码段的结束虚拟地址
     */
    VirtualAddress getCodeEnd(int processId);

    /**
     * 获取进程数据段的起始地址
     * @param processId 进程ID
     * @return 数据段的起始虚拟地址
     */
    VirtualAddress getDataStart(int processId);

    /**
     * 获取进程数据段的结束地址
     * @param processId 进程ID
     * @return 数据段的结束虚拟地址
     */
    VirtualAddress getDataEnd(int processId);

    /**
     * 增长进程的堆
     * @param processId 进程ID
     * @param size 要增长的字节数
     * @return 是否增长成功
     */
    boolean growHeap(int processId, int size);

    /**
     * 收缩进程的堆
     * @param processId 进程ID
     * @param size 要收缩的字节数
     * @return 是否收缩成功
     */
    boolean shrinkHeap(int processId, int size);

    /**
     * 检查地址是否在进程的有效地址空间内
     * @param processId 进程ID
     * @param address 要检查的虚拟地址
     * @return 是否是有效地址
     */
    boolean isValidAddress(int processId, VirtualAddress address);

    /**
     * 进程退出时清理内存资源
     * @param processId 进程ID
     */
    void cleanupProcessMemory(int processId);

    /**
     * 获取进程的当前指令指针
     * @param processId 进程ID
     * @return 指令指针虚拟地址
     */
    VirtualAddress getCurrentInstructionPointer(int processId);

    /**
     * 获取进程的当前栈指针
     * @param processId 进程ID
     * @return 栈指针虚拟地址
     */
    VirtualAddress getCurrentStackPointer(int processId);

    /**
     * 创建进程
     * @param processName 进程名称
     * @param args 参数
     * @param instructions 指令
     * @return 进程信息
     */
    ProcessInfoReturnImplDTO createProcess(String processName, JSONObject args, String[] instructions);

    /**
     * 执行进程
     * @param pcb 进程控制块
     */
    void executeProcess(PCB pcb);

    /**
     * 向进程发送信号
     * @param pid 进程ID
     * @param signalName 信号名称
     * @param message 消息
     * @return 是否成功
     */
    boolean sendSignal(int pid, String signalName, String message);

    /**
     * 终止进程并清理其内存资源
     * @param pid 要终止的进程ID
     * @return 是否成功终止
     */
    boolean terminateProcess(int pid);

    /**
     * 获取进程的内存使用情况
     * @param pid 进程ID
     * @return 进程内存使用情况的描述字符串
     */
    String getProcessMemoryInfo(int pid);

    /**
     * 为进程分配额外内存
     * @param pid 进程ID
     * @param size 要分配的内存大小(字节)
     * @return 分配的虚拟地址，失败返回null
     */
    VirtualAddress allocateMemoryForProcess(int pid, long size);

    /**
     * 释放进程的内存
     * @param pid 进程ID
     * @param address 要释放的内存地址
     * @return 是否成功释放
     */
    boolean freeProcessMemory(int pid, VirtualAddress address);

    /**
     * 改变内存段的访问权限
     * @param pid 进程ID
     * @param address 内存地址
     * @param permissions 新的权限字符串 (如 "RW", "RX", "RWX")
     * @return 是否成功更改
     */
    boolean changeMemoryPermissions(int pid, VirtualAddress address, String permissions);

    /**
     * 检查进程是否存在
     * @param pid 进程ID
     * @return 进程是否存在
     */
    boolean isProcessExist(int pid);

    /**
     * 获取进程控制块
     * @param pid 进程ID
     * @return 进程控制块，不存在则返回null
     */
    PCB getProcess(int pid);
}