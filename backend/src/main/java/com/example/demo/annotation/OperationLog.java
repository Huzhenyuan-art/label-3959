package com.example.demo.annotation;

import com.example.demo.enums.OperationTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    OperationTypeEnum type();

    String targetType();

    String targetIdExpression() default "#id";

    boolean recordParams() default true;

    boolean recordResult() default true;
}
