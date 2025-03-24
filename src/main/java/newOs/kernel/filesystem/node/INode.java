package newOs.kernel.filesystem.node;

/**
 * 文件系统索引节点接口
 * 定义文件系统中基本节点的属性和操作
 */
public interface INode {
    
    /**
     * 获取节点ID
     * @return 节点ID
     */
    int getId();
    
    /**
     * 获取节点名称
     * @return 节点名称
     */
    String getName();
    
    /**
     * 获取节点类型
     * @return 节点类型
     */
    String getType();
    
    /**
     * 获取节点大小
     * @return 大小（字节）
     */
    long getSize();
    
    /**
     * 设置节点大小
     * @param size 大小（字节）
     */
    void setSize(long size);
    
    /**
     * 获取父节点
     * @return 父节点
     */
    INode getParent();
    
    /**
     * 设置父节点
     * @param parent 父节点
     */
    void setParent(INode parent);
} 