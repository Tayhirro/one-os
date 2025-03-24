package newOs.kernel.memory.virtual;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import newOs.exception.MemoryAllocationException;
import newOs.kernel.memory.PhysicalMemory;
import newOs.kernel.memory.virtual.paging.Page;
import newOs.kernel.memory.virtual.paging.PageFrame;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 页帧表类
 * 管理物理内存中的所有页帧
 */
@Component
@Data
@Slf4j
public class PageFrameTable {
    
    // 所有页帧列表
    private final List<PageFrame> frames;
    
    // 空闲页帧列表
    private final List<Integer> freeFrames;
    
    // 按帧号索引的页帧映射表
    private final Map<Integer, PageFrame> frameMap;
    
    // 按进程ID组织的页帧映射表
    private final Map<Integer, List<PageFrame>> processFrames;
    
    // 总页帧数
    private final int totalFrames;
    
    // 物理内存对象
    private final PhysicalMemory physicalMemory;
    
    /**
     * 构造页帧表
     * @param physicalMemory 物理内存对象
     * @param memorySize 物理内存大小（字节）
     */
    public PageFrameTable(
            PhysicalMemory physicalMemory,
            @Value("${memory.physical.size:268435456}") int memorySize) {
        
        this.physicalMemory = physicalMemory;
        this.totalFrames = memorySize / PageFrame.FRAME_SIZE;
        this.frames = new ArrayList<>(totalFrames);
        this.freeFrames = new ArrayList<>(totalFrames);
        this.frameMap = new HashMap<>(totalFrames);
        this.processFrames = new HashMap<>();
        
        // 创建所有页帧
        for (int i = 0; i < totalFrames; i++) {
            PageFrame frame = new PageFrame(i);
            frames.add(frame);
            frameMap.put(i, frame);
            freeFrames.add(i);
        }
        
        log.info("页帧表初始化完成，总页帧数: {}", totalFrames);
    }
    
    /**
     * 分配一个空闲页帧
     * @param pid 进程ID
     * @param pageNumber 页面号
     * @return 分配的页帧
     * @throws MemoryAllocationException 如果没有空闲页帧
     */
    public PageFrame allocateFrame(int pid, int pageNumber) throws MemoryAllocationException {
        if (freeFrames.isEmpty()) {
            throw new MemoryAllocationException("没有可用的空闲页帧", PageFrame.FRAME_SIZE, pid, "NO_FREE_FRAMES");
        }
        
        // 分配一个空闲页帧
        int frameNumber = freeFrames.remove(0);
        PageFrame frame = frameMap.get(frameNumber);
        
        // 标记为已分配
        frame.allocate(pid, pageNumber);
        
        // 清空页帧内容
        frame.clear(physicalMemory);
        
        // 记录进程的页帧
        processFrames.computeIfAbsent(pid, k -> new ArrayList<>()).add(frame);
        
        log.debug("为进程{}分配页帧: 帧号={}, 页号={}", pid, frameNumber, pageNumber);
        
        return frame;
    }
    
    /**
     * 释放指定页帧
     * @param frameNumber 页帧号
     * @return 是否成功释放
     */
    public boolean freeFrame(int frameNumber) {
        if (frameNumber < 0 || frameNumber >= totalFrames) {
            return false;
        }
        
        PageFrame frame = frameMap.get(frameNumber);
        if (!frame.isAllocated()) {
            return false; // 已经是空闲的
        }
        
        // 从进程映射表中移除
        int pid = frame.getPid();
        List<PageFrame> processFrameList = processFrames.get(pid);
        if (processFrameList != null) {
            processFrameList.remove(frame);
            if (processFrameList.isEmpty()) {
                processFrames.remove(pid);
            }
        }
        
        // 释放页帧
        frame.free();
        
        // 添加到空闲列表
        freeFrames.add(frameNumber);
        
        log.debug("释放页帧: 帧号={}", frameNumber);
        
        return true;
    }
    
    /**
     * 释放指定进程的所有页帧
     * @param pid 进程ID
     * @return 释放的页帧数量
     */
    public int freeProcessFrames(int pid) {
        List<PageFrame> processFrameList = processFrames.get(pid);
        if (processFrameList == null || processFrameList.isEmpty()) {
            return 0;
        }
        
        int count = processFrameList.size();
        
        // 创建一个副本，因为在释放过程中会修改原始列表
        List<PageFrame> framesToFree = new ArrayList<>(processFrameList);
        
        for (PageFrame frame : framesToFree) {
            freeFrame(frame.getFrameNumber());
        }
        
        log.debug("释放进程{}的所有页帧: {} 帧", pid, count);
        
        return count;
    }
    
    /**
     * 获取指定页帧
     * @param frameNumber 页帧号
     * @return 页帧对象，如果不存在则返回null
     */
    public PageFrame getFrame(int frameNumber) {
        if (frameNumber < 0 || frameNumber >= totalFrames) {
            return null;
        }
        
        return frameMap.get(frameNumber);
    }
    
    /**
     * 获取进程的所有页帧
     * @param pid 进程ID
     * @return 页帧列表
     */
    public List<PageFrame> getProcessFrames(int pid) {
        return processFrames.getOrDefault(pid, new ArrayList<>());
    }
    
    /**
     * 获取进程分配的页帧数量
     * @param pid 进程ID
     * @return 分配的页帧数量
     */
    public int getProcessFrameCount(int pid) {
        List<PageFrame> processFrameList = processFrames.get(pid);
        return processFrameList != null ? processFrameList.size() : 0;
    }
    
    /**
     * 获取空闲页帧数量
     * @return 空闲页帧数量
     */
    public int getFreeFrameCount() {
        return freeFrames.size();
    }
    
    /**
     * 获取已分配页帧数量
     * @return 已分配页帧数量
     */
    public int getAllocatedFrameCount() {
        return totalFrames - freeFrames.size();
    }
    
    /**
     * 获取页帧使用率
     * @return 使用率（0到1之间的值）
     */
    public double getFrameUsageRatio() {
        return (double) getAllocatedFrameCount() / totalFrames;
    }
    
    /**
     * 检查是否有足够的空闲页帧
     * @param requiredFrames 需要的页帧数
     * @return 是否有足够的空闲页帧
     */
    public boolean hasEnoughFreeFrames(int requiredFrames) {
        return freeFrames.size() >= requiredFrames;
    }
    
    /**
     * 获取指定进程、页面的页帧
     * @param pid 进程ID
     * @param pageNumber 页面号
     * @return 页帧对象，如果不存在则返回null
     */
    public PageFrame findFrame(int pid, int pageNumber) {
        List<PageFrame> processFrameList = processFrames.get(pid);
        if (processFrameList == null) {
            return null;
        }
        
        for (PageFrame frame : processFrameList) {
            if (frame.getPageNumber() == pageNumber) {
                return frame;
            }
        }
        
        return null;
    }
    
    /**
     * 锁定页帧，防止被置换
     * @param frameNumber 页帧号
     * @return 是否成功锁定
     */
    public boolean lockFrame(int frameNumber) {
        PageFrame frame = getFrame(frameNumber);
        if (frame == null || !frame.isAllocated()) {
            return false;
        }
        
        frame.lock();
        return true;
    }
    
    /**
     * 解锁页帧
     * @param frameNumber 页帧号
     * @return 是否成功解锁
     */
    public boolean unlockFrame(int frameNumber) {
        PageFrame frame = getFrame(frameNumber);
        if (frame == null) {
            return false;
        }
        
        frame.unlock();
        return true;
    }
    
    /**
     * 获取所有锁定的页帧数量
     * @return 锁定的页帧数量
     */
    public int getLockedFrameCount() {
        int count = 0;
        for (PageFrame frame : frames) {
            if (frame.isAllocated() && frame.isLocked()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 获取页帧表信息的字符串表示
     * @return 页帧表信息字符串
     */
    public String getFrameTableInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 页帧表信息 =====\n");
        sb.append(String.format("总页帧数: %d\n", totalFrames));
        sb.append(String.format("空闲页帧数: %d (%.2f%%)\n", 
                getFreeFrameCount(), 
                (double) getFreeFrameCount() / totalFrames * 100));
        sb.append(String.format("已分配页帧数: %d (%.2f%%)\n", 
                getAllocatedFrameCount(),
                getFrameUsageRatio() * 100));
        sb.append(String.format("锁定页帧数: %d\n", getLockedFrameCount()));
        
        sb.append("\n进程页帧分配情况:\n");
        for (Map.Entry<Integer, List<PageFrame>> entry : processFrames.entrySet()) {
            int pid = entry.getKey();
            List<PageFrame> processFrameList = entry.getValue();
            
            sb.append(String.format("  进程 %d: %d 帧\n", pid, processFrameList.size()));
        }
        
        return sb.toString();
    }
    
    /**
     * 获取已分配的页帧数
     * @return 已分配的页帧数
     */
    public int getUsedFrames() {
        return totalFrames - getFreeFrameCount();
    }
    
    /**
     * 获取空闲页帧数
     * @return 空闲页帧数
     */
    public int getFreeFrames() {
        return getFreeFrameCount();
    }
} 