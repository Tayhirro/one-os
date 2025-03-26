package newOs.kernel.Device;

import lombok.Data;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoImplDTO;
import newOs.kernel.Device.DeviceImpl.DeviceDriverImpl;
import newOs.kernel.interrupt.InterruptController;
import newOs.component.device.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Data
@Component
public class DeviceFactory {
    private final ProtectedMemory protectedMemory;
    private final InterruptController interruptController;

    @Autowired
    public DeviceFactory(ProtectedMemory protectedMemory, InterruptController interruptController) {
        this.protectedMemory = protectedMemory;
        this.interruptController = interruptController;
    }
    //创建设备
    public DeviceDriverImpl createDevice(DeviceInfoImplDTO deviceInfoImplDTO, Device device){
        DeviceDriverImpl deviceDriver = new DeviceDriverImpl(deviceInfoImplDTO.getDeviceName(), 
                                        deviceInfoImplDTO.getDeviceInfo(), 
                                        interruptController,
                                        device);

        //自动加入队列
        protectedMemory.getDeviceQueue().add(deviceDriver);

        return deviceDriver;
    }

}
