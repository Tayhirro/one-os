package newOs.kernel.filesystem;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import newOs.kernel.DiskStorage.BlockStorageManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static newOs.kernel.filesystem.FileNode.FileType.DIRECTORY;
import static newOs.kernel.filesystem.FileNode.FileType.FILE;

@Data
@Component
public class FileSystem {

    private final FileReader fileReader;
    private final FileWriter fileWriter;
    private final BlockStorageManager blockManager;

    private static FileNode root;
    private static FileNode current_node;
    private static String current_path;


    @Autowired
    public FileSystem(FileReader fileReader, FileWriter fileWriter, BlockStorageManager blockManager) {
        this.fileReader = fileReader;
        this.fileWriter = fileWriter;
        this.blockManager = blockManager;
        root = new FileNode("/", DIRECTORY);
        current_node = root;
        current_path = "/";
    }


    /**
     * 创建文件并分配存储块（若路径不存在则失败）
     * @param path 文件路径（相对路径或绝对路径）
     * @return 操作结果信息
     */
    public String touch(String path) {
        try {
            // 解析路径的父目录和文件名
            int lastSlash = path.lastIndexOf('/');
            String parentPath = (lastSlash == -1) ? current_path : path.substring(0, lastSlash);
            String fileName = (lastSlash == -1) ? path : path.substring(lastSlash + 1);

            // 查找父目录结点
            FileNode parentNode = NameToNode(parentPath);
            if(parentNode == null || parentNode.getFileType() != DIRECTORY) {
                return "Error: Parent directory does not exist.";
            }

            // 检查文件是否已经存在
            for (FileNode child : parentNode.getChildren()) {
                if (child.getFileName().equals(fileName)) {
                    return "Error: File already exists.";
                }
            }


            // 分配inode块
            BlockStorageManager.Block inodeBlock = blockManager.findFirstUnusedBlock();
            if(inodeBlock == null) return "Error: No free blocks for inode.";
            blockManager.updateBlockUsage(inodeBlock.getBlockNumber(), true);

            // 分配内容块
            BlockStorageManager.Block contentBlock = blockManager.findFirstUnusedBlock();
            if(contentBlock == null) {
                // 回滚inode块分配
                blockManager.updateBlockUsage(inodeBlock.getBlockNumber(), false);
                return "Error: No free blocks for content.";
            }
            blockManager.updateBlockUsage(contentBlock.getBlockNumber(), true);

            // 创建文件结点并设置块号
            FileNode newFile = new FileNode(fileName, FILE);
            newFile.setParent(parentNode);
            newFile.getBlockNumbers().add(inodeBlock.getBlockNumber()); // 添加inode块
            newFile.getBlockNumbers().add(contentBlock.getBlockNumber()); // 添加内容块

            // 写入inode基本信息
            JSONObject inodeData = new JSONObject();
            inodeData.put("filename", fileName);
            inodeData.put("create_time", System.currentTimeMillis());
            inodeData.put("file_type", "FILE");
            blockManager.updateBlockData(inodeBlock.getBlockNumber(), inodeData.toString());

            // 设置块链表关系
            blockManager.updateNextBlock(inodeBlock.getBlockNumber(), contentBlock.getBlockNumber());

            // 添加到父目录
            parentNode.getChildren().add(newFile);

            // 信号量初始化
            fileReader.getSemaphoreTable().put(newFile, new Semaphore(3));
            fileWriter.getSemaphoreTable().put(newFile, new Semaphore(1));

            return "File created: " + path + " [Blocks: " + inodeBlock.getBlockNumber() + "->" + contentBlock.getBlockNumber() + "]";
        } catch (SQLException e) {
            return "Error: Database operation failed - " + e.getMessage();
        } catch (JSONException e) {
            return "Error: JSON serialization failed - " + e.getMessage();
        }
    }


    /**
     * 创建目录（若路径不存在则失败）
     * @param path 目录路径（绝对或相对路径）
     * @return 操作结果信息
     */
    public String makedir(String path) {
        // 解析路径的父目录和目录名
        int lastSlash = path.lastIndexOf("/");
        String parentPath = (lastSlash == -1) ? current_path : path.substring(0, lastSlash);
        String dirName = (lastSlash == -1) ? path : path.substring(lastSlash + 1);

        // 查找父目录节点
        FileNode parentNode = NameToNode(parentPath);
        if (parentNode == null || parentNode.getFileType() != DIRECTORY) {
            return "Error: Parent directory does not exist";
        }

        // 检查目录是否已存在
        for (FileNode child : parentNode.getChildren()) {
            if (child.getFileName().equals(dirName)) {
                return "Error: Directory already exists";
            }
        }

        // 创建新目录节点并添加到父目录
        FileNode newDir = new FileNode(dirName, DIRECTORY);
        newDir.setParent(parentNode);
        parentNode.getChildren().add(newDir);
        return "Directory created: " + path;
    }


    /**
     * cd命令，跳转到对应目录
     * @param path 目标路径目录
     * @return 操作结果信息
     */
    public String cd(String path) {
        FileNode targetNode = NameToNode(path);
        if(targetNode == null) {
            return "Error: Directory does not exist.";
        }
        if(targetNode.getFileType() != DIRECTORY) {
            return "Error: Not a directory.";
        }

        // 更新当前结点和路径
        current_node = targetNode;
        current_path = buildPath(targetNode);

        return "Current directory: " + current_path;
    }

    // 辅助方法，根据节点生成完整路径
    private String buildPath(FileNode node) {
        if(node == root) return "/";
        List<String> parts = new ArrayList<>();
        while (node != root) {
            parts.add(node.getFileName());
            node = node.getParent();
        }
        return "/" + String.join("/", parts);
    }

    /**
     * 展示当前目录下的文件和子目录
     * @return 目录内容列表或错误信息
     */
    public String ls() {
        if (current_node.getFileType() != DIRECTORY) {
            return "Error: Not a directory.";
        }

        StringBuilder sb = new StringBuilder();
        for (FileNode child : current_node.getChildren()) {
            sb.append(child.getFileType() == DIRECTORY ? "[DIR] " : "[FILE] ");
            sb.append(child.getFileName()).append("\n");
        }
        return !sb.isEmpty() ? sb.toString() : "Empty Directory";
    }


    /**
     * 展示文件内容
     * @param path 文件路径
     * @return 文件内容或错误信息
     */
    public String cat(String path) {
        FileNode fileNode = NameToNode(path);
        if (fileNode == null) {
            return "Error: File does not exist.";
        }
        if (fileNode.getFileType() != FILE) {
            return "Error: Not a file.";
        }

        // 获取块存储管理器
        BlockStorageManager blockManager = new BlockStorageManager();
        StringBuilder contentBuilder = new StringBuilder();

        try {
            // 获取文件关联的块号列表（跳过第一个inode块）
            List<Integer> blockNumbers = fileNode.getBlockNumbers();
            if (blockNumbers.size() < 2) {
                return "Error: File has no content blocks";
            }

            // 遍历内容块（从第二个块开始）
            for (int i = 1; i < blockNumbers.size(); i++) {
                int blockNumber = blockNumbers.get(i);
                BlockStorageManager.Block block = blockManager.getBlockByNumber(blockNumber);

                if (block == null) {
                    return "Error: Missing block " + blockNumber;
                }
                if (!block.isUsed()) {
                    return "Error: Block " + blockNumber + " is unallocated";
                }

                // 拼接块内容
                contentBuilder.append(block.getData());
            }

            return contentBuilder.toString();

        } catch (SQLException e) {
            return "Error: Database failure - " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }


    /**
     * 删除空目录
     * @param path 目录路径
     * @return 操作结果信息
     */
    public String rmdir(String path) {
        FileNode dirNode = NameToNode(path);
        if (dirNode == null) {
            return "Error: Directory does not exist.";
        }
        if(dirNode.getFileType() != DIRECTORY) {
            return "Error: Not a directory.";
        }
        if (dirNode.getChildren().isEmpty()) {
            return "Error: Directory is not empty.";
        }

        //从父目录删除
        FileNode parent = dirNode.getParent();
        parent.getChildren().remove(dirNode);
        return "Directory removed. ";
    }


    /**
     * 删除文件并释放存储空间
     * @param path 文件路径
     * @return 操作结果信息
     */
    public String rmfile(String path) {
        FileNode fileNode = NameToNode(path);
        if (fileNode == null) {
            return "Error: File does not exist.";
        }
        if (fileNode.getFileType() != FILE) {
            return "Error: Not a file.";
        }

        BlockStorageManager blockManager = new BlockStorageManager();
        List<Integer> blockNumbers = fileNode.getBlockNumbers();

        try {
            // 获取数据库连接并开启事务
            blockManager.releaseBlock(blockNumbers);

            // 从父目录移除文件结点
            FileNode parent = fileNode.getParent();
            parent.getChildren().remove(fileNode);

            return "File removed. Released blocks: " + blockNumbers;

        } catch (SQLException e) {
            return "Error: Database operation failed - " + e.getMessage();
        }
    }


    /**
     * 生成目录树结构
     * @param path 起始路径
     * @return 目录树字符串表示
     */
    public String file_tree(String path) {
        FileNode startNode = NameToNode(path);
        if (startNode == null) {
            return "Error: Path does not exist.";
        }
        return buildTree(startNode, 0);
    }

    // 辅助方法，递归生成目录树
    private String buildTree(FileNode node, int depth) {
        StringBuilder sb = new StringBuilder();
        String indent = "  ".repeat(depth);
        sb.append(indent).append("|- ").append(node.getFileName())
                .append(node.getFileType() == DIRECTORY ? "/" : "").append("\n");

        if (node.getFileType() == DIRECTORY) {
            for (FileNode child : node.getChildren()) {
                sb.append(buildTree(child, depth + 1));
            }
        }
        return sb.toString();
    }

    /**
     * 找到文件路径对应的文件结点
     * @param filePath 文件路径
     * @return 文件名对应的文件结点
     */
    public static FileNode NameToNode(String filePath) {
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
                        currentNode = child;
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

}