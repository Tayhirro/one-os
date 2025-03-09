package newOs.component.cpu.Interrupt;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 硬件信号是存储在irl 物理线路上的，和内存无关
 */
@Slf4j
public class InterruptRequestLine extends ConcurrentLinkedQueue<String> {
    public final String irlName;
    public InterruptRequestLine(String irlName) {
        super();
        this.irlName = irlName;
    }

    @Override
    public boolean offer(String item) {
        if (!item.equals("TIMER_INTERRUPT") || !this.contains("TIMER_INTERRUPT")) {
            return super.offer(item);
        }
        return false;
    }
    //poll 是从队列中取出一个元素
    //peek 是查看队列头部的元素
}
