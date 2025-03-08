package newOs.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import newOs.annotation.arange;


@Aspect
@Component
public class ValidateAspect {
    @Around("@annotation(arange)")
    public Object validateParameters(ProceedingJoinPoint pjp,arange arange) throws Throwable{
        Object[] args = pjp.getArgs();
        for (Object arg : args) {
            if (arg instanceof Integer) {
                int value = (int) arg;
                if (value < arange.min() || value > arange.max()) {
                    throw new IllegalArgumentException("参数不在指定范围内");
                }
            }
        }
        return pjp.proceed();
    }
}
