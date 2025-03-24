package newOs.kernel.memory.virtual.paging;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import newOs.exception.PageFaultException;
import newOs.kernel.memory.PhysicalMemory;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.virtual.PageFrameTable;
import newOs.kernel.memory.virtual.PageTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 交换空间管理器
 * 负责页面的换入换出操作
 */
@Component
@Data
@Slf4j
public class SwapManager {
    
    // 交换文件路径
    private final String swapFilePath;
    
    // 交换文件大小（字节）
    private final long swapFileSize;
    
    // 交换文件
    private RandomAccessFile swapFile;
    
    // 文件通道，用于高效IO
    private FileChannel swapChannel;
    
    // 页面大小，与物理页面大小相同
    private final int pageSize = 4096; // 4KB页面大小
    
    // 可用的交换区域列表，键为起始位置
    private final Map<Long, Integer> freeSwapAreas;
    
    // 已分配的交换区域，键为进程ID和页面标识，值为在交换文件中的位置
    private final Map<String, Long> allocatedSwapAreas;
    
    // 下一个可用的交换位置
    private final AtomicLong nextSwapPosition;
    
    // 用于读写的缓冲区
    private final ByteBuffer pageBuffer;
    
    // 页表对象
    private final PageTable pageTable;
    
    // 页帧表对象
    private final PageFrameTable pageFrameTable;
    
    // 物理内存对象
    private final PhysicalMemory physicalMemory;
    
    // 统计：换出次数
    private long swapOutCount = 0;
    
    // 统计：换入次数
    private long swapInCount = 0;
    
    /**
     * 构造交换空间管理器
     * @param swapFilePath 交换文件路径
     * @param swapFileSize 交换文件大小（字节）
     * @param pageTable 页表对象
     * @param pageFrameTable 页帧表对象
     * @param physicalMemory 物理内存对象
     * @throws IOException 如果交换文件操作失败
     */
    public SwapManager(
            @Value("${memory.swap.file:./swap.bin}") String swapFilePath,
            @Value("${memory.swap.size:536870912}") long swapFileSize,
            PageTable pageTable,
            PageFrameTable pageFrameTable,
            PhysicalMemory physicalMemory) throws IOException {
        
        this.swapFilePath = swapFilePath;
        this.swapFileSize = swapFileSize;
        this.pageTable = pageTable;
        this.pageFrameTable = pageFrameTable;
        this.physicalMemory = physicalMemory;
        
        this.freeSwapAreas = new HashMap<>();
        this.allocatedSwapAreas = new HashMap<>();
        this.nextSwapPosition = new AtomicLong(0);
        this.pageBuffer = ByteBuffer.allocateDirect(pageSize);
        
        initSwapFile();
        
        log.info("交换空间管理器初始化完成，交换文件路径: {}, 大小: {} MB", 
                swapFilePath, swapFileSize / (1024 * 1024));
    }
    
    /**
     * 初始化交换文件
     * @throws IOException 如果文件操作失败
     */
    private void initSwapFile() throws IOException {
        File file = new File(swapFilePath);
        
        // 确保父目录存在
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // 创建或打开交换文件
        this.swapFile = new RandomAccessFile(file, "rw");
        
        // 设置文件大小
        this.swapFile.setLength(swapFileSize);
        
        // 获取文件通道
        this.swapChannel = swapFile.getChannel();
        
        // 初始化自由空间列表
        this.freeSwapAreas.put(0L, (int)(swapFileSize / pageSize));
    }
    
    /**
     * 关闭交换文件
     */
    public void close() {
        try {
            if (swapChannel != null && swapChannel.isOpen()) {
                swapChannel.close();
            }
            
            if (swapFile != null) {
                swapFile.close();
            }
            
            log.info("交换文件已关闭");
        } catch (IOException e) {
            log.error("关闭交换文件时发生错误", e);
        }
    }
    
    /**
     * 分配交换区域
     * @param pid 进程ID
     * @param pageNumber 页号
     * @return 分配的交换区域位置
     */
    public long allocateSwapArea(int pid, int pageNumber) {
        String key = getSwapKey(pid, pageNumber);
        
        // 检查是否已分配
        Long existingPosition = allocatedSwapAreas.get(key);
        if (existingPosition != null) {
            return existingPosition;
        }
        
        // 寻找一个空闲区域
        Long position = null;
        Integer size = null;
        
        // 查找最小的足够大的空闲区域
        for (Map.Entry<Long, Integer> entry : freeSwapAreas.entrySet()) {
            if (entry.getValue() >= 1) { // 需要1个页面大小
                position = entry.getKey();
                size = entry.getValue();
                break;
            }
        }
        
        // 如果没有找到足够大的空闲区域，使用文件末尾
        if (position == null) {
            position = nextSwapPosition.getAndAdd(pageSize);
            
            // 检查交换文件是否已满
            if (position >= swapFileSize) {
                log.error("交换文件已满");
                return -1;
            }
        } else {
            // 从空闲列表中移除已分配的区域
            freeSwapAreas.remove(position);
            
            // 如果有剩余空间，添加回空闲列表
            if (size > 1) {
                freeSwapAreas.put(position + pageSize, size - 1);
            }
        }
        
        // 记录分配
        allocatedSwapAreas.put(key, position);
        
        return position;
    }
    
    /**
     * 释放交换区域
     * @param pid 进程ID
     * @param pageNumber 页号
     * @return 是否成功释放
     */
    public boolean freeSwapArea(int pid, int pageNumber) {
        String key = getSwapKey(pid, pageNumber);
        Long position = allocatedSwapAreas.remove(key);
        
        if (position == null) {
            return false;
        }
        
        // 添加到空闲列表
        freeSwapAreas.put(position, 1);
        
        // 合并相邻的空闲区域
        mergeAdjacentFreeAreas();
        
        return true;
    }
    
    /**
     * 合并相邻的空闲区域
     */
    private void mergeAdjacentFreeAreas() {
        boolean merged;
        do {
            merged = false;
            
            for (Map.Entry<Long, Integer> entry : freeSwapAreas.entrySet()) {
                long position = entry.getKey();
                int size = entry.getValue();
                long nextPosition = position + size * pageSize;
                
                if (freeSwapAreas.containsKey(nextPosition)) {
                    int nextSize = freeSwapAreas.get(nextPosition);
                    
                    // 合并两个区域
                    freeSwapAreas.put(position, size + nextSize);
                    freeSwapAreas.remove(nextPosition);
                    
                    merged = true;
                    break;
                }
            }
        } while (merged);
    }
    
    /**
     * 将页面换出到交换空间
     * @param pid 进程ID
     * @param pageNumber 页号
     * @param frameNumber 页帧号
     * @return 是否成功换出
     */
    public boolean swapOutPage(int pid, int pageNumber, int frameNumber) {
        try {
            // 分配交换区域
            long swapLocation = allocateSwapArea(pid, pageNumber);
            if (swapLocation == -1) {
                return false;
            }
            
            // 准备缓冲区
            pageBuffer.clear();
            
            // 从物理内存读取页面内容
            PhysicalAddress frameAddress = new PhysicalAddress(frameNumber * pageSize);
            physicalMemory.readBlock(frameAddress, pageBuffer.array(), 0, pageSize);
            
            // 写入交换文件
            swapChannel.position(swapLocation);
            swapChannel.write(pageBuffer);
            
            // 更新统计信息
            swapOutCount++;
            
            return true;
        } catch (IOException e) {
            log.error("换出页面失败: pid={}, page={}, frame={}", pid, pageNumber, frameNumber, e);
            return false;
        }
    }
    
    /**
     * 从交换空间换入页面
     * @param pid 进程ID
     * @param frameNumber 页帧号
     * @return 是否成功换入
     */
    public boolean swapIn(long pid, int frameNumber) {
        try {
            // 获取页面在交换空间中的位置
            String key = getSwapKey((int)pid, frameNumber);
            Long swapLocation = allocatedSwapAreas.get(key);
            
            if (swapLocation == null) {
                return false;
            }
            
            // 准备缓冲区
            pageBuffer.clear();
            
            // 从交换文件读取页面内容
            swapChannel.position(swapLocation);
            swapChannel.read(pageBuffer);
            
            // 写入物理内存
            PhysicalAddress frameAddress = new PhysicalAddress(frameNumber * pageSize);
            physicalMemory.writeBlock(frameAddress, pageBuffer.array(), 0, pageSize);
            
            // 更新统计信息
            swapInCount++;
            
            return true;
        } catch (IOException e) {
            log.error("换入页面失败: pid={}, frame={}", pid, frameNumber, e);
            return false;
        }
    }
    
    /**
     * 获取交换键
     * @param pid 进程ID
     * @param pageNumber 页号
     * @return 交换键
     */
    private String getSwapKey(int pid, int pageNumber) {
        return String.format("%d:%d", pid, pageNumber);
    }
    
    /**
     * 获取换出次数
     * @return 换出次数
     */
    public long getSwapOutCount() {
        return swapOutCount;
    }
    
    /**
     * 获取换入次数
     * @return 换入次数
     */
    public long getSwapInCount() {
        return swapInCount;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        swapOutCount = 0;
        swapInCount = 0;
    }
    
    /**
     * 获取统计信息
     * @return 统计信息字符串
     */
    public String getStats() {
        return String.format("交换统计: 换出=%d, 换入=%d", swapOutCount, swapInCount);
    }
} 