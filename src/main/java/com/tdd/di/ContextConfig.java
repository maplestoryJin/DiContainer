package com.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tdd.di.ContextConfig.ContextConfigError.circularDependencies;
import static com.tdd.di.ContextConfig.ContextConfigError.unsatisfiedResolution;
import static com.tdd.di.ContextConfig.ContextConfigException.illegalAnnotation;
import static java.util.Arrays.stream;
import static java.util.List.of;
import static java.util.stream.Collectors.joining;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private final Map<Class<?>, ScopeProvider> scopes = new HashMap<>();
    private final List<Component> staticsComponents = new ArrayList<>();

    public ContextConfig() {
        scope(Singleton.class, SingletonProvider::new);
    }

    public <Type> void instance(Class<Type> type, Type instance) {
        bind(new Component(type, null), (ComponentProvider<Object>) context -> instance, false);
    }

    public <Type> void instance(Class<Type> type, Type instance, Annotation... annotations) {
        bindInstance(type, instance, annotations, false);
    }

    private void bindInstance(Class<?> type, Object instance, Annotation[] annotations, boolean statics) {
        Bindings bindings = new Bindings(type, annotations);
        bind(type, bindings.qualifiers(), context -> instance, statics);
    }

    public <Type, Implementation extends Type> void component(Class<Type> type, Class<Implementation> implementation, Annotation... annotations) {
        bindComponent(type, implementation, annotations, false);
    }

    private void bindComponent(Class<?> type, Class<?> implementation, Annotation[] annotations, boolean statics) {
        Bindings bindings = new Bindings(implementation, annotations);
        bind(type, bindings.qualifiers(), bindings.provider(this::scopeProvider), statics);
    }

    private <Type> void bind(final Class<Type> type, List<Annotation> qualifiers, final ComponentProvider<?> provider, boolean statics) {
        if (qualifiers.isEmpty()) bind(new Component(type, null), provider, statics);
        for (Annotation qualifier : qualifiers) bind(new Component(type, qualifier), provider, statics);
    }

    static class Bindings {
        private Class<?> type;
        private Map<Class<?>, List<Annotation>> group;

        public Bindings(Class<?> type, Annotation... annotations) {
            this.type = type;
            group = parse(annotations);
        }


        private Map<Class<?>, List<Annotation>> parse(Annotation[] annotations) {
            Map<Class<?>, List<Annotation>> annotationGroup = stream(annotations).collect(Collectors.groupingBy(Bindings::typeOf));
            if (annotationGroup.containsKey(Illegal.class))
                throw illegalAnnotation(type, annotationGroup.get(Illegal.class));
            return annotationGroup;
        }

        private static Class<? extends Annotation> typeOf(final Annotation annotation) {
            return Stream.of(Qualifier.class, Scope.class).filter(a -> annotation.annotationType().isAnnotationPresent(a)).findFirst().orElse(Illegal.class);
        }

        List<Annotation> qualifiers() {
            return group.getOrDefault(Qualifier.class, List.of());
        }

        private Optional<Annotation> scope() {
            List<Annotation> scopes = group.getOrDefault(Scope.class, scopeFrom(type));
            if (scopes.size() > 1) throw illegalAnnotation(type, scopes);
            return scopes.stream().findFirst();
        }

        private static <Type> List<Annotation> scopeFrom(final Class<Type> implementation) {
            return stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).toList();
        }

        private ComponentProvider<?> provider(BiFunction<Annotation, ComponentProvider<?>, ComponentProvider<?>> scoped) {
            ComponentProvider<?> injectProvider = new InjectionProvider<>(type);
            return scope().<ComponentProvider<?>>map(s -> scoped.apply(s, injectProvider)).orElse(injectProvider);
        }
    }

    public void from(final Config config) {
        new DSL(config).bind();
    }

    private @interface Illegal {
    }

    private ComponentProvider<?> scopeProvider(Annotation scope, final ComponentProvider<?> injectProvider) {
        if (!scopes.containsKey(scope.annotationType()))
            throw ContextConfigException.unknownScope(scope.annotationType());
        return scopes.get(scope.annotationType()).create(injectProvider);
    }

    private <Type, Implementation extends Type> void bind(Component component, final ComponentProvider<Implementation> provider, boolean statics) {
        if (components.containsKey(component)) throw ContextConfigException.duplicated(component);
        if (statics) staticsComponents.add(component);
        components.put(component, provider);
    }

    public <Type> void scope(final Class<Type> scope, final ScopeProvider provider) {
        scopes.put(scope, provider);
    }


    public Context getContext() {

        Context context = new Context() {

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

        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        injectStaticMembers(context);
        return context;
    }

    private void injectStaticMembers(Context context) {
        for (final Component component : staticsComponents) {
            Optional.ofNullable(components.get(component))
                    .ifPresent(p -> p.statics(context));
        }
    }

    public void checkDependencies(Component component, Stack<Component> visiting) {
        components.get(component).getDependencies().forEach(dependency -> checkDependency(component, visiting, dependency));
    }

    private void checkDependency(Component component, Stack<Component> visiting, ComponentRef dependency) {
        if (!components.containsKey(dependency.component()))
            throw unsatisfiedResolution(component, dependency.component());
        if (!dependency.isContainer()) {
            if (visiting.contains(dependency.component()))
                throw circularDependencies(visiting, dependency.component());
            visiting.push(dependency.component());
            checkDependencies(dependency.component(), visiting);
            visiting.pop();
        }
    }

    interface ScopeProvider {
        ComponentProvider<?> create(ComponentProvider<?> provider);
    }

    static class ContextConfigError extends Error {
        public static ContextConfigError unsatisfiedResolution(Component component, Component dependency) {
            return new ContextConfigError(MessageFormat.format("Unsatisfied resolution: {1} for {0} ", component, dependency));
        }

        public static ContextConfigError circularDependencies(Collection<Component> path, Component circular) {
            return new ContextConfigError(MessageFormat.format("Circular dependencies: {0} -> [{1}]",
                    path.stream().map(Objects::toString).collect(joining(" -> ")), circular));
        }

        ContextConfigError(String message) {
            super(message);
        }
    }

    static class ContextConfigException extends RuntimeException {
        static ContextConfigException illegalAnnotation(Class<?> type, List<Annotation> annotations) {
            return new ContextConfigException(MessageFormat.format("Unqualified annotations: {0} of {1}",
                    String.join(" , ", annotations.stream().map(Object::toString).toList()), type));
        }

        static ContextConfigException unknownScope(Class<? extends Annotation> annotationType) {
            return new ContextConfigException(MessageFormat.format("Unknown scope: {0}", annotationType));
        }

        static ContextConfigException duplicated(Component component) {
            return new ContextConfigException(MessageFormat.format("Duplicated: {0}", component));
        }

        ContextConfigException(String message) {
            super(message);
        }
    }

    private class DSL {
        private final Config config;

        public DSL(final Config config) {
            this.config = config;
        }

        public void bind() {
            for (Declaration declaration : declarations()) {
                declaration.value().ifPresentOrElse(declaration::bindInstance, declaration::bindComponent);
            }
        }

        private List<Declaration> declarations() {
            return stream(config.getClass().getDeclaredFields()).filter(f -> !f.isSynthetic()).map(Declaration::new).toList();
        }

        private class Declaration {
            private Field field;

            Declaration(Field field) {
                this.field = field;
            }

            void bindInstance(Object instance) {
                ContextConfig.this.bindInstance(type(), instance, annotations(), statics());
            }

            void bindComponent() {
                ContextConfig.this.bindComponent(type(), field.getType(), annotations(), statics());
            }

            private Optional<Object> value() {
                try {
                    return Optional.ofNullable(field.get(config));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            private Class<?> type() {
                Config.Export export = field.getAnnotation(Config.Export.class);
                return export != null ? export.value() : field.getType();
            }
            private boolean statics() {
                Config.Static aStatic = field.getAnnotation(Config.Static.class);
                return aStatic != null;
            }

            private Annotation[] annotations() {
                return stream(field.getAnnotations()).filter(a -> a.annotationType() != Config.Export.class && a.annotationType() != Config.Static.class).toArray(Annotation[]::new);
            }
        }
    }
}
