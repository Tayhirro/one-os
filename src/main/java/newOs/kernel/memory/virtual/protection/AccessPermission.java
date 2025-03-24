package newOs.kernel.memory.virtual.protection;

/**
 * 内存访问权限枚举
 * 定义了不同的内存访问权限级别
 */
public enum AccessPermission {
    
    /**
     * 无权限
     */
    NONE(false, false, false),
    
    /**
     * 只读权限
     */
    READ(true, false, false),
    
    /**
     * 读写权限
     */
    READ_WRITE(true, true, false),
    
    /**
     * 执行权限
     */
    EXECUTE(false, false, true),
    
    /**
     * 读取和执行权限
     */
    READ_EXECUTE(true, false, true),
    
    /**
     * 读取、写入和执行权限
     */
    READ_WRITE_EXECUTE(true, true, true);
    
    private final boolean canRead;
    private final boolean canWrite;
    private final boolean canExecute;
    
    /**
     * 构造函数
     * @param canRead 是否可读
     * @param canWrite 是否可写
     * @param canExecute 是否可执行
     */
    AccessPermission(boolean canRead, boolean canWrite, boolean canExecute) {
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.canExecute = canExecute;
    }
    
    /**
     * 检查是否有读权限
     * @return 是否可读
     */
    public boolean canRead() {
        return canRead;
    }
    
    /**
     * 检查是否有写权限
     * @return 是否可写
     */
    public boolean canWrite() {
        return canWrite;
    }
    
    /**
     * 检查是否有执行权限
     * @return 是否可执行
     */
    public boolean canExecute() {
        return canExecute;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (canRead) sb.append("R");
        if (canWrite) sb.append("W");
        if (canExecute) sb.append("X");
        if (sb.length() == 0) sb.append("NONE");
        return sb.toString();
    }
} 