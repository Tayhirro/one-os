/*package newOs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.JSONObject;
import newOs.common.fileSystemConstant.DeviceStatusType;
import newOs.component.device.Disk;
import newOs.component.memory.protected1.PCB;
import newOs.dto.resp.DeviceManage.DeviceInfoReturnImplDTO;
import newOs.kernel.filesystem.FileReader;
import newOs.kernel.filesystem.FileWriter;
import newOs.kernel.interrupt.InterruptController;
import newOs.kernel.DiskStorage.DeviceStorageManager;
import newOs.kernel.DiskStorage.Device;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;

class DiskDriverImplIntegrationTest {
    private static final String DEVICE_NAME = "testDisk";
    private static final String DEVICE_ID = "disk123";
    private DiskDriverImpl diskDriver;
    private InterruptController mockInterruptController;
    private Disk mockDisk;
    private DeviceStorageManager deviceManager;
    private PCB testPcb;

    @BeforeEach
    void setUp() throws SQLException {
        // 初始化数据库和设备
        deviceManager = new DeviceStorageManager();
        Device device = new Device();
        device.setDeviceId(DEVICE_ID);
        device.setDeviceName(DEVICE_NAME);
        device.setDeviceInfo("{\"capacity\": 1024}");
        deviceManager.addDevice(device);

        // 初始化依赖项
        mockInterruptController = mock(InterruptController.class);
        mockDisk = mock(Disk.class);

        // 创建 DiskDriverImpl 实例
        JSONObject deviceInfo = new JSONObject();
        diskDriver = new DiskDriverImpl(DEVICE_NAME, deviceInfo, mockInterruptController, mockDisk);

        // 创建完整的 PCB 对象
        testPcb = new PCB(
                100,                    // pid
                "testProcess",           // processName
                0,                       // ir
                4096,                    // size
                "READY",                 // state
                0x1000,                  // PBTR
                0x2000,                  // SBTR
                256,                     // pageTableSize
                12,                      // segmentTableSize
                System.currentTimeMillis(), // timeStamp
                1000,                    // remainingTime
                5000,                    // expectedTime
                1,                       // priority
                new String[]{"指令1", "指令2"}, // instructions
                10,                      // swapInTime
                10,                      // swapOutTime
                0,                       // pageFaultRate
                1                        // coreId
        );
    }

    @AfterEach
    void tearDown() throws SQLException {
        // 清理数据库中的测试设备
        deviceManager.deleteDevice(DEVICE_ID);
    }

    // --------------------- 测试 executeDeviceReadOperation ---------------------
    @Test
    void executeDeviceReadOperation_WhenDeviceFree_ShouldReadDataFromDB() {
        // 调用读操作
        DeviceInfoReturnImplDTO result = diskDriver.executeDeviceReadOperation(testPcb);

        // 验证设备状态和结果
        assertEquals(DeviceStatusType.FREE, result.getDeviceStatusType());
        assertNotNull(result.getArgs());
        assertFalse(result.getArgs().isEmpty());

        // 验证数据库中的数据是否被正确读取
        JSONObject deviceData = result.getArgs();
        assertEquals(DEVICE_ID, deviceData.getString("deviceId"));
        assertEquals(DEVICE_NAME, deviceData.getString("deviceName"));
    }

    @Test
    void executeDeviceReadOperation_WhenDeviceBusy_ShouldAddToQueue() {
        // 设置设备为忙状态
        diskDriver.setBusy(true);

        // 调用读操作
        DeviceInfoReturnImplDTO result = diskDriver.executeDeviceReadOperation(testPcb);

        // 验证状态和队列
        assertEquals(DeviceStatusType.BUSY, result.getDeviceStatusType());
        assertEquals(1, diskDriver.getDeviceWaitingQueue().size());
        assertTrue(diskDriver.getDeviceWaitingQueue().contains(testPcb));
    }

    // --------------------- 测试 executeDeviceWriteOperation ---------------------
    @Test
    void executeDeviceWriteOperation_WhenDeviceFree_ShouldUpdateDB() {
        // 准备写入数据
        JSONObject writeData = new JSONObject();
        writeData.put("data", "test value");

        // 调用写操作
        DeviceInfoReturnImplDTO result = diskDriver.executeDeviceWriteOperation(writeData, testPcb);

        // 验证设备状态
        assertEquals(DeviceStatusType.FREE, result.getDeviceStatusType());

        // 验证数据库中的设备信息是否更新
        Device updatedDevice = deviceManager.getDeviceByName(DEVICE_NAME);
        assertEquals(writeData.toJSONString(), updatedDevice.getDeviceInfo());
    }

    @Test
    void executeDeviceWriteOperation_WhenDeviceBusy_ShouldAddToQueue() {
        // 设置设备为忙状态
        diskDriver.setBusy(true);
        JSONObject writeData = new JSONObject();

        // 调用写操作
        DeviceInfoReturnImplDTO result = diskDriver.executeDeviceWriteOperation(writeData, testPcb);

        // 验证状态和队列
        assertEquals(DeviceStatusType.BUSY, result.getDeviceStatusType());
        assertEquals(1, diskDriver.getDeviceWaitingQueue().size());
    }
}
