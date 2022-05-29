package com.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.List.of;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <Type> void bind(Class<Type> componentClass, Type instance) {
        bindInstance(componentClass, instance, null);
    }

    public <Type> void bind(Class<Type> componentClass, Type instance, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            bindInstance(componentClass, instance, qualifier);
        }
    }

    public <Type> void bind(Class<Type> componentClass, Class<? extends Type> implementation) {
        bindComponent(componentClass, implementation, null);
    }

    public <Type> void bind(Class<Type> componentClass, Class<? extends Type> implementation, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            bindComponent(componentClass, implementation, qualifier);
        }
    }

    private <Type> void bindInstance(Class<Type> componentClass, Type instance, Annotation qualifier) {
        components.put(new Component(componentClass, qualifier), context -> instance);
    }

    private <Type> void bindComponent(Class<Type> componentClass, Class<? extends Type> implementation, Annotation qualifier) {
        components.put(new Component(componentClass, qualifier), new InjectionProvider<Type>(implementation));
    }

    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {

            @Override
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> componentRef) {
                if (componentRef.isContainer()) {
                    if (componentRef.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(components.get(componentRef.component()))
                            .map(p -> (Provider<Object>) () -> p.get(this));
                }
                return Optional.ofNullable(components.get(componentRef.component())).map(p -> ((ComponentType) p.get(this)));

            }
        };
    }

    public void checkDependencies(Component component, Stack<Class<?>> visiting) {
        components.get(component).getDependencies().forEach(dependency -> checkDependency(component.type(), visiting, dependency));
    }

    private void checkDependency(Class<?> component, Stack<Class<?>> visiting, ComponentRef componentRef) {
        if (!components.containsKey(componentRef.component()))
            throw new DependencyNotFoundException(componentRef.getComponentType(), component);
        if (!componentRef.isContainer()) {
            if (visiting.contains(componentRef.getComponentType()))
                throw new CyclicDependenciesFoundException(visiting.stream().toList());
            visiting.push(componentRef.getComponentType());
            checkDependencies(componentRef.component(), visiting);
            visiting.pop();
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<ComponentRef> getDependencies() {
            return of();
        }
    }

}
