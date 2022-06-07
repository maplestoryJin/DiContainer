package com.tdd.di;

import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.util.Objects;

import static java.lang.annotation.RetentionPolicy.RUNTIME;


interface AnotherDependency {

}

interface Dependency {

}

interface TestComponent {
    default Dependency dependency() {
        return null;
    }

}

record TestLiteral() implements Test {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}

record NamedLiteral(String value) implements jakarta.inject.Named {

    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof jakarta.inject.Named named) return Objects.equals(value, named.value());
        return false;
    }

    @Override
    public int hashCode() {
        return "value".hashCode() * 127 ^ value.hashCode();
    }
}


@Documented
@Retention(RUNTIME)
@Qualifier
@interface Skywalker {
}

record SkywalkerLiteral() implements Skywalker {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Skywalker.class;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Skywalker;
    }
}

