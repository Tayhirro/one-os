package newOs.service.impl;

import lombok.extern.slf4j.Slf4j;
import newOs.exception.AddressTranslationException;
import newOs.exception.MemoryException;
import newOs.exception.MemoryProtectionException;
import newOs.kernel.memory.MemoryManager;
import newOs.kernel.memory.model.PhysicalAddress;
import newOs.kernel.memory.model.VirtualAddress;
import newOs.service.MemoryAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 内存访问服务实现类
 * 提供内存读写操作和地址转换服务的实现
 */
@Service
@Slf4j
public class MemoryAccessServiceImpl implements MemoryAccessService {

    @Autowired
    private MemoryManager memoryManager;

    @Override
    public byte readByte(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException {
        log.debug("读取进程{}虚拟地址{}处的字节", processId, virtualAddress);
        
        try {
            // 检查读取权限
            if (!isReadable(processId, virtualAddress, 1)) {
                throw new MemoryProtectionException("内存区域不可读: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 读取单个字节
            return memoryManager.readByte(processId, virtualAddress);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取字节失败: {}", e.getMessage(), e);
            throw new MemoryException("读取字节失败: " + e.getMessage(), e);
        }
    }

    @Override
    public short readShort(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException {
        log.debug("读取进程{}虚拟地址{}处的短整型", processId, virtualAddress);
        
        try {
            // 检查读取权限
            if (!isReadable(processId, virtualAddress, 2)) {
                throw new MemoryProtectionException("内存区域不可读: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 读取2个字节组成short
            return memoryManager.readShort(processId, virtualAddress);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取短整型失败: {}", e.getMessage(), e);
            throw new MemoryException("读取短整型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int readInt(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException {
        log.debug("读取进程{}虚拟地址{}处的整型", processId, virtualAddress);
        
        try {
            // 检查读取权限
            if (!isReadable(processId, virtualAddress, 4)) {
                throw new MemoryProtectionException("内存区域不可读: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 读取4个字节组成int
            return memoryManager.readInt(processId, virtualAddress);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取整型失败: {}", e.getMessage(), e);
            throw new MemoryException("读取整型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public long readLong(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException {
        log.debug("读取进程{}虚拟地址{}处的长整型", processId, virtualAddress);
        
        try {
            // 检查读取权限
            if (!isReadable(processId, virtualAddress, 8)) {
                throw new MemoryProtectionException("内存区域不可读: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 读取8个字节组成long
            return memoryManager.readLong(processId, virtualAddress);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取长整型失败: {}", e.getMessage(), e);
            throw new MemoryException("读取长整型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public float readFloat(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException {
        log.debug("读取进程{}虚拟地址{}处的浮点型", processId, virtualAddress);
        
        try {
            // 检查读取权限
            if (!isReadable(processId, virtualAddress, 4)) {
                throw new MemoryProtectionException("内存区域不可读: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 读取4个字节解析为float
            int intBits = memoryManager.readInt(processId, virtualAddress);
            return Float.intBitsToFloat(intBits);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取浮点型失败: {}", e.getMessage(), e);
            throw new MemoryException("读取浮点型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public double readDouble(int processId, VirtualAddress virtualAddress) throws MemoryException, MemoryProtectionException {
        log.debug("读取进程{}虚拟地址{}处的双精度浮点型", processId, virtualAddress);
        
        try {
            // 检查读取权限
            if (!isReadable(processId, virtualAddress, 8)) {
                throw new MemoryProtectionException("内存区域不可读: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 读取8个字节解析为double
            long longBits = memoryManager.readLong(processId, virtualAddress);
            return Double.longBitsToDouble(longBits);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取双精度浮点型失败: {}", e.getMessage(), e);
            throw new MemoryException("读取双精度浮点型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] readBytes(int processId, VirtualAddress virtualAddress, int length) throws MemoryException, MemoryProtectionException {
        log.debug("读取进程{}虚拟地址{}处的{}字节数据", processId, virtualAddress, length);
        
        try {
            // 检查读取权限
            if (!isReadable(processId, virtualAddress, length)) {
                throw new MemoryProtectionException("内存区域不可读: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 读取字节数组
            return memoryManager.readMemory(processId, virtualAddress, length);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取字节数组失败: {}", e.getMessage(), e);
            throw new MemoryException("读取字节数组失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String readString(int processId, VirtualAddress virtualAddress, int maxLength) throws MemoryException, MemoryProtectionException {
        log.debug("读取进程{}虚拟地址{}处的字符串，最大长度{}", processId, virtualAddress, maxLength);
        
        try {
            // 检查读取权限
            if (!isReadable(processId, virtualAddress, 1)) {
                throw new MemoryProtectionException("内存区域不可读: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 逐字节读取，直到遇到null结束符或达到最大长度
            byte[] buffer = new byte[maxLength];
            int length = 0;
            
            for (; length < maxLength; length++) {
                byte b = memoryManager.readByte(processId, virtualAddress.add(length));
                if (b == 0) {
                    break;
                }
                buffer[length] = b;
            }
            
            // 转换为字符串
            return new String(buffer, 0, length, StandardCharsets.UTF_8);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取字符串失败: {}", e.getMessage(), e);
            throw new MemoryException("读取字符串失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeByte(int processId, VirtualAddress virtualAddress, byte value) throws MemoryException, MemoryProtectionException {
        log.debug("写入字节{}到进程{}虚拟地址{}", value, processId, virtualAddress);
        
        try {
            // 检查写入权限
            if (!isWritable(processId, virtualAddress, 1)) {
                throw new MemoryProtectionException("内存区域不可写: " + virtualAddress, processId, virtualAddress, false, false, true);
            }
            
            // 写入单个字节
            memoryManager.writeByte(processId, virtualAddress, value);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("写入字节失败: {}", e.getMessage(), e);
            throw new MemoryException("写入字节失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeShort(int processId, VirtualAddress virtualAddress, short value) throws MemoryException, MemoryProtectionException {
        log.debug("写入短整型{}到进程{}虚拟地址{}", value, processId, virtualAddress);
        
        try {
            // 检查写入权限
            if (!isWritable(processId, virtualAddress, 2)) {
                throw new MemoryProtectionException("内存区域不可写: " + virtualAddress, processId, virtualAddress, false, false, true);
            }
            
            // 写入2个字节
            memoryManager.writeShort(processId, virtualAddress, value);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("写入短整型失败: {}", e.getMessage(), e);
            throw new MemoryException("写入短整型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeInt(int processId, VirtualAddress virtualAddress, int value) throws MemoryException, MemoryProtectionException {
        log.debug("写入整型{}到进程{}虚拟地址{}", value, processId, virtualAddress);
        
        try {
            // 检查写入权限
            if (!isWritable(processId, virtualAddress, 4)) {
                throw new MemoryProtectionException("内存区域不可写: " + virtualAddress, processId, virtualAddress, false, false, true);
            }
            
            // 写入4个字节
            memoryManager.writeInt(processId, virtualAddress, value);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("写入整型失败: {}", e.getMessage(), e);
            throw new MemoryException("写入整型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeLong(int processId, VirtualAddress virtualAddress, long value) throws MemoryException, MemoryProtectionException {
        log.debug("写入长整型{}到进程{}虚拟地址{}", value, processId, virtualAddress);
        
        try {
            // 检查写入权限
            if (!isWritable(processId, virtualAddress, 8)) {
                throw new MemoryProtectionException("内存区域不可写: " + virtualAddress, processId, virtualAddress, false, false, true);
            }
            
            // 写入8个字节
            memoryManager.writeLong(processId, virtualAddress, value);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("写入长整型失败: {}", e.getMessage(), e);
            throw new MemoryException("写入长整型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeFloat(int processId, VirtualAddress virtualAddress, float value) throws MemoryException, MemoryProtectionException {
        log.debug("写入浮点型{}到进程{}虚拟地址{}", value, processId, virtualAddress);
        
        try {
            // 检查写入权限
            if (!isWritable(processId, virtualAddress, 4)) {
                throw new MemoryProtectionException("内存区域不可写: " + virtualAddress, processId, virtualAddress, false, false, true);
            }
            
            // 将float转换为int位模式，再写入4个字节
            int intBits = Float.floatToIntBits(value);
            memoryManager.writeInt(processId, virtualAddress, intBits);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("写入浮点型失败: {}", e.getMessage(), e);
            throw new MemoryException("写入浮点型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeDouble(int processId, VirtualAddress virtualAddress, double value) throws MemoryException, MemoryProtectionException {
        log.debug("写入双精度浮点型{}到进程{}虚拟地址{}", value, processId, virtualAddress);
        
        try {
            // 检查写入权限
            if (!isWritable(processId, virtualAddress, 8)) {
                throw new MemoryProtectionException("内存区域不可写: " + virtualAddress, processId, virtualAddress, false, false, true);
            }
            
            // 将double转换为long位模式，再写入8个字节
            long longBits = Double.doubleToLongBits(value);
            memoryManager.writeLong(processId, virtualAddress, longBits);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("写入双精度浮点型失败: {}", e.getMessage(), e);
            throw new MemoryException("写入双精度浮点型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeBytes(int processId, VirtualAddress virtualAddress, byte[] data) throws MemoryException, MemoryProtectionException {
        log.debug("写入{}字节数据到进程{}虚拟地址{}", data.length, processId, virtualAddress);
        
        try {
            // 检查写入权限
            if (!isWritable(processId, virtualAddress, data.length)) {
                throw new MemoryProtectionException("内存区域不可写: " + virtualAddress, processId, virtualAddress, false, false, true);
            }
            
            // 写入字节数组
            memoryManager.writeMemory(processId, virtualAddress, data);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("写入字节数组失败: {}", e.getMessage(), e);
            throw new MemoryException("写入字节数组失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeString(int processId, VirtualAddress virtualAddress, String value, boolean nullTerminated) throws MemoryException, MemoryProtectionException {
        log.debug("写入字符串{}到进程{}虚拟地址{}", value, processId, virtualAddress);
        
        try {
            // 获取字符串的UTF-8字节表示
            byte[] stringBytes = value.getBytes(StandardCharsets.UTF_8);
            
            // 计算总长度（是否需要添加结尾的null字符）
            int totalLength = nullTerminated ? stringBytes.length + 1 : stringBytes.length;
            
            // 检查写入权限
            if (!isWritable(processId, virtualAddress, totalLength)) {
                throw new MemoryProtectionException("内存区域不可写: " + virtualAddress, processId, virtualAddress, false, false, true);
            }
            
            // 写入字符串字节
            memoryManager.writeMemory(processId, virtualAddress, stringBytes);
            
            // 如果需要，写入结尾的null字符
            if (nullTerminated) {
                memoryManager.writeByte(processId, virtualAddress.add(stringBytes.length), (byte) 0);
            }
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("写入字符串失败: {}", e.getMessage(), e);
            throw new MemoryException("写入字符串失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PhysicalAddress translateAddress(int processId, VirtualAddress virtualAddress) throws AddressTranslationException {
        log.debug("将进程{}的虚拟地址{}转换为物理地址", processId, virtualAddress);
        
        try {
            return memoryManager.translate(processId, virtualAddress);
        } catch (Exception e) {
            log.error("地址转换失败: {}", e.getMessage(), e);
            throw new AddressTranslationException("地址转换失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isReadable(int processId, VirtualAddress virtualAddress, long size) {
        try {
            for (long i = 0; i < size; i += 8) {
                // 检查当前位置是否可读
                long remaining = size - i;
                int checkSize = (int) Math.min(remaining, 8);
                
                VirtualAddress currentAddress = virtualAddress.add((int)i);
                if (!memoryManager.isReadable(processId, currentAddress, checkSize)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("检查内存可读性失败", e);
            return false;
        }
    }

    @Override
    public boolean isWritable(int processId, VirtualAddress virtualAddress, long size) {
        try {
            for (long i = 0; i < size; i += 8) {
                // 检查当前位置是否可写
                long remaining = size - i;
                int checkSize = (int) Math.min(remaining, 8);
                
                VirtualAddress currentAddress = virtualAddress.add((int)i);
                if (!memoryManager.isWritable(processId, currentAddress, checkSize)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("检查内存可写性失败", e);
            return false;
        }
    }

    @Override
    public boolean isExecutable(int processId, VirtualAddress virtualAddress, long size) {
        try {
            // 检查内存区域是否可执行
            return memoryManager.checkMemoryPermission(processId, virtualAddress, size, "x");
        } catch (Exception e) {
            log.warn("检查内存区域可执行性失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void clearMemory(int processId, VirtualAddress virtualAddress, long size) throws MemoryException, MemoryProtectionException {
        log.debug("清空进程{}虚拟地址{}处的{}字节内存", processId, virtualAddress, size);
        
        try {
            // 检查写入权限
            if (!isWritable(processId, virtualAddress, size)) {
                throw new MemoryProtectionException("内存区域不可写: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 创建全为0的字节数组
            byte[] zeroBuffer = new byte[(int) size];
            
            // 写入全0数据
            memoryManager.writeMemory(processId, virtualAddress, zeroBuffer);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("清空内存失败: {}", e.getMessage(), e);
            throw new MemoryException("清空内存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void copyMemory(int processId, VirtualAddress sourceAddress, VirtualAddress targetAddress, long size) throws MemoryException, MemoryProtectionException {
        log.debug("复制进程{}的{}字节内存从{}到{}", processId, size, sourceAddress, targetAddress);
        
        try {
            // 检查源区域是否可读
            if (!isReadable(processId, sourceAddress, size)) {
                throw new MemoryProtectionException("源内存区域不可读: " + sourceAddress, processId, sourceAddress, false, true, false);
            }
            
            // 检查目标区域是否可写
            if (!isWritable(processId, targetAddress, size)) {
                throw new MemoryProtectionException("目标内存区域不可写: " + targetAddress, processId, targetAddress, false, true, false);
            }
            
            // 从源区域读取数据
            byte[] buffer = memoryManager.readMemory(processId, sourceAddress, (int) size);
            
            // 写入目标区域
            memoryManager.writeMemory(processId, targetAddress, buffer);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("复制内存失败: {}", e.getMessage(), e);
            throw new MemoryException("复制内存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void copyMemoryBetweenProcesses(int sourceProcessId, VirtualAddress sourceAddress, int targetProcessId, VirtualAddress targetAddress, long size) throws MemoryException, MemoryProtectionException {
        log.debug("跨进程复制{}字节内存，从进程{}的{}到进程{}的{}", size, sourceProcessId, sourceAddress, targetProcessId, targetAddress);
        
        try {
            // 检查源区域是否可读
            if (!isReadable(sourceProcessId, sourceAddress, size)) {
                throw new MemoryProtectionException("源内存区域不可读: 进程" + sourceProcessId + ", 地址" + sourceAddress, sourceProcessId, sourceAddress, false, true, false);
            }
            
            // 检查目标区域是否可写
            if (!isWritable(targetProcessId, targetAddress, size)) {
                throw new MemoryProtectionException("目标内存区域不可写: 进程" + targetProcessId + ", 地址" + targetAddress, targetProcessId, targetAddress, false, true, false);
            }
            
            // 从源进程读取数据
            byte[] buffer = memoryManager.readMemory(sourceProcessId, sourceAddress, (int) size);
            
            // 写入目标进程
            memoryManager.writeMemory(targetProcessId, targetAddress, buffer);
        } catch (MemoryProtectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("跨进程复制内存失败: {}", e.getMessage(), e);
            throw new MemoryException("跨进程复制内存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从虚拟地址读取内存
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 读取大小
     * @return 读取的字节数组
     * @throws MemoryException 内存异常
     */
    @Override
    public byte[] read(int processId, VirtualAddress virtualAddress, int size) throws MemoryException {
        log.debug("读取进程{}虚拟地址{}处的{}字节数据", processId, virtualAddress, size);
        
        try {
            // 检查读取权限
            if (!isReadable(processId, virtualAddress, size)) {
                throw new MemoryProtectionException("内存区域不可读: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 读取指定长度的字节数组
            return readBytes(processId, virtualAddress, size);
        } catch (MemoryProtectionException e) {
            log.error("内存读取权限错误: {}", e.getMessage(), e);
            throw new MemoryException("内存读取权限错误: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("内存读取失败: {}", e.getMessage(), e);
            throw new MemoryException("内存读取失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 向虚拟地址写入内存
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param data 要写入的数据
     * @return 是否写入成功
     * @throws MemoryException 内存异常
     */
    @Override
    public boolean write(int processId, VirtualAddress virtualAddress, byte[] data) throws MemoryException {
        log.debug("向进程{}虚拟地址{}写入{}字节数据", processId, virtualAddress, data.length);
        
        try {
            // 检查写入权限
            if (!isWritable(processId, virtualAddress, data.length)) {
                throw new MemoryProtectionException("内存区域不可写: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 写入字节数组
            writeBytes(processId, virtualAddress, data);
            return true;
        } catch (MemoryProtectionException e) {
            log.error("内存写入权限错误: {}", e.getMessage(), e);
            throw new MemoryException("内存写入权限错误: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("内存写入失败: {}", e.getMessage(), e);
            throw new MemoryException("内存写入失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行虚拟地址处的代码
     * @param processId 进程ID
     * @param virtualAddress 虚拟地址
     * @param size 执行区域大小
     * @return 是否执行成功
     * @throws MemoryException 内存异常
     */
    @Override
    public boolean execute(int processId, VirtualAddress virtualAddress, int size) throws MemoryException {
        log.debug("执行进程{}虚拟地址{}处的代码，大小为{}字节", processId, virtualAddress, size);
        
        try {
            // 检查执行权限
            if (!isExecutable(processId, virtualAddress, size)) {
                throw new MemoryProtectionException("内存区域不可执行: " + virtualAddress, processId, virtualAddress, false, true, false);
            }
            
            // 由于这是模拟系统，这里不实际执行代码，只是进行权限检查
            // 实际执行代码需要虚拟机或解释器支持
            log.info("进程{}的代码段被标记为执行: 虚拟地址={}, 大小={}字节", 
                    processId, virtualAddress, size);
            
            return true;
        } catch (MemoryProtectionException e) {
            log.error("内存执行权限错误: {}", e.getMessage(), e);
            throw new MemoryException("内存执行权限错误: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("内存执行失败: {}", e.getMessage(), e);
            throw new MemoryException("内存执行失败: " + e.getMessage(), e);
        }
    }
} 