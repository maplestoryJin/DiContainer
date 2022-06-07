package com.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

import static java.util.List.of;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private final Map<Class<?>, Function<ComponentProvider<?>, ComponentProvider<?>>> scopes = new HashMap<>();

    public <Type> void bind(Class<Type> componentClass, Type instance) {
        bindInstance(componentClass, instance, null);
    }

    public ContextConfig() {
        scope(Singleton.class, SingletonProvider::new);
    }

    public <Type> void bind(Class<Type> componentClass, Type instance, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            bindInstance(componentClass, instance, qualifier);
        }
    }

    public <Type, Implementation extends Type> void bind(Class<Type> componentClass, Class<Implementation> implementation) {
        bind(componentClass, implementation, implementation.getAnnotations());
    }

    public <Type, Implementation extends Type> void bind(Class<Type> componentClass, Class<Implementation> implementation, Annotation... annotations) {
        if (Arrays.stream(annotations).map(Annotation::annotationType).anyMatch(q -> !q.isAnnotationPresent(Qualifier.class)
                && !q.isAnnotationPresent(Scope.class))) {
            throw new IllegalComponentException();
        }
        Optional<Annotation> scopeFromImplementation = Arrays.stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();
        List<Annotation> qualifiers = Arrays.stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        Optional<Annotation> scope = Arrays.stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst().or(() -> scopeFromImplementation);
        ComponentProvider<Implementation> injectProvider = new InjectionProvider<>(implementation);
        ComponentProvider<Implementation> provider = scope.map(s -> scopeProvider(s, injectProvider)).orElse(injectProvider);
        if (qualifiers.isEmpty()) {
            bindComponent(componentClass, null, provider);
        }
        for (Annotation qualifier : qualifiers) {
            bindComponent(componentClass, qualifier, provider);
        }
    }

    private <Type> ComponentProvider<Type> scopeProvider(Annotation scope, final ComponentProvider<Type> injectProvider) {
        return (ComponentProvider<Type>) scopes.get(scope.annotationType()).apply(injectProvider);
    }

    private <Type> void bindInstance(Class<Type> componentClass, Type instance, Annotation qualifier) {
        components.put(new Component(componentClass, qualifier), context -> instance);
    }

    private <Type, Implementation extends Type> void bindComponent(Class<Type> componentClass, Annotation qualifier, final ComponentProvider<Implementation> provider) {
        components.put(new Component(componentClass, qualifier), provider);
    }

    public <Type> void scope(final Class<Type> scope, final Function<ComponentProvider<?>, ComponentProvider<?>> provider) {
        scopes.put(scope, provider);
    }

    static class SingletonProvider<T> implements ComponentProvider<T> {
        private T singleton;
        private ComponentProvider<T> provider;

        public SingletonProvider(final ComponentProvider<T> provider) {
            this.provider = provider;
        }

        @Override
        public T get(final Context context) {
            if (singleton == null) {
                return singleton = provider.get(context);
            }
            return singleton;
        }

        @Override
        public List<ComponentRef<?>> getDependencies() {
            return provider.getDependencies();
        }
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

    public void checkDependencies(Component component, Stack<Component> visiting) {
        components.get(component).getDependencies().forEach(dependency -> checkDependency(component, visiting, dependency));
    }

    private void checkDependency(Component component, Stack<Component> visiting, ComponentRef dependency) {
        if (!components.containsKey(dependency.component()))
            throw new DependencyNotFoundException(dependency.component(), component);
        if (!dependency.isContainer()) {
            if (visiting.contains(dependency.component()))
                throw new CyclicDependenciesFoundException(visiting.stream().toList());
            visiting.push(dependency.component());
            checkDependencies(dependency.component(), visiting);
            visiting.pop();
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<ComponentRef<?>> getDependencies() {
            return of();
        }
    }

}
