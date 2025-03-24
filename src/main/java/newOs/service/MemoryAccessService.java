package newOs.service;

import newOs.exception.AddressTranslationException;
import newOs.exception.MemoryException;
import newOs.exception.MemoryProtectionException;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;

/**
 * 内存访问服务接口
 * 提供内存读写操作和地址转换服务
 */
public interface MemoryAccessService {

    /**
     * 从虚拟地址读取一个字节
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 读取的字节值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    byte readByte(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException;

    /**
     * 从虚拟地址读取一个短整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 读取的短整型值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    short readShort(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException;

    /**
     * 从虚拟地址读取一个整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 读取的整型值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    int readInt(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException;

    /**
     * 从虚拟地址读取一个长整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 读取的长整型值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    long readLong(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException;

    /**
     * 从虚拟地址读取一个浮点型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 读取的浮点型值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    float readFloat(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException;

    /**
     * 从虚拟地址读取一个双精度浮点型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 读取的双精度浮点型值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    double readDouble(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException;

    /**
     * 从虚拟地址读取指定长度的字节数组
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param length 长度
     * @return 读取的字节数组
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    byte[] readBytes(int processId, VirtualAddress virtualAddress, int length) throws MemoryException, MemoryProtectionException;

    /**
     * 从虚拟地址读取字符串（以null结尾）
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param maxLength 最大长度
     * @return 读取的字符串
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    String readString(int processId, VirtualAddress virtualAddress, int maxLength) throws MemoryException, MemoryProtectionException;

    /**
     * 向虚拟地址写入一个字节
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param value 要写入的字节值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    void writeByte(int processId, VirtualAddress virtualAddress, byte value) throws MemoryException, MemoryProtectionException;

    /**
     * 向虚拟地址写入一个短整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param value 要写入的短整型值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    void writeShort(int processId, VirtualAddress virtualAddress, short value) throws MemoryException, MemoryProtectionException;

    /**
     * 向虚拟地址写入一个整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param value 要写入的整型值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    void writeInt(int processId, VirtualAddress virtualAddress, int value) throws MemoryException, MemoryProtectionException;

    /**
     * 向虚拟地址写入一个长整型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param value 要写入的长整型值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    void writeLong(int processId, VirtualAddress virtualAddress, long value) throws MemoryException, MemoryProtectionException;

    /**
     * 向虚拟地址写入一个浮点型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param value 要写入的浮点型值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    void writeFloat(int processId, VirtualAddress virtualAddress, float value) throws MemoryException, MemoryProtectionException;

    /**
     * 向虚拟地址写入一个双精度浮点型
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param value 要写入的双精度浮点型值
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    void writeDouble(int processId, VirtualAddress virtualAddress, double value) throws MemoryException, MemoryProtectionException;

    /**
     * 向虚拟地址写入字节数组
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param data 要写入的字节数组
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    void writeBytes(int processId, VirtualAddress virtualAddress, byte[] data) throws MemoryException, MemoryProtectionException;

    /**
     * 向虚拟地址写入字符串
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param str
     * @param nullTerminated 是否添加null结尾
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    void writeString(int processId, VirtualAddress virtualAddress, String str, boolean nullTerminated) throws MemoryException, MemoryProtectionException;

    /**
     * 将虚拟地址转换为物理地址
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @return 对应的物理地址
     * @throws AddressTranslationException 地址转换异常
     */
    PhysicalAddress translateAddress(int processId, VirtualAddress virtualAddress) throws AddressTranslationException;

    /**
     * 检查内存区域是否可读
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 区域大小
     * @return 是否可读
     */
    boolean isReadable(int processId, VirtualAddress virtualAddress, long size);

    /**
     * 检查内存区域是否可写
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 区域大小
     * @return 是否可写
     */
    boolean isWritable(int processId, VirtualAddress virtualAddress, long size);

    /**
     * 检查内存区域是否可执行
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 区域大小
     * @return 是否可执行
     */
    boolean isExecutable(int processId, VirtualAddress virtualAddress, long size);

    /**
     * 清空内存区域（填充0）
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 区域大小
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    void clearMemory(int processId, VirtualAddress virtualAddress, long size) throws MemoryException, MemoryProtectionException;

    /**
     * 复制内存区域
     * @param processId 进程ID
     * @param sourceAddress 源虚拟地址
     * @param targetAddress 目标虚拟地址
     * @param size 复制大小
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    void copyMemory(int processId, VirtualAddress sourceAddress, VirtualAddress targetAddress, long size) throws MemoryException, MemoryProtectionException;

    /**
     * 跨进程复制内存
     * @param sourceProcessId 源进程ID
     * @param sourceAddress 源虚拟地址
     * @param targetProcessId 目标进程ID
     * @param targetAddress 目标虚拟地址
     * @param size 复制大小
     * @throws MemoryException 内存异常
     * @throws MemoryProtectionException 内存保护异常
     */
    void copyMemoryBetweenProcesses(int sourceProcessId, VirtualAddress sourceAddress, 
                                    int targetProcessId, VirtualAddress targetAddress, 
                                    long size) throws MemoryException, MemoryProtectionException;
                                    
    /**
     * 从虚拟地址读取内存
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 读取大小
     * @return 读取的字节数组
     * @throws MemoryException 内存异常
     */
    byte[] read(int processId, VirtualAddress virtualAddress, int size) throws MemoryException;
    
    /**
     * 向虚拟地址写入内存
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param data 要写入的数据
     * @return 是否写入成功
     * @throws MemoryException 内存异常
     */
    boolean write(int processId, VirtualAddress virtualAddress, byte[] data) throws MemoryException;
    
    /**
     * 执行虚拟地址处的代码
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 执行区域大小
     * @return 是否执行成功
     * @throws MemoryException 内存异常
     */
    boolean execute(int processId, VirtualAddress virtualAddress, int size) throws MemoryException;
} 