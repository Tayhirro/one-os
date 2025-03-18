package newOs.component.cpu;


import lombok.Data;
import lombok.Generated;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;
import newOs.common.cpuConstant.RegisterType;
import newOs.component.cpu.ALU.ALU;
import newOs.component.cpu.MMU.MMU;
import newOs.component.cpu.Registers.AbstractRegister;
import newOs.component.cpu.Registers.AbstractRegisterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.relational.core.sql.In;
import org.springframework.stereotype.Component;




@Component
@Data
public class X86CPUSimulator {
    private final ExecutorService[] executors;
    private final List<AtomicInteger> executorServiceReady = Collections.synchronizedList(new ArrayList<>());


    private MMU mmu;
    //private Map<RegisterType, AbstractRegister> registers = new HashMap<>();
    //private ALU alu; 暂时没实现

    @Autowired
    public X86CPUSimulator(MMU mmu, AbstractRegisterFactory registerFactory) {
        this.mmu = mmu;
        //this.alu = alu;
        //创建寄存器组
        //registers = registerFactory.GenerateBaseConfig();

        this.executors = new ExecutorService[5];

        //模拟N核
        for(int i= 0; i <=4; i++) {
            this.executors[i] = Executors.newFixedThreadPool(1);        //实际上0不会被用到
            executorServiceReady.add(new AtomicInteger(0));         //添加计数
        }

    }
    public ExecutorService[] getExecutors() {
        return executors;
    }
}
