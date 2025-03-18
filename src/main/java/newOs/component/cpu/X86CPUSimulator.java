package newOs.component.cpu;


import lombok.Generated;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.RequiredArgsConstructor;
import newOs.common.cpuConstant.RegisterType;
import newOs.component.cpu.ALU.ALU;
import newOs.component.cpu.MMU.MMU;
import newOs.component.cpu.Registers.AbstractRegister;
import newOs.component.cpu.Registers.AbstractRegisterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;




@Component
public class X86CPUSimulator {
    private final ExecutorService[] executors;

    private MMU mmu;
    //private Map<RegisterType, AbstractRegister> registers = new HashMap<>();
    //private ALU alu; 暂时没实现

    @Autowired
    public X86CPUSimulator(MMU mmu, AbstractRegisterFactory registerFactory) {
        this.mmu = mmu;
        //this.alu = alu;
        //创建寄存器组
        //registers = registerFactory.GenerateBaseConfig();

        this.executors = new ExecutorService[4];

        //模拟N核
        for(int i= 0; i < 4; i++) {
            executors[i] = Executors.newFixedThreadPool(1);
        }

    }
    public ExecutorService[] getExecutors() {
        return executors;
    }
}
