package com.tdd.di;

public class DependencyNotFoundException extends RuntimeException {
    private Component dependency;
    private Component component;

    public DependencyNotFoundException(final Component dependency, final Component component) {
        this.dependency = dependency;
        this.component = component;
    }

    public Component getDependency() {
        return dependency;
    }

    public Component getComponent() {
        return component;
    }
}
