package newOs.kernel.filesystem;

import newOs.component.memory.protected1.PCB;

import java.io.OutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import static newOs.kernel.filesystem.FileNode.FileType.DIRECTORY;
import static newOs.kernel.filesystem.FileNode.FileType.FILE;

/**
 * 文件读取器
 */
public class FileReader {
    private static FileReader fileReader;
    private final FileSystem fileSystem = FileSystem.getFileSystem();

    private final ConcurrentHashMap<FileNode, Semaphore> semaphoreTable;

    public FileReader() {
        semaphoreTable = new ConcurrentHashMap<>();
    }

    public static FileReader getFileReader() {
        if (fileReader == null) {
            synchronized (FileReader.class) {
                if (fileReader == null) {
                    fileReader = new FileReader();
                }
            }
        }
        return fileReader;
    }

    /**
     * 读取文件内容到输出流。
     * @param pcb 进程控制块，包含进程状态及剩余执行时间
     * @param filePath 待读取的文件（以路径形式给出）
     * @param output 从文件中读取的内容
     * @return 是否读取成功。
     */
    public boolean readFile(PCB pcb, String filePath, OutputStream output) {
        //将文件路径转换为对应的文件结点
        FileNode fileNode = fileSystem.NameToNode(filePath);
        if (fileNode == null || fileNode.getFileType() != FILE) {
            return false; // 文件不存在或路径指向目录
        }

        //获取文件关联的信号量（实现文件互斥访问）
        Semaphore fileSemaphore = semaphoreTable.computeIfAbsent(
                fileNode,
                k -> new Semaphore(1)
        );

        boolean semaphoreAcquired = false;
        try {
            //在进程剩余时间内循环尝试获取信号量
            while (pcb.getRemainingTime() > 0) {
                if(fileSemaphore.tryAcquire()) { //非阻塞尝试获取信号量
                    semaphoreAcquired = true;
                    System.out.printf("[DEBUG] 文件 %s 当前可用许可数: %d%n",
                            fileNode.getFileName(),
                            fileSemaphore.availablePermits()
                    );

                    //记录文件访问信息
                    //updateFileAccessRecord(pcb, fileNode);

                    //检查文件是否已加载到内存
                    //if (!memoryManager.isFileLoaded(fileNode)) 内存管理模块检查文件是否存在内存中
                    if(false) {
                        System.out.printf("[Memory] 文件 %s 未加载， 触发磁盘调入%n", fileNode.getFileName());
                        //memoryManager.loadFromDisk(fileNode);
                        //暂时不是很清楚这一步放在哪里操作。
                        return false;
                    }

                    //若在内存中，则从内存中读取文件内容
                    // byte[] fileContent = memoryManager.readFileContent(fileNode);
                    // output.write(fileContent);
                    return true;
                }
                // 未获取到信号量时的等待策略
                Thread.sleep(200);
                //adjustRemainingTime(pcb, 200);
            }
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            // 确保信号量释放和清洁工作
            if (semaphoreAcquired) {
                fileSemaphore.release();
                // cleanupFileAccessRecord(pcb, fileNode);
            }
        }
    }





}

