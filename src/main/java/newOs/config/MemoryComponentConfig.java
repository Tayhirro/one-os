package newOs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import newOs.kernel.memory.PhysicalMemory;
import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.allocation.MemoryReclaimer;
import newOs.component.memory.protected1.ProtectedMemory;

/**
 * 内存组件配置类
 * 用于手动管理可能存在循环依赖的内存组件
 */
@Configuration
public class MemoryComponentConfig {
    
    @Bean
    public ProtectedMemory protectedMemory() {
        return new ProtectedMemory();
    }
    
    @Bean
    public MemoryManager memoryManager() {
        return new MemoryManager();
    }
} 