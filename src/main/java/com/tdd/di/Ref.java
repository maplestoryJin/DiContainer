package com.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class Ref<ComponentType> {
    private Component component;
    private Type container;

    public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> component) {
        return new Ref<>(component, null);
    }

    public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new Ref<>(component, qualifier);
    }

    public static Ref of(Type type) {
        return new Ref(type, null);
    }

    Ref(Type type, Annotation qualifier) {
        init(type, qualifier);
    }

    protected Ref() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type, null);
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
        Ref<?> ref = (Ref<?>) o;
        return component.equals(ref.component) && Objects.equals(container, ref.container);
    }

    @Override
    public int hashCode() {
        return Objects.hash(component, container);
    }
}
