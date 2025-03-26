package newOs.kernel.DiskStorage;

import com.google.gson.Gson;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备存储管理器，负责 device 表的 CRUD 操作
 */
@Component
public class DeviceStorageManager {
    private static final String DB_URL = "jdbc:sqlite:src\\main\\java\\newOs\\component\\disk.db";

    /**
     * 获取数据库连接
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * 添加设备
     * @param deviceName 设备名称（唯一）
     * @param contentJson 设备内容（JSON字符串）
     */
    public void addDevice(String deviceName, String contentJson) throws SQLException {
        String sql = "INSERT INTO device(device_name, device_status, device_info) VALUES(?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deviceName);
            pstmt.setInt(2, 0);
            pstmt.setString(3, contentJson);
            pstmt.executeUpdate();
        }
    }

    /**
     * 根据设备ID查询设备
     * @param deviceId 设备ID
     * @return 设备对象，不存在则返回null
     */
    public Device getDeviceById(int deviceId) throws SQLException {
        String sql = "SELECT * FROM device WHERE device_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, deviceId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? mapResultSetToDevice(rs) : null;
        }
    }

    /**
     * 根据设备名称查询设备
     * @param deviceName 设备名称
     */
    public Device getDeviceByName(String deviceName) throws SQLException {
        String sql = "SELECT * FROM device WHERE device_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deviceName);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? mapResultSetToDevice(rs) : null;
        }
    }

    /**
     * 更新设备状态
     * @param deviceId 设备ID
     * @param newStatus 新状态（0/1）
     */
    public void updateDeviceStatus(int deviceId, int newStatus) throws SQLException {
        String sql = "UPDATE device SET device_status = ? WHERE device_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newStatus);
            pstmt.setInt(2, deviceId);
            pstmt.executeUpdate();
        }
    }

    /**
     * 删除设备
     * @param deviceId 设备ID
     */
    public void deleteDevice(int deviceId) throws SQLException {
        String sql = "DELETE FROM device WHERE device_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, deviceId);
            pstmt.executeUpdate();
        }
    }

    /**
     * 获取所有设备列表
     */
    public List<Device> getAllDevices() throws SQLException {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT * FROM device";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                devices.add(mapResultSetToDevice(rs));
            }
        }
        return devices;
    }

    /**
     * 更新设备信息字段（JSON内容）
     * @param deviceId 设备ID
     * @param infoJson 新的JSON内容
     * @throws SQLException 若数据库操作失败
     */
    public void updateDeviceInfo(int deviceId, String infoJson) throws SQLException {
        String sql = "UPDATE device SET device_info = ? WHERE device_id = ?";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, infoJson);
                pstmt.setInt(2, deviceId);
                pstmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * 将 ResultSet 映射为 Device 对象
     */
    private Device mapResultSetToDevice(ResultSet rs) throws SQLException {
        return new Device(
                rs.getInt("device_id"),
                rs.getString("device_name"),
                rs.getInt("device_status"),
                rs.getString("device_info")
        );
    }

    /**
     * 设备实体类，对应数据库 device 表
     */
    @Data
    public static class Device {
        private int deviceId;
        private String deviceName;
        private int deviceStatus;
        private String deviceInfo;

        public Device(int deviceId, String deviceName, int deviceStatus, String deviceInfo) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.deviceStatus = deviceStatus;
            this.deviceInfo = deviceInfo;
        }

        /**
         * 将对象序列化为 JSON 存储到 deviceInfo
         */
        public void setInfo(Object info) {
            this.deviceInfo = new Gson().toJson(info);
        }
    }

}