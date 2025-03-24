package newOs.config;

import newOs.kernel.process.ProcessExecutionTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 进程组件配置
 * 负责注入ApplicationContext到ProcessExecutionTask类中
 */
@Configuration
public class ProcessComponentConfig {

    @Autowired
    private ApplicationContext applicationContext;
    
    @PostConstruct
    public void init() {
        // 设置ApplicationContext到ProcessExecutionTask
        ProcessExecutionTask.setApplicationContext(applicationContext);
    }
} 