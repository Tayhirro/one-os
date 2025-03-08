package newOs.component.cpu.Registers.RegisterInterface;


import newOs.component.cpu.Registers.AbstractRegister;

// 程序计数器接口（不支持位操作）
public interface ProgramCounter extends AbstractRegister {
    void setValue(int address);
    int getValue();
    void increment();
}