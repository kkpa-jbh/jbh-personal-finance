package com.jbh.products.application.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {

  String value() default "";

  boolean logParams() default false;

  boolean logResult() default false;

  boolean logExecutionTime() default true;

  String module() default "";
}