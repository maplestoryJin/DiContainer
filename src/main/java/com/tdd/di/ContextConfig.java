package com.tdd.di;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.List.of;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> componentClass, Type instance) {
        providers.put(componentClass, (ComponentProvider<Type>) context -> instance);
    }

    public <Type> void bind(Class<Type> componentClass, Class<? extends Type> implementation) {

        providers.put(componentClass, new InjectionProvider<Type>(implementation));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {
            private <Type> Optional<Type> getComponent(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(p -> ((Type) p.get(this)));
            }

            private Optional getContainer(ParameterizedType type) {
                if (type.getRawType() != Provider.class) return Optional.empty();
                return Optional.ofNullable(providers.get(getComponentType(type)))
                        .map(p -> (Provider<Object>) () -> p.get(this));
            }

            @Override
            public Optional get(Type type) {
                if (isContainerType(type)) return getContainer((ParameterizedType) type);
                return getComponent((Class<?>) type);
            }
        };
    }

    private Class<?> getComponentType(ParameterizedType type) {
        return (Class<?>) type.getActualTypeArguments()[0];
    }

    private boolean isContainerType(Type type) {
        return type instanceof ParameterizedType;
    }

    public void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        providers.get(component).getDependencies().forEach(dependency -> {
            if (isContainerType(dependency)) checkContainerTypeDependencies(component, (ParameterizedType) dependency);
            else checkComponentDependency(component, visiting, (Class<?>) dependency);
        });
    }

    private void checkContainerTypeDependencies(Class<?> component, ParameterizedType dependency) {
        if (!providers.containsKey(getComponentType(dependency)))
            throw new DependencyNotFoundException(getComponentType(dependency), component);
    }

    private void checkComponentDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(dependency, component);
        if (visiting.contains(dependency)) throw new CyclicDependenciesFoundException(visiting.stream().toList());
        visiting.push(dependency);
        checkDependencies(dependency, visiting);
        visiting.pop();
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Type> getDependencies() {
            return of();
        }
    }

}
