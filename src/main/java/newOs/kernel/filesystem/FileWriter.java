package newOs.kernel.filesystem;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import newOs.kernel.DiskStorage.BlockStorageManager;
import newOs.kernel.DiskStorage.DeviceStorageManager.Device;
import newOs.kernel.DiskStorage.DeviceStorageManager;
import newOs.component.memory.protected1.PCB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import static newOs.kernel.DiskStorage.BlockStorageManager.Block.BLOCK_SIZE;
import static newOs.kernel.filesystem.FileNode.FileType.DIRECTORY;
import static newOs.kernel.filesystem.FileNode.FileType.FILE;

@Data
@Component
public class FileWriter {

    private final ConcurrentHashMap<FileNode, Semaphore> semaphoreTable;
    private final DeviceStorageManager deviceStorageManager;

    @Autowired
    public FileWriter(DeviceStorageManager deviceStorageManager) {
        this.deviceStorageManager = deviceStorageManager;

        semaphoreTable = new ConcurrentHashMap<>();
    }

    /**
     * 写入内容到指定文件
     * @param pcb 进程控制块
     * @param filePath 文件路径
     * @param input 要写入的内容输入流
     * @return 是否写入成功
     */
    public boolean writeToFile(PCB pcb, String filePath, InputStream input) {
        FileNode fileNode = FileSystem.NameToNode(filePath);
        if (fileNode == null || fileNode.getFileType() != FILE) {
            return false;
        }

        Semaphore writeSemaphore = semaphoreTable.computeIfAbsent(
                fileNode,
                k -> new Semaphore(1)
        );

        boolean semaphoreAcquired = false;
        BlockStorageManager blockManager = new BlockStorageManager();

        //只是一个实现的假想，有方法就行。
        MemoryManager memoryManager = MemoryManager.getInstance();

        try {
            // 尝试获取写信号量
            while (pcb.getRemainingTime() > 0) {
                if (writeSemaphore.tryAcquire()) {
                    semaphoreAcquired = true;
                    System.out.printf("[DEBUG] 获取文件 %s 写锁，剩余许可: %d%n",
                            fileNode.getFileName(),
                            writeSemaphore.availablePermits());

                    // 检查内存加载状态
                    boolean isLoaded = memoryManager.isFileLoaded(fileNode);
                    byte[] content = input.readAllBytes();
                    int bytesWritten = 0;

                    if (isLoaded) {
                        // 在内存里写入文件内容
                        byte[] existing = memoryManager.getFileContent(fileNode);
                        byte[] newContent = mergeContent(existing, content);
                        memoryManager.writeFileContent(fileNode, newContent);
                        bytesWritten = newContent.length;
                    } else {
                        // 磁盘直接写入流程
                        List<Integer> blockList = fileNode.getBlockNumbers();
                        int currentBlockIndex = 1; // 从内容块开始
                        int currentBlockNumber = blockList.get(currentBlockIndex);

                        while (bytesWritten < content.length) {
                            BlockStorageManager.Block currentBlock = blockManager.getBlockByNumber(currentBlockNumber);
                            int remainingSpace = BLOCK_SIZE - (currentBlock.getData().length());

                            // 当前块剩余空间足够
                            if (remainingSpace >= content.length - bytesWritten) {
                                String newData = currentBlock.getData() +
                                        new String(content, bytesWritten, content.length - bytesWritten);
                                blockManager.updateBlockData(currentBlockNumber, newData);
                                bytesWritten = content.length;
                            } else {
                                // 分配新块
                                BlockStorageManager.Block newBlock = blockManager.findFirstUnusedBlock();
                                if (newBlock == null) return false;

                                // 更新块关系
                                blockManager.updateBlockUsage(newBlock.getBlockNumber(), true);
                                blockManager.updateNextBlock(currentBlockNumber, newBlock.getBlockNumber());

                                // 写入部分数据
                                String partialData = new String(content, bytesWritten, remainingSpace);
                                blockManager.updateBlockData(currentBlockNumber,
                                        currentBlock.getData() + partialData);

                                bytesWritten += remainingSpace;
                                currentBlockNumber = newBlock.getBlockNumber();
                                blockList.add(currentBlockNumber);
                            }
                        }

                        // 调入修改后的块到内存
                        memoryManager.loadFromDisk(fileNode);
                    }

                    // 同步写回磁盘
                    if (isLoaded) {
                        syncMemoryToDisk(fileNode, blockManager, memoryManager);
                    }

                    return true;
                }
                Thread.sleep(200);
                // adjustRemainingTime(pcb, 200);
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (semaphoreAcquired) {
                writeSemaphore.release();
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private byte[] mergeContent(byte[] existing, byte[] newContent) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(existing);
            outputStream.write(newContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    // 辅助方法：内存数据同步到磁盘
    private void syncMemoryToDisk(FileNode fileNode, BlockStorageManager blockManager, MemoryManager memoryManager) throws SQLException {
        byte[] content = memoryManager.readFileContent(fileNode);
        List<Integer> blocks = fileNode.getBlockNumbers();

        int contentIndex = 0;
        for (int i = 1; i < blocks.size(); i++) { // 跳过inode块
            int blockNumber = blocks.get(i);
            int chunkSize = Math.min(BLOCK_SIZE, content.length - contentIndex);
            String chunk = new String(content, contentIndex, chunkSize);
            blockManager.updateBlockData(blockNumber, chunk);
            contentIndex += chunkSize;
        }
    }


    /**
     * 向指定设备写入JSON数据
     * @param deviceName 设备名称
     * @param data 要写入的JSON数据
     * @return 操作结果信息（成功或错误原因）
     */
    public String writeToDevice(String deviceName, JSONObject data) {
        try {
            // 1. 查询设备是否存在
            Device device = deviceStorageManager.getDeviceByName(deviceName);
            if (device == null) {
                return "Error: Device \"" + deviceName + "\" not found";
            }

            // 2. 转换JSON数据为字符串
            String infoJson = data.toJSONString();

            // 3. 更新设备信息字段
            deviceStorageManager.updateDeviceInfo(device.getDeviceId(), infoJson);

            return "Success: Data written to device \"" + deviceName + "\"";
        } catch (Exception e) {
            return "Error: Database operation failed -  " + e.getMessage();
        }
    }






}
