package newOs.dto.req.Info;

import lombok.Data;
import newOs.common.InterruptConstant.InterruptType;
import newOs.kernel.memory.model.VirtualAddress;

import java.util.HashMap;
import java.util.Map;

/**
 * 内存中断信息类
 * 用于传递内存相关中断的具体信息
 */
@Data
public class MemoryInterruptInfo implements InterruptInfo {
    
    /**
     * 中断类型
     */
    private InterruptType interruptType;
    
    /**
     * 地址字符串表示
     */
    private String address;
    
    /**
     * 虚拟地址对象
     */
    private VirtualAddress virtualAddress;
    
    /**
     * 进程ID
     */
    private int processId;
    
    /**
     * 中断类型的字符串表示
     */
    private String type;
    
    /**
     * 错误消息
     */
    private String message;
    
    /**
     * 附加信息
     */
    private Map<String, Object> additionalInfo = new HashMap<>();
    
    /**
     * 添加附加信息
     * @param key 键
     * @param value 值
     */
    public void addAdditionalInfo(String key, Object value) {
        additionalInfo.put(key, value);
    }
    
    /**
     * 设置附加信息（兼容旧方法）
     * @param key 键
     * @param value 值
     */
    public void setAdditionalInfo(String key, Object value) {
        addAdditionalInfo(key, value);
    }
    
    /**
     * 获取附加信息
     * @param key 键
     * @return 值
     */
    public Object getAdditionalInfo(String key) {
        return additionalInfo.get(key);
    }
    
    @Override
    public InterruptType getInterruptType() {
        return interruptType;
    }
} 