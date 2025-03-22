package newOs.kernel.filesystem;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

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

    private FileSystem() {
        root = new FileNode("/", DIRECTORY);
        current_node = root;
        current_path = "/";
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
        return "" ;
    }



}