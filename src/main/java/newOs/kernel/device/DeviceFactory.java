package newOs.kernel.device;

import lombok.Data;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoImplDTO;
import newOs.kernel.device.DeviceImpl.DeviceDriverImpl;
import org.springframework.stereotype.Component;

@Data
@Component
public class DeviceFactory {
    private final ProtectedMemory protectedMemory;


    public DeviceFactory(ProtectedMemory protectedMemory) {
        this.protectedMemory = protectedMemory;
    }
    //创建设备
    public DeviceDriverImpl createDevice(DeviceInfoImplDTO deviceInfoImplDTO){
        DeviceDriverImpl deviceDriver = new DeviceDriverImpl(deviceInfoImplDTO.getDeviceName(), deviceInfoImplDTO.getDeviceInfo());

        //自动加入队列
        protectedMemory.getDeviceQueue().add(deviceDriver);

        return deviceDriver;
    }

}
