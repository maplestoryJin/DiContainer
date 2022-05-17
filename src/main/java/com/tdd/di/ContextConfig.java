package com.tdd.di;

import java.util.*;

import static java.util.List.of;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> componentClass, Type instance) {
        providers.put(componentClass, new ComponentProvider<Type>() {
            @Override
            public Type get(Context context) {
                return instance;
            }

            @Override
            public List<Class<?>> getDependencies() {
                return of();
            }
        });
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
        };
    }

    public void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        providers.get(component).getDependencies().forEach(dependency -> {
            if (!providers.containsKey(dependency)) {
                throw new DependencyNotFoundException(dependency, component);
            }
            if (visiting.contains(dependency)) {
                throw new CyclicDependenciesFoundException(visiting.stream().toList());
            }
            visiting.push(dependency);
            checkDependencies(dependency, visiting);
            visiting.pop();
        });
    }

    interface ComponentProvider<Type> {
        Type get(Context context);

        List<Class<?>> getDependencies();
    }

}
