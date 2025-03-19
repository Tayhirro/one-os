package newOs.common.processConstant;

public class InstructionConstant {
    // C time （计算指令，使用CPU，时长time）
    public static final String C = "C";
    // K time （I/O指令，键盘输入，时长time）
    public static final String D = "D";
    public static final String OPEN = "OPEN"; //打开文件
    public static final String CLOSE = "CLOSE"; //关闭文件
    public static final String READ = "READ";
    // W filename time size （写文件，时长，文件大小size）
    public static final String WRITE = "WRITE";
    // M block 进程占用内存空间 （资源需求声明）
    public static final String M = "M";
    // Y number 进程的优先数 （调度参数声明，优先级）
    public static final String Y = "Y";

    // A 模拟访问内存
    public static final String A = "A";

    // Q 结束运行 （程序结束）
    public static final String Q = "Q";
}