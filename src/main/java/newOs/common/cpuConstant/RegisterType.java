package newOs.common.cpuConstant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum RegisterType{
    GENERAL_REGISTER(0),
    SEGMENT_REGISTER(1),
    PROGRAM_COUNTER(2),
    STATUS_REGISTER(3),
    MAR(4),
    MDR(5);
    private  final int value;
    RegisterType(int value){
        this.value = value;
    }
}