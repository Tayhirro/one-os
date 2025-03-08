package newOs.component.cpu.Registers.RegisterImpl;

import lombok.Data;
import lombok.experimental.Accessors;
import newOs.component.cpu.Registers.RegisterInterface.GeneralRegister;


/*
*  GeneralRegister 通用寄存器
*   bit: 32
 */


@Data
@Accessors(chain = true)
public class X86GeneralRegister implements GeneralRegister {
    private int value;
    private String name;  // 增加名称字段

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }


    public void setBit(int index, boolean value) throws IllegalArgumentException{
        if(index<0 || index>31){
            throw new IllegalArgumentException("X86_General_Register Bit index out of range");
        }
        if (value) {
            this.value |= (1 << index);
        } else {
            this.value &= ~(1 << index);
        }
    }
}
