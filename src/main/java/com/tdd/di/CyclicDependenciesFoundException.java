package com.tdd.di;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CyclicDependenciesFoundException extends RuntimeException {
    private Set<Component> components = new HashSet<>();

    public CyclicDependenciesFoundException(List<Component> components) {
        this.components.addAll(components);
    }


    public Class<?>[] getComponents() {
        return components.stream().map(component -> component.type()).toArray(Class[]::new);
    }
}
