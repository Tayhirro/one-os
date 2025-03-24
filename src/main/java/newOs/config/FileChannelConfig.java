package newOs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * 文件通道配置类
 * 提供交换文件通道的 bean
 */
@Configuration
public class FileChannelConfig {

    /**
     * 创建并提供 FileChannel bean 供 WriteBackController 使用
     * @param swapFilePath 交换文件路径
     * @return FileChannel 实例
     * @throws IOException 如果创建或打开文件失败
     */
    @Bean
    @Primary
    public FileChannel swapFileChannel(
            @Value("${memory.swap.file:./swap.bin}") String swapFilePath) throws IOException {
        
        // 确保目录存在
        File swapFile = new File(swapFilePath);
        File parentDir = swapFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // 以读写模式打开文件
        RandomAccessFile randomAccessFile = new RandomAccessFile(swapFile, "rw");
        
        // 确保文件足够大
        if (randomAccessFile.length() == 0) {
            randomAccessFile.setLength(1024 * 1024 * 512); // 默认 512MB
        }
        
        return randomAccessFile.getChannel();
    }
} 