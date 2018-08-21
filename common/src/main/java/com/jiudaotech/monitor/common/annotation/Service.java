package com.jiudaotech.monitor.common.annotation;

import java.lang.annotation.*;

/**
 * @author gzy
 * @since 2018/4/26 13:32
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Service {
    String type() default "";
}
