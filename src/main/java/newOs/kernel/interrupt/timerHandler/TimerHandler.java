package newOs.kernel.interrupt.timerHandler;

import newOs.component.cpu.Interrupt.InterruptRequestLine;
import newOs.component.memory.protected1.ProtectedMemory;
import newOs.dto.req.Info.TimerInfo;
import newOs.kernel.interrupt.ISR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TimerHandler implements ISR<TimerInfo> {
        private final ConcurrentHashMap<Long, InterruptRequestLine> irlTable;
        //定时中断
        @Autowired
        public TimerHandler(ProtectedMemory protectedMemory) {
            this.irlTable = protectedMemory.getIrlTable();
        }

        //目前只有一个定时中断
        @Override
        public TimerInfo execute(TimerInfo interruptInfo) {
                Set<Long> set = irlTable.keySet();
                for (Long threadId : set) {
                    InterruptRequestLine irl = irlTable.get(threadId);
                    if(irl != null)
                        irl.offer("TIMER_INTERRUPT");       //推入irl中断请求
                }
                return interruptInfo;   //原路返回
        }
}
