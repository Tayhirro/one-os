package newOs.component.cpu.Registers.RegisterImpl;

import lombok.Data;

import lombok.experimental.Accessors;
import newOs.component.cpu.Registers.RegisterInterface.ProgramCounter;

/*
 *  Program Counter
 *   bit: 32
 */


@Data
@Accessors(chain = true)
public class X86ProgramCounter implements ProgramCounter {
    private int address; // 32位
    private String name;

    @Override
    public void setValue(int address) {
        this.address = address & 0xFFFFFFFF;
    }

    @Override
    public int getValue() {
        return address;
    }

    @Override
    public void increment() {
        address += 4; // X86指令默认4字节对齐
    }
}
