package com.shipparts.annotation;

import java.lang.annotation.*;

/**
 *自定义控制器注解
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtController {
}
