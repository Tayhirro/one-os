package newOs.kernel.interrupt.hardwareHandler;

import newOs.component.memory.protected1.PCB;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.InfoImplDTO.DeviceInfoReturnImplDTO;
import newOs.dto.req.Info.InterruptInfo;
import newOs.kernel.interrupt.ISR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;


@Component
public class IOInterruptHandler implements ISR<DeviceInfoReturnImplDTO> {
    private final ConcurrentLinkedQueue<PCB> irlIO;

    @Autowired
    public IOInterruptHandler(ProtectedMemory protectedMemory) {
        this.irlIO = protectedMemory.getIrlIO();
    }


    @Override
    public InterruptInfo execute(DeviceInfoReturnImplDTO interruptDeviceInfo) {
        irlIO.add(interruptDeviceInfo.getPcb());
        return  null;
    }
}
