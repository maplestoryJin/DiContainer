package com.tdd.di;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;


public interface Config {
    @Documented
    @Retention(RUNTIME)
    @Target({ElementType.FIELD})
    @interface Export {
        Class<?> value();
    }
}