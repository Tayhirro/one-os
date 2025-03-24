package newOs.kernel.filesystem;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
@Data
public class FileNode {
    public enum FileType { FILE, DIRECTORY }


    private int id;

    private String fileName; //文件名

    private FileType fileType; //文件类型

    private List<Integer> blockNumbers; //文件存储块号

    private int size; //文件大小

    private FileNode parent; //父结点
    private List<FileNode> children; //孩子结点

    public FileNode(String fileName, FileType fileType) {
        this.id = InodeGenerator.allocate();
        this.fileName = fileName;
        this.fileType = fileType;
        this.blockNumbers = new ArrayList<>();
        this.size = 0;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    static class InodeGenerator {
        private static final AtomicInteger nextInode = new AtomicInteger(1);

        //分配文件标识符
        public static int allocate() {
            return nextInode.getAndIncrement();
        }

        //持久化当前最大值
        public static void initFromPersistedMax(int max) {
            nextInode.set(max + 1);
        }
    }


}
