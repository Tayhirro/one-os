package newOs.kernel.filesystem;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.Getter;
import newOs.component.memory.protected1.PCB;
import newOs.kernel.DiskStorage.DeviceStorageManager.Device;
import newOs.kernel.DiskStorage.DeviceStorageManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import static newOs.kernel.filesystem.FileNode.FileType.DIRECTORY;
import static newOs.kernel.filesystem.FileNode.FileType.FILE;


@Data
@Component
public class FileReader {

    private final ConcurrentHashMap<FileNode, Semaphore> semaphoreTable;
    private final DeviceStorageManager deviceManager;
    private final Gson gson = new Gson();

    @Autowired
    public FileReader(DeviceStorageManager deviceStorageManager) {
        this.deviceManager = deviceStorageManager;

        semaphoreTable = new ConcurrentHashMap<>();
    }


    /**
     * 读取文件内容到输出流。
     * @param pcb 进程控制块，包含进程状态及剩余执行时间
     * @param filePath 待读取的文件（以路径形式给出）
     * @param output 从文件中读取的内容输出流
     * @return 是否读取成功
     */
    public boolean readFile(PCB pcb, String filePath, OutputStream output) {
        FileNode fileNode = FileSystem.NameToNode(filePath);
        if (fileNode == null || fileNode.getFileType() != FILE) {
            return false; // 文件不存在或路径指向目录
        }

        // 获取文件读写信号量（实现文件互斥访问）
        Semaphore fileSemaphore = semaphoreTable.computeIfAbsent(
                fileNode,
                k -> new Semaphore(1)
        );

        boolean semaphoreAcquired = false;

        //只是一个实现的假想，有方法就行。
        MemoryManager memoryManager = MemoryManager.getInstance();

        try {
            // 在进程剩余时间内尝试获取信号量
            while (pcb.getRemainingTime() > 0) {
                if (fileSemaphore.tryAcquire()) {
                    semaphoreAcquired = true;
                    System.out.printf("[DEBUG] 文件 %s 当前可用许可数: %d%n",
                            fileNode.getFileName(),
                            fileSemaphore.availablePermits()
                    );

                    // ==== 记录文件访问日志 ====
                    // AccessLogger.logAccess(pcb.getPid(), fileNode.getFilePath(), "READ");

                    // ==== 内存加载检查 ====
                    if (!memoryManager.isFileLoaded(fileNode)) {
                        System.out.printf("[Memory] 文件 %s 未加载，触发磁盘调入%n", fileNode.getFileName());
                        if (!memoryManager.loadFromDisk(fileNode)) {
                            return false; // 磁盘调入失败
                        }
                    }

                    // ==== 读取内存数据 ====
                    byte[] fileContent = memoryManager.readFileContent(fileNode);
                    if (fileContent == null) {
                        return false; // 内容读取失败
                    }

                    // ==== 写入输出流 ====
                    try {
                        output.write(fileContent);
                        output.flush();
                    } catch (IOException e) {
                        System.err.println("输出流写入异常: " + e.getMessage());
                        return false;
                    }

                    // ==== 更新内存元数据 ====
                    // memoryManager.updateLastAccessTime(fileNode, System.currentTimeMillis());

                    return true;
                }

                // ==== 进程时间调整 ====
                //pcb.adjustRemainingTime(-200); // 扣除等待耗时
                Thread.sleep(200); // 非忙等待

                if (pcb.getRemainingTime() <= 0) {
                    System.out.println("[WARN] 进程剩余时间不足，放弃读取");
                    return false;
                }
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("进程被中断: " + e.getMessage());
            return false;
        } finally {
            // ==== 资源清理 ====
            if (semaphoreAcquired) {
                fileSemaphore.release();
                // AccessLogger.cleanupAccessRecord(pcb.getPid());
            }
            try {
                output.close();
            } catch (IOException e) {
                System.err.println("输出流关闭异常: " + e.getMessage());
            }
        }
    }

    /**
     * 读取设备信息并填充到 JSONObject
     * @param deviceName 设备名称
     * @param output 用于接收设备信息的 JSONObject
     * @return 操作是否成功
     */
    public boolean readDevice(String deviceName, JSONObject output) {
        try {
            // 1. 查询设备信息
            Device device = deviceManager.getDeviceByName(deviceName);
            if (device == null) {
                output.put("error", "Device not found: " + deviceName);
                return false;
            }

            // 2. 填充基础字段
            output.put("deviceId", device.getDeviceId());
            output.put("deviceName", device.getDeviceName());
            output.put("deviceStatus", device.getDeviceStatus());

            // 3. 解析 deviceInfo 的 JSON 字符串
            String deviceInfoStr = device.getDeviceInfo();
            try {
                JSONObject deviceInfo = new Gson().fromJson(deviceInfoStr, JSONObject.class); // 解析 JSON
                output.put("deviceInfo", deviceInfo);
            } catch (JSONException e) {
                // JSON 解析失败时记录错误信息
                output.put("deviceInfo", "Invalid JSON: " + deviceInfoStr);
                output.put("warning", "Device info is not valid JSON");
            }

            return true;
        } catch (SQLException e) {
            try {
                output.put("error", "Database error: " + e.getMessage());
            } catch (JSONException jsonEx) {
                // 理论上此处不会触发，因 error 是有效键名
                jsonEx.printStackTrace();
            }
            return false;
        } catch (JSONException e) {
            try {
                output.put("error", "JSON operation failed: " + e.getMessage());
            } catch (JSONException jsonEx) {
                jsonEx.printStackTrace();
            }
            return false;
        }
    }





}

