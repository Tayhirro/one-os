package newOs.component.device;

/**
 * 设备接口
 * 定义设备的基本操作和属性
 */
public interface Device {
    
    /**
     * 获取设备实例
     * @return 设备实例
     */
    Device getDevice();
    
    /**
     * 获取设备总容量
     * @return 总容量（字节）
     */
    long getTotalCapacity();
    
    /**
     * 获取设备已用容量
     * @return 已用容量（字节）
     */
    long getUsedCapacity();
    
    /**
     * 传输数据
     * @param source 源地址
     * @param destination 目标地址
     * @param size 数据大小（字节）
     * @return 传输是否成功
     */
    boolean transferData(long source, long destination, long size);
} 