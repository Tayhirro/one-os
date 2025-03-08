package newOs.component.cpu.Registers.RegisterImpl;


import lombok.Data;
import lombok.experimental.Accessors;
import newOs.component.cpu.Registers.RegisterInterface.SegmentRegister;

/*
* Segment Register
*   bit : 16
*
 */
@Data
@Accessors(chain = true)
public class X86SegmentRegister implements SegmentRegister {
    private int value;
    private String name;


    @Override
    public void setValue(int value) {
        this.value = value & 0xffff;
    }
    @Override
    public int getValue() {
        return value & 0xffff;
    }


    public void setBit(int index, boolean value) throws IllegalArgumentException{
        if(index<0 || index>15) {
            throw new IllegalArgumentException("X86_Segment_Register Bit index out of range");
        }
        if (value) {
            this.value |= (1 << index);
        } else {
            this.value &= ~(1 << index);
        }
    }
}
