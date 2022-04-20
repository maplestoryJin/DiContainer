package com.tdd.di;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CyclicDependenciesFoundException extends RuntimeException {
    private Set<Class<?>> components = new HashSet<>();

    public CyclicDependenciesFoundException(List<Class<?>> components) {
        this.components.addAll(components);
    }


    public Class<?>[] getComponents() {
        return components.toArray(Class[]::new);
    }
}
