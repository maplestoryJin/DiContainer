package com.tdd.di;

public class DependencyNotFoundException extends RuntimeException {
    private Class<?> dependency;
    private Class<?> component;

    public DependencyNotFoundException(Class<?> dependency, Class<?> component) {
        this.dependency = dependency;
        this.component = component;
    }

    public Class<?> getDependency() {
        return dependency;
    }

    public Class<?> getComponent() {
        return component;
    }
}
