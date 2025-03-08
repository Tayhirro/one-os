package newOs.annotation;

import java.lang.annotation.*;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface arange {
    int min() default 0;
    int max()  default 10;
}
