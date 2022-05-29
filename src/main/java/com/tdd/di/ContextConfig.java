package com.tdd.di;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.List.of;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <Type> void bind(Class<Type> componentClass, Type instance) {
        providers.put(componentClass, context -> instance);
    }

    public <Type> void bind(Class<Type> componentClass, Type instance, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(componentClass, qualifier), context -> instance);
        }
    }

    record Component(Class<?> type, Annotation qualifier) {
    }

    public <Type> void bind(Class<Type> componentClass, Class<? extends Type> implementation) {
        providers.put(componentClass, new InjectionProvider<Type>(implementation));
    }

    public <Type> void bind(Class<Type> componentClass, Class<? extends Type> implementation, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(componentClass, qualifier), new InjectionProvider<Type>(implementation));
        }
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {

            @Override
            public <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref) {
                if (ref.getQualifier() != null) {
                    return Optional.ofNullable(components.get(new Component(ref.getComponent(), ref.getQualifier()))).map(p -> ((ComponentType) p.get(this)));
                }
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(providers.get(ref.getComponent()))
                            .map(p -> (Provider<Object>) () -> p.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponent())).map(p -> ((ComponentType) p.get(this)));
            }
        };
    }

    public void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        providers.get(component).getDependencies().forEach(dependency -> checkDependency(component, visiting, dependency));
    }

    private void checkDependency(Class<?> component, Stack<Class<?>> visiting, Context.Ref ref) {
        if (!providers.containsKey(ref.getComponent()))
            throw new DependencyNotFoundException(ref.getComponent(), component);
        if (!ref.isContainer()) {
            if (visiting.contains(ref.getComponent()))
                throw new CyclicDependenciesFoundException(visiting.stream().toList());
            visiting.push(ref.getComponent());
            checkDependencies(ref.getComponent(), visiting);
            visiting.pop();
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Context.Ref> getDependencies() {
            return of();
        }
    }

}
