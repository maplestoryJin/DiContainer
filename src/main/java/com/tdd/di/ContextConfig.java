package com.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.List.of;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private final Map<Class<?>, ScopeProvider> scopes = new HashMap<>();

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
        Map<? extends Class<? extends Annotation>, List<Annotation>> annotationGroup = Arrays.stream(annotations).collect(Collectors.groupingBy(this::typeOf, Collectors.toList()));
        if (annotationGroup.containsKey(Illegal.class)) throw new IllegalComponentException();
        bind(componentClass, annotationGroup, createProvider(implementation, new InjectionProvider<>(implementation), annotationGroup.getOrDefault(Scope.class, List.of())));
    }

    private <Type> ComponentProvider<Type> createProvider(final Class<Type> implementation, final ComponentProvider<Type> injectProvider, final List<Annotation> scopes) {
        if (scopes.size() > 1) throw new IllegalComponentException();
        return scopes.stream().findFirst().or(() -> scopeFrom(implementation)).map(s -> scopeProvider(s, injectProvider)).orElse(injectProvider);
    }

    private <Type, Implementation extends Type> void bind(final Class<Type> componentClass, final Map<? extends Class<? extends Annotation>, List<Annotation>> annotationGroup, final ComponentProvider<Implementation> provider) {
        List<Annotation> qualifiers = annotationGroup.getOrDefault(Qualifier.class, List.of());
        if (qualifiers.isEmpty()) bindComponent(componentClass, null, provider);
        for (Annotation qualifier : qualifiers) bindComponent(componentClass, qualifier, provider);
    }

    private <Type> Optional<Annotation> scopeFrom(final Class<Type> implementation) {
        return Arrays.stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();
    }

    private Class<? extends Annotation> typeOf(final Annotation annotation) {
        return Stream.of(Qualifier.class, Scope.class).filter(a -> annotation.annotationType().isAnnotationPresent(a)).findFirst().orElse(Illegal.class);
    }

    private @interface Illegal {
    }

    private <Type> ComponentProvider<Type> scopeProvider(Annotation scope, final ComponentProvider<Type> injectProvider) {
        if (!scopes.containsKey(scope.annotationType())) throw new IllegalComponentException();
        return (ComponentProvider<Type>) scopes.get(scope.annotationType()).create(injectProvider);
    }

    private <Type> void bindInstance(Class<Type> componentClass, Type instance, Annotation qualifier) {
        components.put(new Component(componentClass, qualifier), context -> instance);
    }

    private <Type, Implementation extends Type> void bindComponent(Class<Type> componentClass, Annotation qualifier, final ComponentProvider<Implementation> provider) {
        components.put(new Component(componentClass, qualifier), provider);
    }

    public <Type> void scope(final Class<Type> scope, final ScopeProvider provider) {
        scopes.put(scope, provider);
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

    interface ScopeProvider {
        ComponentProvider<?> create(ComponentProvider<?> provider);
    }

}
