package com.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ComponentRef<ComponentType> {
    private Component component;
    private Type container;

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return of(component, null);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new ComponentRef<>(component, qualifier);
    }

    public static ComponentRef of(Type type) {
        return of(type, null);
    }

    public static ComponentRef of(Type type, Annotation qualifier) {
        return new ComponentRef(type, qualifier);
    }

    ComponentRef(Type type, Annotation qualifier) {
        init(type, qualifier);
    }

    protected ComponentRef() {
        this(null);
    }
protected ComponentRef(Annotation qualifier) {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type, qualifier);
    }

    private void init(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType container) {
            component = new Component((Class<ComponentType>) container.getActualTypeArguments()[0], qualifier);
            this.container = container.getRawType();
        } else {
            component = new Component((Class<ComponentType>) type, qualifier);
        }
    }

    public boolean isContainer() {
        return container != null;
    }

    public Class<?> getComponentType() {
        return component.type();
    }

    public Type getContainer() {
        return container;
    }

    public Component component() {
        return component;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentRef<?> componentRef = (ComponentRef<?>) o;
        return component.equals(componentRef.component) && Objects.equals(container, componentRef.container);
    }

    @Override
    public int hashCode() {
        return Objects.hash(component, container);
    }
}
