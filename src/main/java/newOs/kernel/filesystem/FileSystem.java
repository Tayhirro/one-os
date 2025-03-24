package newOs.kernel.filesystem;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import newOs.kernel.memory.model.VirtualAddress;
import newOs.kernel.memory.model.MemorySegment;
import newOs.exception.MemoryException;
import newOs.service.MemoryFileSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.HashMap;

import static newOs.kernel.filesystem.FileNode.FileType.DIRECTORY;
import static newOs.kernel.filesystem.FileNode.FileType.FILE;


public class FileSystem {
    private static final FileReader fileReader = FileReader.getFileReader();
    //private static final FileWriter fileWriter = FileWriter.getInstance();

    @Getter
    private FileNode root;
    @Getter
    private FileNode current_node;
    private String current_path;
    private static FileSystem fileSystem;
    
    @Autowired
    private MemoryFileSystemService memoryFileSystemService;
    
    private Map<String, VirtualAddress> fileMemoryMap = new HashMap<>();
    
    private int cacheBlockSize = 4 * 1024;
    
    private long fileSystemBufferSize = 64 * 1024 * 1024; // 64MB
    
    private long allocatedMemorySize = 0;
    
    private long cacheHits = 0;
    private long cacheMisses = 0;

    private FileSystem() {
        root = new FileNode("/", DIRECTORY);
        current_node = root;
        current_path = "/";
        
        try {
            initializeFileSystemBuffer();
        } catch (Exception e) {
            System.err.println("初始化文件系统缓冲区失败: " + e.getMessage());
        }
    }

    public static FileSystem getFileSystem() {
        if (fileSystem == null) {
            synchronized (FileNode.class) {
                if (fileSystem == null) {
                    fileSystem = new FileSystem();
                }
            }
        }
        return fileSystem;
    }

    public String getCurrentPath() {
        return current_path;
    }


    /**
     * 找到文件路径对应的文件结点
     * @param filePath 文件路径
     * @return 文件名对应的文件结点
     */
    public FileNode NameToNode(String filePath) {
        if(filePath == null || filePath.isEmpty()) {
            return null;
        }

        //拆分路径并处理特殊字符
        List<String> parts = new ArrayList<>();
        boolean isAbsolute = filePath.startsWith("/");
        String[] segments = filePath.split("/");

        for (String seg : segments) {
            if (seg.equals(".") || seg.isEmpty()) {
                continue;
            }
            parts.add(seg);
        }

        FileNode currentNode = isAbsolute ? root : current_node;

        //处理根路径的特殊情况，例如"/"
        if (isAbsolute && parts.isEmpty() && filePath.equals("/")) {
            return root;
        }

        for (String part : parts) {
            if(part.equals("..")) {
                //如果当前结点不是根节点，则移动到父结点
                if(currentNode != root) {
                    FileNode parent = currentNode.getParent();
                    currentNode = (parent == null) ? root : parent;
                }
            } else {
                // 当前节点必须是目录才能继续查找
                if(currentNode.getFileType() != DIRECTORY) {
                    return null;
                }
                boolean found = false;
                for (FileNode child : currentNode.getChildren()) {
                    if (child.getFileName().equals(part)) {
                        currentNode = child;;
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    return null;
                }
            }
        }
        return currentNode;
    }

    /**
     * 创建一个文件
     * @param path 文件路径（可以是相对路径或绝对路径）
     * @return
     */
    public String touch(String path) {
        try {
            VirtualAddress fileAddress = allocateFileMemory(cacheBlockSize);
            
            fileMemoryMap.put(path, fileAddress);
            
            return "文件创建成功: " + path;
        } catch (MemoryException e) {
            return "创建文件失败 - 内存分配错误: " + e.getMessage();
        }
    }
    
    private void initializeFileSystemBuffer() throws MemoryException {
        if (memoryFileSystemService != null) {
            memoryFileSystemService.initializeFileSystemBuffer(fileSystemBufferSize);
        }
    }
    
    private VirtualAddress allocateFileMemory(long size) throws MemoryException {
        if (allocatedMemorySize + size > fileSystemBufferSize) {
            throw new MemoryException("文件系统内存缓冲区已满");
        }
        
        VirtualAddress address = memoryFileSystemService.allocateFileMemory(size);
        if (address != null) {
            allocatedMemorySize += size;
        }
        return address;
    }
    
    public byte[] readFileFromMemory(String filePath, int offset, int length) throws MemoryException {
        VirtualAddress fileAddress = fileMemoryMap.get(filePath);
        if (fileAddress == null) {
            cacheMisses++;
            fileAddress = loadFileToMemory(filePath);
            if (fileAddress == null) {
                throw new MemoryException("无法加载文件到内存: " + filePath);
            }
        } else {
            cacheHits++;
        }
        
        return memoryFileSystemService.readFileData(fileAddress, offset, length);
    }
    
    public void writeFileToMemory(String filePath, byte[] data, int offset) throws MemoryException {
        VirtualAddress fileAddress = fileMemoryMap.get(filePath);
        if (fileAddress == null) {
            fileAddress = allocateFileMemory(Math.max(data.length, cacheBlockSize));
            fileMemoryMap.put(filePath, fileAddress);
        }
        
        memoryFileSystemService.writeFileData(fileAddress, data, offset);
    }
    
    private VirtualAddress loadFileToMemory(String filePath) throws MemoryException {
        FileNode fileNode = NameToNode(filePath);
        if (fileNode == null || fileNode.getFileType() != FILE) {
            throw new MemoryException("文件不存在: " + filePath);
        }
        
        long fileSize = fileNode.getSize();
        if (fileSize == 0) {
            fileSize = cacheBlockSize;
        }
        
        VirtualAddress fileAddress = allocateFileMemory(fileSize);
        fileMemoryMap.put(filePath, fileAddress);
        
        return fileAddress;
    }
    
    public boolean releaseFileMemory(String filePath) {
        VirtualAddress address = fileMemoryMap.remove(filePath);
        if (address != null) {
            try {
                boolean result = memoryFileSystemService.freeFileMemory(address);
                if (result) {
                    allocatedMemorySize -= cacheBlockSize;
                    return true;
                }
            } catch (MemoryException e) {
                System.err.println("释放文件内存失败: " + e.getMessage());
            }
        }
        return false;
    }
    
    public Map<String, Object> getFileCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBufferSize", fileSystemBufferSize);
        stats.put("allocatedMemory", allocatedMemorySize);
        stats.put("freeMemory", fileSystemBufferSize - allocatedMemorySize);
        stats.put("cacheHits", cacheHits);
        stats.put("cacheMisses", cacheMisses);
        stats.put("hitRatio", calculateHitRatio());
        stats.put("cachedFiles", fileMemoryMap.size());
        return stats;
    }
    
    private double calculateHitRatio() {
        long totalAccesses = cacheHits + cacheMisses;
        return totalAccesses > 0 ? (double) cacheHits / totalAccesses : 0.0;
    }
    
    public int clearFileCache() {
        int count = fileMemoryMap.size();
        for (String filePath : new ArrayList<>(fileMemoryMap.keySet())) {
            releaseFileMemory(filePath);
        }
        fileMemoryMap.clear();
        allocatedMemorySize = 0;
        return count;
    }
}