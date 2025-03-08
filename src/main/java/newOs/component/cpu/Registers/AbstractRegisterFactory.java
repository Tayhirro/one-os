package newOs.component.cpu.Registers;


import newOs.common.cpuConstant.RegisterType;
import newOs.component.cpu.Registers.RegisterInterface.GeneralRegister;
import newOs.component.cpu.Registers.RegisterInterface.ProgramCounter;
import newOs.component.cpu.Registers.RegisterInterface.SegmentRegister;
import newOs.component.cpu.Registers.RegisterInterface.StatusRegister;

import java.util.Map;

public interface AbstractRegisterFactory {
    GeneralRegister createGeneralRegister();  // 通用寄存器（如 EAX）
    SegmentRegister createSegmentRegister();  // 段寄存器（如 CS）
    ProgramCounter createProgramCounter();    // 程序计数器（PC）
    StatusRegister createStatusRegister();    // 状态寄存器（FLAGS）
    Map<RegisterType,AbstractRegister> GenerateBaseConfig();
}

