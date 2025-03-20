package newOs.component.device;


import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Data
public class Disk {
    //定义一个线程池
    private final ExecutorService executor;

    public Disk() {
        this.executor = Executors.newFixedThreadPool(1);    //作为一个线程池来进行
    }
}
