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
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(p -> ((Type) p.get(this)));
            }

            @Override
            public Optional get(ParameterizedType type) {
                Class<?> componentType = (Class<?>) type.getActualTypeArguments()[0];
                if (type.getRawType() != Provider.class) return Optional.empty();
                return Optional.ofNullable(providers.get(componentType))
                        .map(p -> (Provider<Object>) () -> p.get(this));
            }
        };
    }

    public void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        providers.get(component).getDependencyTypes().forEach(dependency -> {
            if (dependency instanceof ParameterizedType) {
                Class<?> providerDependencyType = (Class<?>) ((ParameterizedType) dependency).getActualTypeArguments()[0];
                if (!providers.containsKey(providerDependencyType))
                    throw new DependencyNotFoundException(providerDependencyType, component);
            }
            if (dependency instanceof Class<?>) {
                checkDependency(component, visiting, (Class<?>) dependency);
            }
        });
    }

    private void checkDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency)) {
            throw new DependencyNotFoundException(dependency, component);
        }
        if (visiting.contains(dependency)) {
            throw new CyclicDependenciesFoundException(visiting.stream().toList());
        }
        visiting.push(dependency);
        checkDependencies(dependency, visiting);
        visiting.pop();
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Class<?>> getDependencies() {
            return of();
        }

        default List<Type> getDependencyTypes() {
            return of();
        }
    }

}
