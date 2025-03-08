package newOs.common.processConstant;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class processStateConstant {
    public static final String CREATED = "CREATED";
    public static final String READY = "READY";
    public static final String WAITING = "WAITING";
    public static final String RUNNING = "RUNNING";
    public static final String TERMINATED = "TERMINATED";
}


