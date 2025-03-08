package newOs.component.cpu.Registers.RegisterImpl;


import lombok.Data;
import lombok.experimental.Accessors;
import newOs.component.cpu.Registers.RegisterInterface.StatusRegister;



@Data
@Accessors(chain = true)
public class X86StatusRegister implements StatusRegister {
    private int value;
    private String name;

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }


    public void setBit(int index, boolean value) throws IllegalArgumentException{
        if (index < 0 || index >= 32) {
            throw new IllegalArgumentException("X86_Flag_Register Bit index out of range");
        }
        if (value) {
            this.value |= (1 << index);
        } else {
            this.value &= ~(1 << index);
        }
    }
}
