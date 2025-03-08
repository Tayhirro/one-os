package newOs.component.cpu.Registers.RegisterInterface;


import newOs.component.cpu.Registers.AbstractRegister;

public interface StatusRegister extends AbstractRegister {
    void setValue(int value);
    int getValue();
    void setBit(int index, boolean value);
}
