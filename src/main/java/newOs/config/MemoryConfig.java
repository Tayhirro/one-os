package newOs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 内存相关配置
 */
@Configuration
public class MemoryConfig {

    @Value("${memory.physical.size:1073741824}")
    private int physicalMemorySize = 1073741824; // 默认1GB
    
    /**
     * 提供总内存大小的Bean
     * @return 总内存大小（字节）
     */
    @Bean
    public int totalMemorySize() {
        return physicalMemorySize;
    }
} 