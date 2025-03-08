package newOs.component.cpu.Registers;


import newOs.common.cpuConstant.RegisterType;
import newOs.component.cpu.Registers.AbstractRegisterFactory;
import newOs.component.cpu.Registers.RegisterImpl.X86GeneralRegister;
import newOs.component.cpu.Registers.RegisterImpl.X86ProgramCounter;
import newOs.component.cpu.Registers.RegisterImpl.X86SegmentRegister;
import newOs.component.cpu.Registers.RegisterImpl.X86StatusRegister;
import newOs.component.cpu.Registers.RegisterInterface.GeneralRegister;
import newOs.component.cpu.Registers.RegisterInterface.ProgramCounter;
import newOs.component.cpu.Registers.RegisterInterface.SegmentRegister;
import newOs.component.cpu.Registers.RegisterInterface.StatusRegister;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
@Primary    //优先注入
public class X86registerFactory implements AbstractRegisterFactory {    //x86刚刚好register32位
    @Override
    public GeneralRegister createGeneralRegister() {
        return new X86GeneralRegister();
    }

    @Override
    public SegmentRegister createSegmentRegister() {
        return new X86SegmentRegister();
    }

    @Override
    public ProgramCounter createProgramCounter() {
        return new X86ProgramCounter();
    }

    @Override
    public StatusRegister createStatusRegister() {
        return new X86StatusRegister();
    }

    @Override
    public Map<RegisterType,AbstractRegister> GenerateBaseConfig(){
        Map<RegisterType,AbstractRegister> X86RegisterMap = new HashMap<>();
        X86RegisterMap.put(RegisterType.GENERAL_REGISTER,new X86GeneralRegister().setName("eax"));
        X86RegisterMap.put(RegisterType.GENERAL_REGISTER,new X86GeneralRegister().setName("ebx"));
        X86RegisterMap.put(RegisterType.GENERAL_REGISTER,new X86GeneralRegister().setName("ecx"));
        X86RegisterMap.put(RegisterType.GENERAL_REGISTER,new X86GeneralRegister().setName("edx"));
        X86RegisterMap.put(RegisterType.GENERAL_REGISTER,new X86GeneralRegister().setName("esi"));
        X86RegisterMap.put(RegisterType.GENERAL_REGISTER,new X86GeneralRegister().setName("edi"));
        //ebp 为基址寄存器
        X86RegisterMap.put(RegisterType.GENERAL_REGISTER,new X86GeneralRegister().setName("ebp"));
        //esp 为栈指针寄存器
        X86RegisterMap.put(RegisterType.GENERAL_REGISTER,new X86GeneralRegister().setName("esp"));
        //PC
        //eip 为指令指针寄存器
        X86RegisterMap.put(RegisterType.PROGRAM_COUNTER,new X86ProgramCounter().setName("eip"));

        //SR
        //cs 为代码段寄存器
        X86RegisterMap.put(RegisterType.SEGMENT_REGISTER,new X86SegmentRegister().setName("cs"));
        //ds 为数据段寄存器
        X86RegisterMap.put(RegisterType.SEGMENT_REGISTER,new X86SegmentRegister().setName("ds"));
        //ss 为堆栈段寄存器
        X86RegisterMap.put(RegisterType.SEGMENT_REGISTER,new X86SegmentRegister().setName("ss"));
        //es 为附加段寄存器
        X86RegisterMap.put(RegisterType.SEGMENT_REGISTER,new X86SegmentRegister().setName("es"));
        //fs 为附加段寄存器
        X86RegisterMap.put(RegisterType.SEGMENT_REGISTER,new X86SegmentRegister().setName("fs"));
        //gs 为附加段寄存器
        X86RegisterMap.put(RegisterType.SEGMENT_REGISTER,new X86SegmentRegister().setName("gs"));
        //StatusR
        //eflags 为标志寄存器
        X86RegisterMap.put(RegisterType.STATUS_REGISTER,new X86StatusRegister().setName("eflags"));

        X86RegisterMap.put(RegisterType.MAR,new X86GeneralRegister().setName("MAR"));
        X86RegisterMap.put(RegisterType.MDR,new X86GeneralRegister().setName("MDR"));
        //ir寄存器  不能直接操作  ，同时由指令流水线进行控制

        return X86RegisterMap;
    }

}

