package newOs.component.device;


import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Data
public class Disk implements Device {
    //定义一个线程池
    private final ExecutorService executor;
    // 磁盘总容量（默认1GB）
    private final long totalCapacity = 1024 * 1024 * 1024;
    // 已使用容量
    private long usedCapacity = 0;

    public Disk() {
        this.executor = Executors.newFixedThreadPool(1);    //作为一个线程池来进行
    }
    
    @Override
    public Device getDevice() {
        return this;
    }
    
    @Override
    public long getTotalCapacity() {
        return totalCapacity;
    }
    
    @Override
    public long getUsedCapacity() {
        return usedCapacity;
    }
    
    @Override
    public boolean transferData(long source, long destination, long size) {
        try {
            // 模拟数据传输
            Thread.sleep(size / 1024); // 假设每KB数据传输需要1毫秒
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
