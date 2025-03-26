/*package newOs;


import static org.junit.jupiter.api.Assertions.*;
import com.alibaba.fastjson.JSONObject;
import newOs.kernel.DiskStorage.Device;
import newOs.kernel.DiskStorage.DeviceStorageManager;
import newOs.kernel.filesystem.FileReader;
import newOs.kernel.filesystem.FileWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;

class FileReaderWriterIntegrationTest {
    private static final String TEST_DEVICE_NAME = "testDevice1";
    private DeviceStorageManager deviceManager;
    private FileReader fileReader;
    private FileWriter fileWriter;

    @BeforeEach
    void setUp() throws SQLException {
        // 初始化数据库管理器和设备
        deviceManager = new DeviceStorageManager();
        fileReader = FileReader.getFileReader();
        fileWriter = FileWriter.getFileWriter();

        // 插入测试设备到数据库
        deviceManager.addDevice("testDevice2","{\"capacity\": 1024}" );
    }

    // --------------------- 测试 FileReader.readDevice ---------------------
    @Test
    void readDevice_WhenDeviceExists_ShouldReturnValidData() {
        JSONObject output = new JSONObject();
        boolean result = fileReader.readDevice(TEST_DEVICE_NAME, output);

        assertTrue(result);
        assertEquals(TEST_DEVICE_NAME, output.getString("deviceName"));
        assertNotNull(output.getJSONObject("deviceInfo"));
    }

    @Test
    void readDevice_WhenDeviceNotFound_ShouldReturnError() {
        JSONObject output = new JSONObject();
        boolean result = fileReader.readDevice("nonExistentDevice", output);

        assertFalse(result);
        assertEquals("Device not found: nonExistentDevice", output.getString("error"));
    }

    // --------------------- 测试 FileWriter.writeToDevice ---------------------
    @Test
    void writeToDevice_WhenDeviceExists_ShouldUpdateData() throws SQLException {
        JSONObject data = new JSONObject();
        data.put("key", "value");

        String result = fileWriter.writeToDevice(TEST_DEVICE_NAME, data);

        assertTrue(result.startsWith("Success"));

        // 验证数据库中的设备信息是否更新
        Device updatedDevice = deviceManager.getDeviceByName(TEST_DEVICE_NAME);
        assertEquals(data.toJSONString(), updatedDevice.getDeviceInfo());
    }

    @Test
    void writeToDevice_WhenDeviceNotFound_ShouldReturnError() {
        JSONObject data = new JSONObject();
        String result = fileWriter.writeToDevice("invalidDevice", data);

        assertTrue(result.startsWith("Error"));
        assertTrue(result.contains("not found"));
    }
}
