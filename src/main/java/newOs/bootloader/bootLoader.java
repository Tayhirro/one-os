package newOs.bootloader;

import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.cpu.X86CPUSimulator;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.kernel.process.scheduler.ProcessScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Component

public class bootLoader implements ApplicationRunner {
    // 注入核心组件
    private final X86CPUSimulator x86CPUSimulator;
    private final ProtectedMemory protectedMemory;
    private final ProcessScheduler processScheduler;

    @Autowired
    public bootLoader(X86CPUSimulator x86CPUSimulator, ProtectedMemory protectedMemory, ProcessScheduler processScheduler) {
        this.x86CPUSimulator = x86CPUSimulator;
        this.protectedMemory = protectedMemory;
        this.processScheduler = processScheduler;
    }


    @Override
    public void run(ApplicationArguments args) {
        // 通过 protectedMemory 访问共享资源
        ConcurrentHashMap<Long, InterruptRequestLine> irlTable =
                protectedMemory.getIrlTable();

        ExecutorService executor = x86CPUSimulator.getExecutor();

        // 初始化逻辑
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                long threadId = Thread.currentThread().getId();
                irlTable.put(threadId, new InterruptRequestLine());
                try {
                    Thread.sleep(10);
                    System.out.println("initializing" + Thread.currentThread().getId());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        // 调度逻辑
        for (int i = 0; i < 4; i++) {
            processScheduler.spanWait(executor);
        }
    }
}