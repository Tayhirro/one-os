package newOs.kernel.device;


import newOs.dto.resp.DeviceManage.DeviceQueryAllRespDTO;

public interface DeviceDriver {
    void add(String deviceName);
    void delete(String deviceName);
    void shutdownAll();
    void shutdown(String deviceName);
    DeviceQueryAllRespDTO queryAllDeviceInfo();
}